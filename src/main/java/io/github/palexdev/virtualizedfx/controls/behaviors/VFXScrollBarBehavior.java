package io.github.palexdev.virtualizedfx.controls.behaviors;

import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils;
import io.github.palexdev.mfxeffects.animations.Animations.KeyFrames;
import io.github.palexdev.mfxeffects.animations.Animations.PauseBuilder;
import io.github.palexdev.mfxeffects.animations.Animations.TimelineBuilder;
import io.github.palexdev.mfxeffects.animations.MomentumTransition;
import io.github.palexdev.mfxeffects.animations.base.Curve;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.palexdev.virtualizedfx.controls.VFXScrollBar.*;

/**
 * Extension of {@link BehaviorBase} and default behavior implementation for {@link VFXScrollBar}.
 * <p></p>
 * This offers all the methods to manage scrolling and smooth scrolling, track press/release,
 * buttons press/release, thumb press/drag/release. And a bunch of other misc methods.
 */
public class VFXScrollBarBehavior extends BehaviorBase<VFXScrollBar> {
	//================================================================================
	// Properties
	//================================================================================
	private double dragStart;

	// Animations config
	protected Duration FIRST_TICK_DURATION = M3Motion.SHORT1;
	protected Interpolator FIRST_TICK_CURVE = M3Motion.STANDARD;
	protected Duration SMOOTH_SCROLL_DURATION = M3Motion.LONG2;
	protected Duration TRACK_SMOOTH_SCROLL_DURATION = M3Motion.EXTRA_LONG1;
	protected Interpolator SMOOTH_SCROLL_CURVE = Curve.EASE_BOTH;
	protected Duration MAX_BUTTONS_SCROLL_DURATION = Duration.millis(3000);
	protected Duration HOLD_DELAY = M3Motion.SHORT4;

	// Animations state
	private Animation holdAnimation;
	private Animation scrollAnimation;
	private final Set<Animation> smoothScrollAnimations = new LinkedHashSet<>();

	//================================================================================
	// Constructors
	//================================================================================
	public VFXScrollBarBehavior(VFXScrollBar bar) {
		super(bar);
	}

	//================================================================================
	// Methods
	//================================================================================
	// THUMB

	/**
	 * Action performed when the thumb has been pressed.
	 * <p></p>
	 * The first steps are to stop any animation (see {@link #stopAnimations()}) then {@link #requestFocus()}
	 * and update the dragStart position using {@link #getMousePos(MouseEvent)}, in case the next user action is
	 * dragging the thumb.
	 */
	public void thumbPressed(MouseEvent me) {
		stopAnimations();
		requestFocus();
		dragStart = getMousePos(me);
	}


	/**
	 * Action performed when the thumb is being dragged.
	 * <p></p>
	 * First we call {@link #onDragging(boolean)}. Then we acquire the mouse position with {@link #getMousePos(MouseEvent)}
	 * to compute the traveled distance since the press event as {@code currentPos - dragStart}.
	 * <p>
	 * We convert the traveled distance to the corresponding delta value by using {@link NumberUtils#mapOneRangeToAnother(double, DoubleRange, DoubleRange)}.
	 * A call to {@link #getAndSetScrollDirection(boolean)} updates the {@link VFXScrollBar#scrollDirectionProperty()}
	 * and returns a multiplier (1 or -1) used to adjust the scroll bar's value by the found delta value:
	 * {@code bar.setValue(bar.getValue() + deltaVal * mul)}.
	 */
	public void thumbDragged(MouseEvent me) {
		VFXScrollBar bar = getNode();
		onDragging(true);
		double pos = getMousePos(me);
		double deltaPos = pos - dragStart;
		double maxPos = (bar.getOrientation() == Orientation.VERTICAL) ? bar.getHeight() : bar.getWidth();
		// This is not accurate in theory as the max bound would be trackLength - thumbLength
		// Works just fine so not gonna bother
		double deltaVal = NumberUtils.mapOneRangeToAnother(
			NumberUtils.clamp(Math.abs(deltaPos), 0.0, maxPos),
			DoubleRange.of(0.0, maxPos),
			DoubleRange.of(0.0, bar.getMaxScroll())
		);
		int mul = getAndSetScrollDirection(deltaPos > 0);
		bar.setValue(bar.getValue() + deltaVal * mul);
	}

