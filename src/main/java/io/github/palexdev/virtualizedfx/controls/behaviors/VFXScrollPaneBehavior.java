package io.github.palexdev.virtualizedfx.controls.behaviors;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxeffects.animations.MomentumTransition;
import io.github.palexdev.mfxeffects.animations.base.Curve;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollPaneSkin;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import io.github.palexdev.virtualizedfx.utils.ScrollBounds;
import javafx.animation.Interpolator;
import javafx.geometry.Orientation;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Extension of {@link BehaviorBase} and default behavior implementation for {@link VFXScrollPane}.
 * <p></p>
 * Most of the work related to scrolling is already handled by the scroll bars present in the {@link VFXScrollPaneSkin}.
 * This just implements the methods necessary to make the {@link VFXScrollPane#dragToScrollProperty()} feature work.
 * <p>
 * You can fine-tune the {@link VFXScrollPane#dragSmoothScrollProperty()} by changing the properties:
 * <p> - {@link #DRAG_SMOOTH_SCROLL_DURATION}
 * <p> - {@link #DRAG_SMOOTH_SCROLL_CURVE}
 * <p> - {@link #SMOOTH_DRAG_SENSIBILITY} (this is basically a multiplier to scroll more/less and make the smooth scroll
 * more significant)
 */
public class VFXScrollPaneBehavior extends BehaviorBase<VFXScrollPane> {
	//================================================================================
	// Properties
	//================================================================================
	private Position initValues = Position.of(0, 0);
	private Position dragStart = Position.of(-1, -1);

	protected Duration DRAG_SMOOTH_SCROLL_DURATION = M3Motion.EXTRA_LONG1;
	protected Interpolator DRAG_SMOOTH_SCROLL_CURVE = Curve.EASE_BOTH;
	protected double SMOOTH_DRAG_SENSIBILITY = 1.25;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXScrollPaneBehavior(VFXScrollPane pane) {
		super(pane);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Checks whether the scroll bar for the given orientation is visible.
	 * <p>
	 * Since this has no access to the skin and thus to the scroll bars, the check is done by considering the content's
	 * visible amount given by {@link ScrollBounds#visibleAmount(Orientation)} (specified by {@link VFXScrollPane#contentBoundsProperty()}),
	 * and the {@link ScrollBarPolicy}.
	 */
	protected boolean isScrollBarVisible(Orientation o) {
		VFXScrollPane pane = getNode();
		ScrollBounds bounds = pane.getContentBounds();
		double va = bounds.visibleAmount(o);
		ScrollBarPolicy policy = (o == Orientation.VERTICAL) ? pane.getVBarPolicy() : pane.getHBarPolicy();
		return va < 1.0 && policy != ScrollBarPolicy.NEVER;
	}

	/**
	 * Convenience method to build a {@link MomentumTransition} using {@link MomentumTransition#fromTime(double, double)}
	 * with the given parameter. Uses {@link #DRAG_SMOOTH_SCROLL_DURATION} as the duration and {@link #DRAG_SMOOTH_SCROLL_CURVE}
	 * as the interpolator.
	 */
	protected MomentumTransition withMomentum(double delta) {
		return (MomentumTransition) MomentumTransition.fromTime(delta, DRAG_SMOOTH_SCROLL_DURATION.toMillis())
			.setInterpolatorFluent(DRAG_SMOOTH_SCROLL_CURVE);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Action performed when a {@link MouseEvent#MOUSE_PRESSED} event occurs.
	 * <p></p>
	 * If the {@link VFXScrollPane#dragToScrollProperty()} is active, stores both the mouse position and the scroll values
	 * which will be needed by {@link #mouseDragged(MouseEvent)} to compute how much to scroll.
	 */
	@Override
	public void mousePressed(MouseEvent me) {
		VFXScrollPane pane = getNode();
		if (!pane.isDragToScroll()) {
			dragStart = Position.of(-1, -1);
			initValues = Position.origin();
			return;
		}
		dragStart = Position.of(me.getX(), me.getY());
		initValues = Position.of(pane.getHVal(), pane.getVVal());
	}

	/**
	 * Action performed when a {@link MouseEvent#MOUSE_DRAGGED} event occurs.
	 * <p></p>
	 * Depending on the {@link VFXScrollPane#dragSmoothScrollProperty()} and whether it is possible to scroll in a certain
	 * direction (by checking with {@link #isScrollBarVisible(Orientation)}), adjusts both the vertical and horizontal
	 * scroll values according to the mouse movement, immediately via setters or with an animation ({@link #withMomentum(double)}).
	 * <p></p>
	 * The displacement is computed as follows:
	 * <p> - {@code mouseX - dragStartX} for the horizontal axis
	 * <p> - {@code mouseY - dragStartY} for the vertical axis
	 * <p></p>
	 * Also, if the values are adjusted by animations, every time one is played the {@code dragStart} positions are updated
	 * for a better user experience.
	 *
	 * @see #mousePressed(MouseEvent)
	 */
	@Override
	public void mouseDragged(MouseEvent me) {
		if (dragStart.getX() == -1 || dragStart.getY() == -1) return;

		VFXScrollPane pane = getNode();
		if (isScrollBarVisible(Orientation.HORIZONTAL)) {
			double xDelta = -(me.getX() - dragStart.getX());
			if (!pane.isDragSmoothScroll()) {
				pane.setHVal(initValues.getX() + xDelta);
			} else {
				MomentumTransition mt = withMomentum(xDelta * SMOOTH_DRAG_SENSIBILITY);
				mt.setOnUpdate(u -> {
					double target = pane.getHVal() + u;
					double clamped = NumberUtils.clamp(target, pane.getHMin(), pane.getHMax());
					pane.setHVal(clamped);
					if (target != clamped) mt.stop();
				});
				mt.play();
				dragStart.setX(me.getX());
			}
		}

		if (isScrollBarVisible(Orientation.VERTICAL)) {
			double yDelta = -(me.getY() - dragStart.getY());
			if (!pane.isDragSmoothScroll()) {
				pane.setVVal(initValues.getY() + yDelta);
			} else {
				MomentumTransition mt = withMomentum(yDelta * SMOOTH_DRAG_SENSIBILITY);
				mt.setOnUpdate(u -> {
					double target = pane.getVVal() + u;
					double clamped = NumberUtils.clamp(target, pane.getVMin(), pane.getVMax());
					pane.setVVal(clamped);
					if (target != clamped) mt.stop();
				});
				mt.play();
				dragStart.setY(me.getY());
			}
		}
	}
}
