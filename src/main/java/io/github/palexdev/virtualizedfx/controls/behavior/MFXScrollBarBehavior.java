/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of MaterialFX (https://github.com/palexdev/MaterialFX).
 *
 * MaterialFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MaterialFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.controls.behavior;

import io.github.palexdev.mfxcore.animations.Animations.KeyFrames;
import io.github.palexdev.mfxcore.animations.Animations.PauseBuilder;
import io.github.palexdev.mfxcore.animations.Animations.TimelineBuilder;
import io.github.palexdev.mfxcore.animations.BezierEasing;
import io.github.palexdev.mfxcore.animations.Interpolators;
import io.github.palexdev.mfxcore.animations.MomentumTransition;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils.ScrollDirection;
import io.github.palexdev.virtualizedfx.controls.MFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.behavior.base.BehaviorBase;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.geometry.Orientation;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Concrete implementation of {@link BehaviorBase}.
 * <p></p>
 * This offers all the methods to manage scrolling and smooth scrolling, track press/release,
 * buttons press, thumb press/drag/release
 * <p></p>
 */
public class MFXScrollBarBehavior extends BehaviorBase<MFXScrollBar> {
	//================================================================================
	// Properties
	//================================================================================
	private final ObjectProperty<ScrollDirection> direction = new SimpleObjectProperty<>();

	private Animation holdAnimation;
	private Animation scrollAnimation;

	private final Queue<Animation> smoothScrollAnimations = new ConcurrentLinkedQueue<>();
	private double dragStart;

	//================================================================================
	// Constructors
	//================================================================================
	public MFXScrollBarBehavior(MFXScrollBar sb) {
		super(sb);
	}

	//================================================================================
	// Methods
	//================================================================================
	// SCROLL //

	/**
	 * Action performed when a {@link ScrollEvent} occurs.
	 * <p></p>
	 * First the scroll delta is computes with {@link #getScrollDelta(ScrollEvent)} and in case it's 0
	 * it immediately returns.
	 * <p>
	 * Then computes a multiplier integer (1 or -1 depending on the direction of the scroll, see {@link #updateScrollDirection(int)}
	 * for more info).
	 * <p>
	 * If smooth scroll is active {@link #smoothScroll(int)} is called and this exits, otherwise
	 * ths scroll bar's value is adjusted as follows: {@code val = val + unitIncrement * mul}.
	 */
	public void scroll(ScrollEvent se) {
		MFXScrollBar sb = getNode();
		boolean isSmooth = sb.isSmoothScroll();
		double delta = getScrollDelta(se);
		if (delta == 0) return;

		// Determine scroll direction
		// Delta > 0: Decreasing
		// Delta < 0: Increasing
		int mul = delta > 0 ? -1 : 1;
		updateScrollDirection(mul);

		if (isSmooth) {
			smoothScroll(mul);
			return;
		}
		sb.setValue(sb.getValue() + sb.getUnitIncrement() * mul);
	}

	// THUMB //

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
	 * The first steps are to get the actual mouse position with {@link #getMousePos(MouseEvent)},
	 * then compute the traveled distance since the press event as {@code currentPos - dragStart}.
	 * <p>
	 * At this point we determine the scroll direction and update the property with {@link #updateScrollDirection(int)},
	 * we also update the dragging property to activate the ":dragging" PseudoClass.
	 * <p>
	 * Finally, the scroll bar's value is updated by converting the previously computed traveled distance in the
	 * range of scroll bar's values, see {@link #posToVal(double)}, then update the value as follows:
	 * {@code val = val + deltaVal * mul}.
	 */
	public void thumbDragged(MouseEvent me) {
		MFXScrollBar sb = getNode();
		double position = getMousePos(me);
		double delta = position - dragStart;
		int mul = delta < 0 ? -1 : 1;
		double deltaVal = posToVal(Math.abs(delta));
		updateScrollDirection(mul);
		setDragging(true);
		sb.setValue(sb.getValue() + deltaVal * mul);
	}

	/**
	 * Action performed when the thumb is released.
	 * <p></p>
	 * The dragStart position is reset to 0.0, and the dragging property set to false.
	 */
	public void thumbReleased() {
		dragStart = 0.0;
		setDragging(false);
	}

	// TRACK //

	/**
	 * Action performed when the track is pressed.
	 * <p></p>
	 * The first steps are to stop any animation (see {@link #stopAnimations()}) then {@link #requestFocus()}.
	 * Then we check whether smooth scroll and track smooth scroll features have been enabled, in such case the method
	 * exists as the scroll will be handled by {@link #trackReleased(MouseEvent)}.
	 * <p></p>
	 * Otherwise, we get the mouse position with {@link #getMousePos(MouseEvent)}, we check if the position is an increment
	 * or a decrement with {@link #isIncrementing(double)} and then we update the scroll direction with {@link #updateScrollDirection(int)}.
	 * <p>
	 * The new scroll bar's value is computed as {@code val = val + trackIncrement * mul} but it's not yet set.
	 * <p>
	 * At this point two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the track you expect the thumb to move
	 * towards the mouse position by the specified {@link MFXScrollBar#trackIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (by default the delay is 350 milliseconds)
	 * and makes the thumb reposition towards the mouse position with a {@link MomentumTransition}. Before doing so, of course,
	 * it checks if the thumb is already at the mouse position or beyond, in such cases it simply does nothing.
	 * <p></p>
	 * Both the second animation and the inner {@link MomentumTransition} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 */
	public void trackPressed(MouseEvent me) {
		stopAnimations();
		requestFocus();

		MFXScrollBar sb = getNode();
		if (sb.isSmoothScroll() && sb.isTrackSmoothScroll()) return;

		double position = getMousePos(me);
		boolean increment = isIncrementing(position);
		int mul = increment ? 1 : -1;
		updateScrollDirection(mul);

		double newVal = sb.getValue() + sb.getTrackIncrement() * mul;
		Animation firstTick = TimelineBuilder.build()
				.add(KeyFrames.of(300, sb.valueProperty(), newVal, Interpolators.EASE_OUT_SINE))
				.getAnimation();

		holdAnimation = PauseBuilder.build()
				.setDuration(350)
				.setOnFinished(event -> {
					if (increment && getThumbPos() > position ||
							!increment && getThumbPos() < position ||
							sb.getValue() == sb.getMax()) return;

					double target = increment ? posToVal(position - thumbLength()) : posToVal(position);
					double delta = Math.abs(target - sb.getValue());

					MomentumTransition mt = MomentumTransition.fromDeceleration(delta, -1.5e-6);
					mt.setOnUpdate(upd -> {
						double val = sb.getValue() + upd * mul;
						double clamped = increment ? Math.min(val, target) : Math.max(val, target);
						sb.setValue(clamped);
					});
					mt.setInterpolator(Interpolators.EASE_IN_OUT_SINE);
					scrollAnimation = mt;
					scrollAnimation.play();
				}).getAnimation();

		firstTick.play();
		holdAnimation.play();
	}

	/**
	 * Action performed when the track is released
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()}).
	 * <p></p>
	 * The rest of the method executes only if both the smooth scroll and track smooth scroll features are enabled.
	 * <p>
	 * As usual, we get the mouse position with {@link #getMousePos(MouseEvent)}, check if is increment or decrement with
	 * {@link #isIncrementing(double)} and update the direction with {@link #updateScrollDirection(int)}.
	 * <p>
	 * Then we compute the target position using {@link #posToVal(double)}, and the difference between
	 * the target value and the current value: {@code delta = target - val}
	 * <p>
	 * Finally, the scroll bar's value is adjusted with a {@link MomentumTransition}.
	 * <p></p>
	 * A little side note on the conversion from mouse pos to value: the value is computed differently
	 * depending on whether we should increment or decrement the value.
	 * <p></p>
	 * <p> Incrementing: {@code posToVal(pos - thumbLength)} so that the cursor always stays below the thumb (or at least
	 * approximately)
	 * <p> Decrementing: {@code posToVal(pos)} so that the cursor is above the thumb (or at least approximately)
	 */
	public void trackReleased(MouseEvent me) {
		stopAnimations();

		MFXScrollBar sb = getNode();
		if (sb.isSmoothScroll() && sb.isTrackSmoothScroll()) {
			double position = getMousePos(me);
			boolean increment = isIncrementing(position);
			int mul = increment ? 1 : -1;
			updateScrollDirection(mul);


			double target = increment ? posToVal(position - thumbLength()) : posToVal(position);
			double delta = Math.abs(target - sb.getValue());

			MomentumTransition mt = MomentumTransition.fromDeceleration(delta, -2e-6);
			mt.setOnUpdate(upd -> {
				double val = sb.getValue() + upd * mul;
				double clamp = increment ? Math.min(val, target) : Math.max(val, target);
				sb.setValue(clamp);
			});
			mt.setInterpolator(Interpolators.EASE_IN_OUT_SINE);
			scrollAnimation = mt;
			scrollAnimation.play();
		}
	}

	// BUTTONS //

	/**
	 * Action performed when the decrease button is pressed.
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()})
	 * <p>
	 * The new scroll bar's value is computed as {@code val = val - unitIncrement} but it's not yet set.
	 * <p>
	 * At this point two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the button you expect the thumb to move
	 * up/left (depending on the orientation) by the specified {@link MFXScrollBar#unitIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (by default the delay is 350 milliseconds)
	 * and makes the thumb reposition with a {@link Timeline}.
	 * <p></p>
	 * Both the second animation and the inner {@link Timeline} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 */
	public void decPressed() {
		stopAnimations();

		MFXScrollBar sb = getNode();
		double newVal = sb.getValue() - sb.getUnitIncrement();
		Animation firstTick = TimelineBuilder.build()
				.add(KeyFrames.of(50, sb.valueProperty(), newVal, Interpolators.EASE_IN_OUT_SINE))
				.getAnimation();

		holdAnimation = PauseBuilder.build()
				.setDuration(350)
				.setOnFinished(event -> {
					double duration = sb.getValue() / (sb.getUnitIncrement() / 50);
					scrollAnimation = TimelineBuilder.build()
							.add(KeyFrames.of(duration, sb.valueProperty(), sb.getMin()))
							.getAnimation();
					scrollAnimation.play();
				}).getAnimation();

		updateScrollDirection(-1);
		firstTick.play();
		holdAnimation.play();
	}

	/**
	 * Action performed when the increase button is pressed.
	 * <p></p>
	 * The first step is to stop any animation (see {@link #stopAnimations()})
	 * <p>
	 * The new scroll bar's value is computed as {@code val = val + unitIncrement} but it's not yet set.
	 * <p>
	 * At this point two animations are built:
	 * <p>
	 * The first animation is responsible for the "first tick". When you press the button you expect the thumb to move
	 * down/right (depending on the orientation) by the specified {@link MFXScrollBar#unitIncrementProperty()}. Then we must also
	 * consider what happens when the value has been adjusted but the mouse is still pressed. Here's when the second animation
	 * comes into play. This animation basically detects if the mouse is still pressed (by default the delay is 350 milliseconds)
	 * and makes the thumb reposition with a {@link Timeline}.
	 * <p></p>
	 * Both the second animation and the inner {@link Timeline} are assigned to variables so that
	 * if any event occurs in the meanwhile which requires the animations to stop it can be done.
	 */
	public void incPressed() {
		stopAnimations();

		MFXScrollBar sb = getNode();
		double newVal = NumberUtils.clamp(sb.getValue() + sb.getUnitIncrement(), sb.getMin(), sb.getMax());
		Animation firstTick = TimelineBuilder.build()
				.add(KeyFrames.of(50, sb.valueProperty(), newVal, Interpolators.EASE_IN_OUT_SINE))
				.getAnimation();

		holdAnimation = PauseBuilder.build()
				.setDuration(350)
				.setOnFinished(event -> {
					double duration = (sb.getMax() - sb.getValue()) / (sb.getUnitIncrement() / 50);
					scrollAnimation = TimelineBuilder.build()
							.add(KeyFrames.of(duration, sb.valueProperty(), sb.getMax()))
							.getAnimation();
					scrollAnimation.play();
				}).getAnimation();

		updateScrollDirection(1);
		firstTick.play();
		holdAnimation.play();
	}

	// MISC //

	/**
	 * This is called by {@link #scroll(ScrollEvent)} if the smooth scroll feature is enabled.
	 * <p></p>
	 * The target value is computes as {@code target = val + unitIncrement * direction}, then we compute
	 * the difference between the current value and the target (which gives the "travel distance") as absolute number.
	 * <p>
	 * The scroll bar's value is then adjusted with a {@link MomentumTransition}.
	 */
	public void smoothScroll(int direction) {
		MFXScrollBar sb = getNode();

		double target = sb.getValue() + sb.getUnitIncrement() * direction;
		double delta = Math.abs(sb.getValue() - target);
		MomentumTransition mt = MomentumTransition.fromTime(delta, 530);
		mt.setInterpolator(BezierEasing.EASE);
		mt.setOnUpdate(upd -> sb.setValue(sb.getValue() + upd * direction));
		mt.setOnFinished(event -> smoothScrollAnimations.remove(mt));
		smoothScrollAnimations.add(mt);
		mt.play();
	}

	/**
	 * Stops any currently playing animation, including: smooth scroll animations,
	 * hold animation (those responsible for detecting mouse press and hold), and any other
	 * scroll animation (typically the ones created inside hold animations)
	 */
	public void stopAnimations() {
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
	 * Responsible for updating the {@link #directionProperty()} from an integer value that is
	 * either 1 or -1.
	 * <p>
	 * This value maps as follows:
	 * <p> For VERTICAL orientation: 1 -> {@link ScrollDirection#DOWN}, -1 -> {@link ScrollDirection#UP}
	 * <p> For HORIZONTAL orientation: 1 -> {@link ScrollDirection#RIGHT}, -1 -> {@link ScrollDirection#LEFT}
	 */
	protected void updateScrollDirection(int i) {
		Orientation o = getNode().getOrientation();
		ScrollDirection sd = (o == Orientation.VERTICAL) ?
				(i == 1) ? ScrollDirection.DOWN : ScrollDirection.UP :
				(i == 1) ? ScrollDirection.RIGHT : ScrollDirection.LEFT;
		setDirection(sd);
	}

	/**
	 * Requests focus for the scroll bar if it's not already focused and if it's focus traversable.
	 */
	protected void requestFocus() {
		MFXScrollBar sb = getNode();
		if (!sb.isFocused() && sb.isFocusTraversable()) sb.requestFocus();
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
	 * Core method responsible for converting the given position to a value which is in the
	 * scroll bar's values range (between min and max).
	 */
	private double posToVal(double position) {
		MFXScrollBar sb = getNode();
		double maxPos = getTrackLength() - thumbLength();
		double clampedPos = NumberUtils.clamp(position, 0.0, maxPos);
		return NumberUtils.mapOneRangeToAnother(
				clampedPos,
				DoubleRange.of(0.0, maxPos),
				DoubleRange.of(sb.getMin(), sb.getMax())
		);
	}

	/**
	 * @return the scroll bar's thumb length
	 */
	private double thumbLength() {
		MFXScrollBar sb = getNode();
		return getTrackLength() * sb.getVisibleAmount();
	}

	/**
	 * Checks if the given position is greater than the current thumb position, {@link MFXScrollBar#thumbPosProperty()}.
	 */
	private boolean isIncrementing(double position) {
		return position > getThumbPos();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public void dispose() {
		smoothScrollAnimations.clear();
		super.dispose();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public ScrollDirection getDirection() {
		return direction.get();
	}

	/**
	 * Specifies the current or last scrolling direction.
	 */
	public ReadOnlyObjectProperty<ScrollDirection> directionProperty() {
		return direction;
	}

	protected void setDirection(ScrollDirection direction) {
		this.direction.set(direction);
	}

	protected double getThumbPos() {
		return getNode().getThumbPos();
	}

	protected double getTrackLength() {
		return getNode().getTrackLength();
	}

	protected boolean isDragging() {
		return getNode().isDragging();
	}

	/**
	 * Specifies whether the thumb is being dragged.
	 */
	protected ReadOnlyBooleanProperty draggingProperty() {
		return getNode().draggingProperty();
	}

	protected void setDragging(boolean dragging) {
		((BooleanProperty) draggingProperty()).set(dragging);
	}
}