	/**
	 * Action performed when the thumb is released.
	 * <p></p>
	 * The dragStart position is reset to 0.0, and the dragging property set to false.
	 */
	public void thumbReleased(MouseEvent me) {
		dragStart = 0.0;
		onDragging(false);
	}

	// TRACK

	/**
	 * Action performed when the track is pressed.
	 * <p></p>
	 * The first steps are to stop any animation (see {@link #stopAnimations()}) then {@link #requestFocus()}.
	 * Then we check whether smooth scroll and track smooth scroll features have been enabled, in such case the method
	 * exists as the scroll will be handled by {@link #trackReleased(MouseEvent)}.
	 * <p></p>
	 * Otherwise, we get the mouse position with {@link #getMousePos(MouseEvent)} and the track length with {@link #getTrackLength(MouseEvent)}.
	 * We define the pos percentage as {@code mousePos / trackLength}. The target value is given by {@code maxScroll * posPercentage}.
	 * The delta value is {@code targetVal - bar.getValue()}. Finally, the scroll direction is determined by {@link #getAndSetScrollDirection(boolean)}.
	 * <p></p>
	 * At this point, two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the track you expect the thumb to move
	 * towards the mouse position by the specified {@link VFXScrollBar#trackIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted, but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (the delay is specified by {@link #HOLD_DELAY})
	 * and makes the thumb reposition towards the mouse position with a {@link MomentumTransition}. Before doing so, of course,
	 * it checks if the thumb is already at the mouse position or beyond, in such cases it simply does nothing.
	 * <p></p>
	 * Both the second animation and the inner {@link MomentumTransition} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 */
	public void trackPressed(MouseEvent me) {
		stopAnimations();
		requestFocus();

		VFXScrollBar bar = getNode();
		// If smooth scroll and track smooth scroll are active do not proceed.
		// Such case is managed on track release
		if (bar.isSmoothScroll() && bar.isTrackSmoothScroll()) return;

		double pos = getMousePos(me);
		double trackLength = getTrackLength(me);
		double posPercentage = pos / trackLength;
		double targetVal = bar.getMaxScroll() * posPercentage;
		double deltaVal = targetVal - bar.getValue();
		int mul = getAndSetScrollDirection(targetVal > bar.getValue());

		// First scroll tick
		Animation firstTick = firstTick(bar.getValue() + bar.getTrackIncrement() * mul);

		// Detect hold
		holdAnimation = onHold(e -> {
			boolean isHover = bar.isHover();
			boolean incAndGreater = (mul == 1) && bar.getValue() >= targetVal;
			boolean decAndLesser = (mul != 1) && bar.getValue() <= targetVal;
			boolean isMax = bar.getValue() == bar.getMax();
			if (!isHover || incAndGreater || decAndLesser || isMax) return;

			scrollAnimation = withMomentum(deltaVal, TRACK_SMOOTH_SCROLL_DURATION)
				.setOnUpdate(u -> {
					if (!bar.isHover()) {
						stopAnimations();
						return;
					}
					double val = bar.getValue() + u;
					double clamped = (mul == 1) ? Math.min(val, targetVal) : Math.max(val, targetVal);
					bar.setValue(clamped);
				});
			scrollAnimation.play();
		});
		firstTick.play();
		holdAnimation.play();
	}

