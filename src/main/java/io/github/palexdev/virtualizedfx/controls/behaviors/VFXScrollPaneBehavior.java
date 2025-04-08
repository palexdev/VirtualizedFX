/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.controls.behaviors;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxeffects.animations.MomentumTransition;
import io.github.palexdev.mfxeffects.animations.base.Curve;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollPaneSkin;
import javafx.animation.Interpolator;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import static javafx.scene.input.KeyCode.*;

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
    private Orientation mainDragAxis;
    private double dragThreshold = 16.0;

    protected Duration DRAG_SMOOTH_SCROLL_DURATION = M3Motion.EXTRA_LONG1;
    protected Interpolator DRAG_SMOOTH_SCROLL_CURVE = Curve.EASE_BOTH;
    protected double SMOOTH_DRAG_SENSIBILITY = 1.5;

    private Size viewportSize;
    private boolean canVScroll = true;
    private boolean canHScroll = true;

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
     * Stores both the mouse position and the scroll values which will be needed by {@link #mouseDragged(MouseEvent)}
     * to compute how much to scroll if the drag to scroll feature is active.
     */
    @Override
    public void mousePressed(MouseEvent me) {
        VFXScrollPane pane = getNode();
        dragStart = Position.of(me.getSceneX(), me.getSceneY());
        initValues = Position.of(pane.getHValue(), pane.getVValue());
    }

    /**
     * Action performed when a {@link MouseEvent#MOUSE_DRAGGED} event occurs.
     * <p></p>
     * Adjusts both the vertical or the horizontal scroll values according to the mouse movement,
     * immediately via setters or with an animation ({@link #withMomentum(double)}) if the "smooth drag to scroll feature" is active.
     * <p>
     * There are several conditions that get checked here:
     * <p> 1) Exits if either the feature is off or the content is null
     * <p> 2) Does not allow diagonal scrolling for a more pleasant UX and so the dominant axis is determined by how much
     * the mouse moved vertically and horizontally since the press of the mouse.
     * <p> 3) The scroll starts only after a certain threshold, by default 16px.
     * <p> 4) The scroll occurs only if the bar for the determined axis is visible. This is managed by the
     * {@link #canVScroll} and {@link #canHScroll} flags. The default skin does not allow the scroll when a bar is hidden,
     * either because the content is smaller than the viewport on its axis or if the policies hide the bar.
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
        VFXScrollPane pane = getNode();
        if (!pane.isDragToScroll() || pane.getContent() == null) return;

        double meX = me.getSceneX();
        double xDelta = -(meX - dragStart.getX());
        double meY = me.getSceneY();
        double yDelta = -(meY - dragStart.getY());
        Size cb = getContentBounds();

        // Do not allow diagonal scrolling as it feels unnatural and may result in a clumsy UX
        // Also, do not start scrolling until a certain threshold is surpassed
        //
        // The main axis is determined only one time on drag and reset only when the mouse is released
        if (mainDragAxis == null && (Math.abs(xDelta) > dragThreshold || Math.abs(yDelta) > dragThreshold)) {
            mainDragAxis = Math.abs(xDelta) > Math.abs(yDelta)
                ? Orientation.HORIZONTAL
                : Orientation.VERTICAL;

            // When the threshold is surpassed, reset the dragStart to the current position but skip the scrolling
            // on for this frame.
            dragStart = Position.of(meX, meY);
            return;
        }

        if (canHScroll && mainDragAxis == Orientation.HORIZONTAL) {
            double maxX = Math.max(0.0, cb.getWidth() - viewportSize.getWidth());
            double deltaVal = NumberUtils.mapOneRangeToAnother(
                NumberUtils.clamp(Math.abs(xDelta), 0.0, maxX),
                DoubleRange.of(0.0, maxX),
                DoubleRange.of(0.0, 1.0)
            );
            int mul = xDelta > 0 ? 1 : -1;

            if (!pane.isDragSmoothScroll()) {
                pane.setHValue(initValues.getX() + deltaVal * mul);
            } else {
                MomentumTransition mt = withMomentum(deltaVal * SMOOTH_DRAG_SENSIBILITY);
                mt.setOnUpdate(u -> {
                    double target = pane.getHValue() + u * mul;
                    double clamped = NumberUtils.clamp(target, pane.getHMin(), pane.getHMax());
                    pane.setHValue(clamped);
                    if (target != clamped) mt.stop();
                });
                mt.play();
                dragStart.setX(meX);
            }
        }

        if (canVScroll && mainDragAxis == Orientation.VERTICAL) {
            double maxY = Math.max(0.0, cb.getHeight() - viewportSize.getHeight());
            double deltaVal = NumberUtils.mapOneRangeToAnother(
                NumberUtils.clamp(Math.abs(yDelta), 0.0, maxY),
                DoubleRange.of(0.0, maxY),
                DoubleRange.of(0.0, 1.0)
            );
            int mul = yDelta > 0 ? 1 : -1;

            if (!pane.isDragSmoothScroll()) {
                pane.setVValue(initValues.getY() + deltaVal * mul);
            } else {
                MomentumTransition mt = withMomentum(deltaVal * SMOOTH_DRAG_SENSIBILITY);
                mt.setOnUpdate(u -> {
                    double target = pane.getVValue() + u * mul;
                    double clamped = NumberUtils.clamp(target, pane.getVMin(), pane.getVMax());
                    pane.setVValue(clamped);
                    if (target != clamped) mt.stop();
                });
                mt.play();
                dragStart.setY(meY);
            }
        }
    }

    /**
     * Action performed when a {@link MouseEvent#MOUSE_RELEASED} event occurs.
     * <p></p>
     * Resets the properties needed by {@link #mouseDragged(MouseEvent)}.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        dragStart = Position.of(-1, -1);
        initValues = Position.origin();
        mainDragAxis = null;
    }

    /**
     * Action performed when {@link KeyEvent#KEY_PRESSED} events occurs.
     */
    @Override
    public void keyPressed(KeyEvent ke) {
        VFXScrollPane pane = getNode();
        Orientation o = pane.getMainAxis();
        switch (ke.getCode()) {
            case HOME -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.setVValue(0);
                if (o == Orientation.HORIZONTAL && canHScroll) pane.setHValue(0);
            }
            case END -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.setVValue(1.0);
                if (o == Orientation.HORIZONTAL && canHScroll) pane.setHValue(1.0);
            }
            case PAGE_UP -> {
                if (o == Orientation.VERTICAL && canVScroll)
                    pane.setVValue(pane.getVValue() - pane.getVTrackIncrement());
                if (o == Orientation.HORIZONTAL && canHScroll)
                    pane.setHValue(pane.getHValue() - pane.getHTrackIncrement());
            }
            case PAGE_DOWN -> {
                if (o == Orientation.VERTICAL && canVScroll)
                    pane.setVValue(pane.getVValue() + pane.getVTrackIncrement());
                if (o == Orientation.HORIZONTAL && canHScroll)
                    pane.setHValue(pane.getHValue() + pane.getHTrackIncrement());
            }
            case KeyCode c when c == UP && canVScroll -> pane.setVValue(pane.getVValue() - pane.getVUnitIncrement());
            case KeyCode c when c == DOWN && canVScroll -> pane.setVValue(pane.getVValue() + pane.getVUnitIncrement());
            case KeyCode c when c == LEFT && canHScroll -> pane.setHValue(pane.getHValue() - pane.getHUnitIncrement());
            case KeyCode c when c == RIGHT && canHScroll -> pane.setHValue(pane.getHValue() + pane.getHUnitIncrement());
            default -> {}
        }
    }

    /**
     * @return the appropriate sizes for the scroll pane's content. For virtualized containers ({@link VFXContainer})
     * the values are given by the {@link VFXContainer#virtualMaxXProperty()} and the {@link VFXContainer#virtualMaxYProperty()}
     */
    protected Size getContentBounds() {
        VFXScrollPane pane = getNode();
        Node content = pane.getContent();
        return switch (content) {
            case null -> Size.empty();
            case VFXContainer<?> c -> Size.of(c.getVirtualMaxX(), c.getVirtualMaxY());
            default -> {
                Bounds b = content.getLayoutBounds();
                yield Size.of(b.getWidth(), b.getHeight());
            }
        };
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @see #setViewportSize(Size)
     */
    public Size getViewportSize() {
        return viewportSize;
    }

    /**
     * This is called by the default skin to make the viewport's size available to the behavior. These values are important
     * for the drag to scroll feature to work properly.
     */
    public void setViewportSize(Size viewportSize) {
        this.viewportSize = viewportSize;
    }

    /**
     * @see #setCanVScroll(boolean)
     */
    public boolean isCanVScroll() {
        return canVScroll;
    }

    /**
     * This is called by the default skin to make the behavior class aware of the vertical scroll bar's visibility.
     * This information is important for the drag to scroll feature to work properly.
     */
    public void setCanVScroll(boolean canVScroll) {
        this.canVScroll = canVScroll;
    }

    /**
     * @see #setCanHScroll(boolean)
     */
    public boolean isCanHScroll() {
        return canHScroll;
    }

    /**
     * This is called by the default skin to make the behavior class aware of the horizontal scroll bar's visibility.
     * This information is important for the drag to scroll feature to work properly.
     */
    public void setCanHScroll(boolean canHScroll) {
        this.canHScroll = canHScroll;
    }

    /**
     * @see #setDragThreshold(double)
     */
    public double getDragThreshold() {
        return dragThreshold;
    }

    /**
     * Sets the number of pixels that act as a threshold before the scroll happens on drag.
     *
     * @see #mouseDragged(MouseEvent)
     */
    public void setDragThreshold(double dragThreshold) {
        this.dragThreshold = dragThreshold;
    }
}
