/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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
        dragStart = Position.of(me.getSceneX(), me.getSceneY());
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
            double meX = me.getSceneX();
            double xDelta = -(meX - dragStart.getX());
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
                dragStart.setX(meX);
            }
        }

        if (isScrollBarVisible(Orientation.VERTICAL)) {
            double meY = me.getSceneY();
            double yDelta = -(meY - dragStart.getY());
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
                dragStart.setY(meY);
            }
        }
    }

    /**
     * Action performed when {@link KeyEvent#KEY_PRESSED} events occurs.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        VFXScrollPane pane = getNode();
        Orientation o = pane.getOrientation();
        boolean canHScroll = isScrollBarVisible(Orientation.HORIZONTAL);
        boolean canVScroll = isScrollBarVisible(Orientation.VERTICAL);
        switch (e.getCode()) {
            case HOME -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.setVVal(0);
                if (o == Orientation.HORIZONTAL && canHScroll) pane.setHVal(0);
            }
            case END -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.setVVal(Double.MAX_VALUE);
                if (o == Orientation.HORIZONTAL && canHScroll) pane.setHVal(Double.MAX_VALUE);
            }
            case PAGE_UP -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.vtDecrement();
                if (o == Orientation.HORIZONTAL && canHScroll) pane.htDecrement();
            }
            case PAGE_DOWN -> {
                if (o == Orientation.VERTICAL && canVScroll) pane.vtIncrement();
                if (o == Orientation.HORIZONTAL && canHScroll) pane.htIncrement();
            }
            case KeyCode c when c == UP && canVScroll -> pane.vuDecrement();
            case KeyCode c when c == DOWN && canVScroll -> pane.vuIncrement();
            case KeyCode c when c == LEFT && canHScroll -> pane.huDecrement();
            case KeyCode c when c == RIGHT && canHScroll -> pane.huIncrement();
            default -> {}
        }
    }
}