	/**
	 * Action performed when the track is released
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()}).
	 * <p>
	 * The rest of the method executes only if both the smooth scroll and track smooth scroll features are enabled.
	 * <p>
	 * As usual,we get the mouse position with {@link #getMousePos(MouseEvent)} and the track length with {@link #getTrackLength(MouseEvent)}.
	 * We define the pos percentage as {@code mousePos / trackLength}. The target value is given by {@code maxScroll * posPercentage}.
	 * The delta value is {@code targetVal - bar.getValue()}. Finally, the scroll direction is determined by {@link #getAndSetScrollDirection(boolean)}.
	 * <p>
	 * Finally, the scroll bar's value is adjusted with a {@link MomentumTransition}.
	 */
	public void trackReleased(MouseEvent me) {
		stopAnimations();
		VFXScrollBar bar = getNode();
		if (!bar.isHover()) return;

		if (bar.isSmoothScroll() && bar.isTrackSmoothScroll()) {
			double position = getMousePos(me);
			double trackLength = getTrackLength(me);
			double posPercentage = position / trackLength;
			double targetVal = bar.getMaxScroll() * posPercentage;
			double deltaVal = targetVal - bar.getValue();
			int mul = getAndSetScrollDirection(targetVal > bar.getValue());
			boolean increment = mul == 1;

			scrollAnimation = withMomentum(deltaVal, TRACK_SMOOTH_SCROLL_DURATION)
				.setOnUpdate(u -> {
					double val = bar.getValue() + u;
					double clamped = increment ? Math.min(val, targetVal) : Math.max(val, targetVal);
					bar.setValue(clamped);
				});
			scrollAnimation.play();
		}
	}

	/**
	 * Retrieves the scroll bar's track length from the given {@link MouseEvent}. This can be done on events intercepted
	 * by the track because we get the node thanks to {@link PickResult#getIntersectedNode()}.
	 * The returned value depends on the orientation.
	 */
	protected double getTrackLength(MouseEvent me) {
		Orientation o = getNode().getOrientation();
		Node track = me.getPickResult().getIntersectedNode();
		Bounds bounds = track.getLayoutBounds();
		return (o == Orientation.VERTICAL) ? bounds.getHeight() : bounds.getWidth();
	}

	// BUTTONS

	/**
	 * Action performed when the decrease button is pressed.
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()}) and to acquire focus with {@link #requestFocus()}.
	 * <p>
	 * Two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the button you expect the thumb to move
	 * up/left (depending on the orientation) by the specified {@link VFXScrollBar#unitIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted, but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (the delay is specified by {@link #HOLD_DELAY})
	 * and makes the thumb reposition with a {@link Timeline}.
	 * <p></p>
	 * Both the second animation and the inner {@link Timeline} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 * <p></p>
	 * A call to {@link #getAndSetScrollDirection(boolean)} with {@code false} as parameter ensures that the
	 * {@link VFXScrollBar#scrollDirectionProperty()} is updated.
	 */
	public void decreasePressed(MouseEvent me) {
		stopAnimations();
		requestFocus();

		VFXScrollBar bar = getNode();
		// First scroll tick
		Animation firstTick = firstTick(bar.getValue() - bar.getUnitIncrement());

		// Detect hold
		holdAnimation = onHold(e -> {
			double range = Math.min(bar.getMax(), bar.getMaxScroll()) - bar.getMin();
			double speed = range / MAX_BUTTONS_SCROLL_DURATION.toMillis();
			double deltaVal = bar.getValue() - bar.getMin();
			double duration = deltaVal / speed;
			scrollAnimation = TimelineBuilder.build()
				.add(KeyFrames.of(duration, bar.valueProperty(), bar.getMin()))
				.getAnimation();
			scrollAnimation.play();
		});
		getAndSetScrollDirection(false);
		firstTick.play();
		holdAnimation.play();
	}

	/**
	 * Action performed when the decrease button is released.
	 * <p>
	 * Simply calls {@link #stopAnimations()}.
	 */
	public void decreaseReleased(MouseEvent me) {
		stopAnimations();
	}

	/**
	 * Action performed when the increase button is pressed.
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()}) and to acquire focus with {@link #requestFocus()}.
	 * <p>
	 * Two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the button you expect the thumb to move
	 * down/right (depending on the orientation) by the specified {@link VFXScrollBar#unitIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted, but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (the delay is specified by {@link #HOLD_DELAY})
	 * and makes the thumb reposition with a {@link Timeline}.
	 * <p></p>
	 * Both the second animation and the inner {@link Timeline} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 * <p></p>
	 * A call to {@link #getAndSetScrollDirection(boolean)} with {@code true} as parameter ensures that the
	 * {@link VFXScrollBar#scrollDirectionProperty()} is updated.
	 */
	public void increasePressed(MouseEvent me) {
		stopAnimations();
		requestFocus();

		VFXScrollBar bar = getNode();
		// First scroll tick
		Animation firstTick = firstTick(bar.getValue() + bar.getUnitIncrement());

		// Detect hold
		holdAnimation = onHold(e -> {
			double max = Math.min(bar.getMax(), bar.getMaxScroll());
			double range = max - bar.getMin();
			double speed = range / MAX_BUTTONS_SCROLL_DURATION.toMillis();
			double deltaVal = max - bar.getValue();
			double duration = deltaVal / speed;
			scrollAnimation = TimelineBuilder.build()
				.add(KeyFrames.of(duration, bar.valueProperty(), max))
				.getAnimation();
			scrollAnimation.play();
		});
		getAndSetScrollDirection(true);
		firstTick.play();
		holdAnimation.play();
	}

	/**
	 * Action performed when the increment button is released.
	 * <p>
	 * Simply calls {@link #stopAnimations()}.
	 */
	public void increaseReleased(MouseEvent me) {
		stopAnimations();
	}

	// MISC

	/**
	 * This method is responsible for switching the orientation {@link PseudoClass} on the scroll bar when the
	 * {@link VFXScrollBar#orientationProperty()} changes.
	 * <p>
	 * The given callback is executed at the end and gives the new orientation as input.
	 */
	public void onOrientationChanged(Consumer<Orientation> callback) {
		VFXScrollBar bar = getNode();
		Orientation o = bar.getOrientation();
		if (o == Orientation.VERTICAL) {
			bar.pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, false);
			bar.pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, true);
		} else {
			bar.pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, false);
			bar.pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, true);
		}
		callback.accept(o);
	}

	/**
	 * Simply enables or disabled the ":dragging" {@link PseudoClass} on the scroll bar depending on the given parameter.
	 */
	protected void onDragging(boolean dragging) {
		getNode().pseudoClassStateChanged(DRAGGING_PSEUDO_CLASS, dragging);
	}

	/**
	 * Stops any currently playing animation, including smooth scroll animations,
	 * hold animation (those responsible for detecting mouse press and hold), and any other
	 * scroll animation (typically the ones created inside hold animations)
	 */
	protected void stopAnimations() {
		if (!smoothScrollAnimations.isEmpty()) {
			smoothScrollAnimations.forEach(Animation::stop);
			smoothScrollAnimations.clear();
		}
		if (holdAnimation != null) {
			holdAnimation.stop();
			holdAnimation = null;
		}
		if (scrollAnimation != null) {
			scrollAnimation.stop();
			scrollAnimation = null;
		}
	}

	/**
	 * Obtains the mouse position from the given {@link MouseEvent} depending on
	 * the scroll bar's orientation.
	 */
	protected double getMousePos(MouseEvent me) {
		return (getNode().getOrientation() == Orientation.VERTICAL) ?
			me.getY() :
			me.getX();
	}

	/**
	 * Obtains the scroll delta from the given {@link ScrollEvent} depending on
	 * the scroll bar's orientation.
	 */
	protected double getScrollDelta(ScrollEvent se) {
		double delta = (getNode().getOrientation() == Orientation.VERTICAL) ?
			se.getDeltaY() :
			se.getDeltaX();
		return (delta != 0) ? delta : (se.getDeltaY() != 0) ? se.getDeltaY() : se.getDeltaX();
	}

	protected int getAndSetScrollDirection(boolean incrementing) {
		VFXScrollBar bar = getNode();
		Orientation o = bar.getOrientation();
		int mul = incrementing ? 1 : -1;
		ScrollUtils.ScrollDirection sd = (o == Orientation.VERTICAL) ?
			(mul == 1) ? ScrollUtils.ScrollDirection.DOWN : ScrollUtils.ScrollDirection.UP :
			(mul == 1) ? ScrollUtils.ScrollDirection.RIGHT : ScrollUtils.ScrollDirection.LEFT;
		bar.setScrollDirection(sd);
		return mul;
	}

	/**
	 * Requests focus for the scroll bar if it's not already focused and if it's focus traversable.
	 */
	protected void requestFocus() {
		VFXScrollBar bar = getNode();
		if (!bar.isFocused() && bar.isFocusTraversable()) bar.requestFocus();
	}

	/**
	 * Convenience method to build a {@link Timeline} animation for the "first tick".
	 * Uses {@link #FIRST_TICK_DURATION} and {@link #FIRST_TICK_CURVE} and moves the scroll bar's value towards the given
	 * target value.
	 */
	protected Animation firstTick(double targetVal) {
		return TimelineBuilder.build()
			.add(KeyFrames.of(
				FIRST_TICK_DURATION,
				getNode().valueProperty(),
				targetVal,
				FIRST_TICK_CURVE
			))
			.getAnimation();
	}

	/**
	 * Convenience method to build a {@link PauseTransition} used to detect "mouse hold" events. The duration is set to
	 * {@link #HOLD_DELAY} and the given handler determines what happens when the animation ends.
	 */
	protected Animation onHold(EventHandler<ActionEvent> handler) {
		return PauseBuilder.build()
			.setDuration(HOLD_DELAY)
			.setOnFinished(handler)
			.getAnimation();
	}

	/**
	 * Convenience method to build a {@link MomentumTransition} using {@link MomentumTransition#fromTime(double, double)}
	 * with the two given parameters. Uses {@link #SMOOTH_SCROLL_CURVE} as the interpolator.
	 */
	protected MomentumTransition withMomentum(double delta, Duration duration) {
		return (MomentumTransition) MomentumTransition.fromTime(delta, duration.toMillis())
			.setInterpolatorFluent(SMOOTH_SCROLL_CURVE);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Action performed when a {@link ScrollEvent} occurs.
	 * <p></p>
	 * First the scroll delta is computed with {@link #getScrollDelta(ScrollEvent)} and in case it's 0
	 * it immediately exits.
	 * <p>
	 * Then we determine the scroll direction with {@link #getAndSetScrollDirection(boolean)} and depending on the
	 * {@link VFXScrollBar#smoothScrollProperty()} the scroll value is adjuster either by a {@link MomentumTransition} or
	 * by the setter.
	 */
	@Override
	public void scroll(ScrollEvent se, Consumer<ScrollEvent> callback) {
		VFXScrollBar bar = getNode();
		double delta = getScrollDelta(se);
		if (delta == 0) return;

		// Determine scroll direction
		// Delta > 0: Decreasing
		// Delta < 0: Increasing
		int mul = getAndSetScrollDirection(delta < 0);
		if (bar.isSmoothScroll()) {
			double deltaVal = bar.getValue() - (bar.getValue() + bar.getUnitIncrement() * -mul);
			Animation mt = withMomentum(deltaVal, SMOOTH_SCROLL_DURATION)
				.setOnUpdate(u -> bar.setValue(bar.getValue() + u));
			mt.setOnFinished(e -> smoothScrollAnimations.remove(mt));
			smoothScrollAnimations.add(mt);
			mt.play();
			if (callback != null) callback.accept(se);
			return;
		}
		bar.setValue(bar.getValue() + bar.getUnitIncrement() * mul);
	}

	@Override
	public void dispose() {
		smoothScrollAnimations.clear();
		super.dispose();
	}
}
