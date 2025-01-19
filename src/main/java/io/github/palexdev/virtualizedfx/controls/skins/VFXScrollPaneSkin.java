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

package io.github.palexdev.virtualizedfx.controls.skins;

import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollPaneBehavior;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.HBarPos;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.VBarPos;
import io.github.palexdev.virtualizedfx.utils.ScrollBounds;
import javafx.animation.Animation;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollPane.COMPACT_MODE_PSEUDO_CLASS;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollPane.DRAG_TO_SCROLL_PSEUDO_CLASS;

/**
 * Default skin implementation for {@link VFXScrollPane}.
 * <p></p>
 * There are three components: the content container also referred to as the "viewport", and the two scroll bars.
 * <p></p>
 * All the components are positioned and sized according to a custom layout strategy
 * which varies based on several properties, five are the main ones: {@link VFXScrollPane#layoutModeProperty()},
 * {@link VFXScrollPane#vBarPosProperty()}, {@link VFXScrollPane#hBarPosProperty()},
 * {@link VFXScrollPane#vBarPolicyProperty()} and {@link VFXScrollPane#hBarPolicyProperty()}.
 * <p>
 * <p> - The LayoutMode specifies if the scroll pane should reserve extra space for the
 * scroll bars ({@link LayoutMode#DEFAULT}), or they can be positioned above the content ({@link LayoutMode#COMPACT}).
 * This last option is especially useful in combination with {@link VFXScrollPane#autoHideBarsProperty()}.
 * <p> - The vBarPos allows to specify the position of the vertical scroll bar LEFT/RIGHT(default)
 * <p> - The hBarPos allows to specify the position of the horizontal scroll bar TOP/BOTTOM(default)
 * <p> - The vBarPolicy allows to specify the visibility policy for the vertical scroll bar
 * <p> - The hBarPolicy allows to specify the visibility policy for the horizontal scroll bar
 */
public class VFXScrollPaneSkin extends SkinBase<VFXScrollPane, VFXScrollPaneBehavior> {
    //================================================================================
    // Properties
    //================================================================================
    private final Pane viewport;
    private final Rectangle clip;
    private final VFXScrollBar hBar;
    private final VFXScrollBar vBar;
    private Node content;
    protected double DEFAULT_SIZE = 100.0;

    // Animations
    protected double SHOW_HIDE_DURATION = 250;
    private Animation hShow;
    private Animation hHide;
    private Animation vShow;
    private Animation vHide;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollPaneSkin(VFXScrollPane pane) {
        super(pane);
        this.content = pane.getContent();

        // Init viewport
        viewport = new Pane() {
            @Override
            protected void layoutChildren() {
                if (content == null) return;

                double x = snappedLeftInset();
                double y = snappedTopInset();
                double w = getWidth() - snappedLeftInset() - snappedRightInset();
                double h = getHeight() - snappedTopInset() - snappedBottomInset();
                if (content instanceof VFXContainer<?>) {
                    content.resizeRelocate(x, y, w, h);
                } else {
                    ScrollBounds bounds = pane.getContentBounds();
                    w = pane.isFitToWidth() ? Math.max(w, bounds.contentWidth()) : bounds.contentWidth();
                    h = pane.isFitToHeight() ? Math.max(h, bounds.contentHeight()) : bounds.contentHeight();
                    content.resizeRelocate(x, y, w, h);
                }
            }
        };
        viewport.getStyleClass().add("viewport");

        clip = new Rectangle();
        clip.widthProperty().bind(pane.widthProperty().subtract(snappedLeftInset() + snappedRightInset()));
        clip.heightProperty().bind(pane.heightProperty().subtract(snappedTopInset() + snappedBottomInset()));
        clip.arcWidthProperty().bind(pane.clipBorderRadiusProperty());
        clip.arcHeightProperty().bind(pane.clipBorderRadiusProperty());
        viewport.setClip(clip);

        // Init scroll bars
        // double lambda go brrrr, hahahaha
        hBar = new VFXScrollBar(Orientation.HORIZONTAL);
        hBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
            .setMapper(() -> () -> pane.getHBarBehavior().apply(hBar))
            .addSources(pane.hBarBehaviorProperty())
            .get()
        );

        vBar = new VFXScrollBar(Orientation.VERTICAL);
        vBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
            .setMapper(() -> () -> pane.getVBarBehavior().apply(vBar))
            .addSources(pane.vBarBehaviorProperty())
            .get()
        );

        if (pane.isAutoHideBars()) {
            hBar.setOpacity(0.0);
            vBar.setOpacity(0.0);
        }

        // Init PseudoClasses
        pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, pane.getLayoutMode() == LayoutMode.COMPACT);
        pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, pane.isDragToScroll());

        // Animations
        hShow = Animations.TimelineBuilder.build()
            .add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), 1.0))
            .setOnFinished(event -> {
                if (hBar.isHover() || hBar.isPressed()) return;
                hHide.playFromStart();
            })
            .getAnimation();
        hHide = Animations.TimelineBuilder.build()
            .setDelay(600)
            .add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), 0.0))
            .getAnimation();

        vShow = Animations.TimelineBuilder.build()
            .add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), 1.0))
            .setOnFinished(event -> {
                if (vBar.isHover() || vBar.isPressed()) return;
                vHide.playFromStart();
            })
            .getAnimation();
        vHide = Animations.TimelineBuilder.build()
            .setDelay(600)
            .add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), 0.0))
            .getAnimation();

        // Finalize init
        addListeners();
        getChildren().setAll(viewport, vBar, hBar);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Creates the following bindings:
     * <p> - binds the {@link VFXScrollPane#viewportSizeProperty()} to the viewport's width and height.
     * <p> - binds all the horizontal scroll bar's properties to the ones of the scroll pane.
     * The {@link VFXScrollPane#hValProperty()} is bound bidirectionally to the {@link VFXScrollBar#valueProperty()}
     * <p> - binds all the vertical scroll bar's properties to the ones of the scroll pane.
     * The {@link VFXScrollPane#vValProperty()} is bound bidirectionally to the {@link VFXScrollBar#valueProperty()}
     * <p></p>
     * <p>
     * Adds the following listeners:
     * <p> - A listener to update the layout to the following properties: {@link VFXScrollPane#hBarPolicyProperty()},
     * {@link VFXScrollPane#vBarPolicyProperty()}, {@link VFXScrollPane#hBarPosProperty()}, {@link VFXScrollPane#vBarPosProperty()},
     * {@link VFXScrollPane#hBarPaddingProperty()}, {@link VFXScrollPane#vBarPaddingProperty()}
     * <p> - A listener on the {@link VFXScrollPane#contentProperty()} to update the viewport
     * <p> - A listener on the {@link VFXScrollPane#contentBoundsProperty()} to update the scroll bars' visibility if the
     * {@link VFXScrollPane#autoHideBarsProperty()} is active
     * <p> - A listener on the {@link VFXScrollPane#layoutModeProperty()} to update the layout and enable/disable the
     * ':compact' {@link PseudoClass}
     * <p> - A listener on the {@link VFXScrollPane#dragToScrollProperty()} to enable/disable the ':drag-to-scroll' {@link PseudoClass}
     * <p> - A listener on the {@link VFXScrollPane#hValProperty()} to show the horizontal scroll bar if the
     * {@link VFXScrollPane#autoHideBarsProperty()} is active
     * <p> - A listener on the horizontal scroll bar's {@link VFXScrollBar#hoverProperty()} to show/hide it when the
     * {@link VFXScrollPane#autoHideBarsProperty()} is active
     * <p> - A listener on the {@link VFXScrollPane#vValProperty()} to show the vertical scroll bar if the
     * {@link VFXScrollPane#autoHideBarsProperty()} is active
     * <p> - A listener on the vertical scroll bar's {@link VFXScrollBar#hoverProperty()} to show/hide it when the
     * {@link VFXScrollPane#autoHideBarsProperty()} is active
     */
    private void addListeners() {
        VFXScrollPane pane = getSkinnable();

        // Bindings
        ((SizeProperty) pane.viewportSizeProperty()).bind(ObjectBindingBuilder.<Size>build()
            .setMapper(() -> {
                double hInsets = viewport.snappedLeftInset() + viewport.snappedRightInset();
                double vInsets = viewport.snappedTopInset() + viewport.snappedBottomInset();
                return Size.of(
                    Math.max(0.0, viewport.getWidth() - hInsets),
                    Math.max(0.0, viewport.getHeight() - vInsets)
                );
            })
            .addSources(viewport.widthProperty(), viewport.heightProperty())
            .get()
        );

        // HBar
        hBar.scrollBoundsProperty().bind(pane.contentBoundsProperty());
        hBar.visibleProperty().bind(BooleanBindingBuilder.build()
            .setMapper(() -> hBar.getVisibleAmount() < 1.0 && pane.getHBarPolicy() != ScrollBarPolicy.NEVER)
            .addSources(pane.contentBoundsProperty(), pane.hBarPolicyProperty())
            .get()
        );
        hBar.minProperty().bind(pane.hMinProperty());
        pane.hValProperty().bindBidirectional(hBar.valueProperty());
        hBar.maxProperty().bind(pane.hMaxProperty());
        hBar.showButtonsProperty().bind(pane.showButtonsProperty());
        hBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        hBar.trackIncrementProperty().bind(pane.hTrackIncrementProperty());
        hBar.unitIncrementProperty().bind(pane.hUnitIncrementProperty());
        hBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        hBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

        // VBar
        vBar.scrollBoundsProperty().bind(pane.contentBoundsProperty());
        vBar.visibleProperty().bind(BooleanBindingBuilder.build()
            .setMapper(() -> vBar.getVisibleAmount() < 1.0 && pane.getVBarPolicy() != ScrollBarPolicy.NEVER)
            .addSources(pane.contentBoundsProperty(), pane.vBarPolicyProperty())
            .get()
        );
        vBar.minProperty().bind(pane.vMinProperty());
        pane.vValProperty().bindBidirectional(vBar.valueProperty());
        vBar.maxProperty().bind(pane.vMaxProperty());
        vBar.showButtonsProperty().bind(pane.showButtonsProperty());
        vBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        vBar.trackIncrementProperty().bind(pane.vTrackIncrementProperty());
        vBar.unitIncrementProperty().bind(pane.vUnitIncrementProperty());
        vBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        vBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

        // Listeners
        InvalidationListener ll = o -> pane.requestLayout();
        listeners(
            // Base
            onInvalidated(pane.contentProperty())
                .then(c -> {
                    if (content != null) {
                        content = null;
                        viewport.getChildren().clear();
                    }
                    if (c != null) {
                        content = c;
                        viewport.getChildren().setAll(content);
                    }
                    updateScrollBindings();
                })
                .executeNow(),
            onChanged(pane.contentBoundsProperty())
                .condition((o, n) -> pane.isAutoHideBars())
                .then((o, n) -> {
                    if (o.contentWidth() != n.contentWidth()) {
                        hShow.playFromStart();
                    }
                    if (o.contentHeight() != n.contentHeight()) {
                        vShow.playFromStart();
                    }
                }),
            // PseudoClasses
            onInvalidated(pane.layoutModeProperty())
                .then(m -> {
                    pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, m == LayoutMode.COMPACT);
                    pane.requestLayout();
                }),
            onInvalidated(pane.dragToScrollProperty())
                .then(v -> pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, v)),
            // Layout
            withListener(pane.fitToWidthProperty(), ll),
            withListener(pane.fitToHeightProperty(), ll),
            withListener(pane.hBarPolicyProperty(), ll),
            withListener(pane.vBarPolicyProperty(), ll),
            withListener(pane.hBarPosProperty(), ll),
            withListener(pane.vBarPosProperty(), ll),
            withListener(pane.hBarPaddingProperty(), ll),
            withListener(pane.vBarPaddingProperty(), ll),
            // Animations
            onInvalidated(pane.autoHideBarsProperty())
                .then(v -> {
                    if (v) {
                        if (!hBar.isHover() && !hBar.isPressed()) hHide.playFromStart();
                        if (!vBar.isHover() && !vBar.isPressed()) vHide.playFromStart();
                    } else {
                        hBar.setOpacity(1.0);
                        vBar.setOpacity(1.0);
                    }
                }),
            // HBar
            onInvalidated(pane.hValProperty())
                .condition(v -> pane.isAutoHideBars())
                .then(v -> {
                    hHide.stop();
                    hShow.play();
                }),
            onInvalidated(hBar.hoverProperty())
                .condition(v -> pane.isAutoHideBars())
                .then(v -> {
                    if (!v && hBar.isPressed()) {
                        onChanged(hBar.pressedProperty())
                            .then((ov, nv) -> {
                                if (!nv) hHide.playFromStart();
                            })
                            .oneShot()
                            .listen();
                        return;
                    }

                    if (v) {
                        hHide.stop();
                        hShow.playFromStart();
                    } else if (!Animations.isPlaying(hShow)) {
                        hHide.playFromStart();
                    }
                }),
            // VBar
            onInvalidated(pane.vValProperty())
                .then(v -> {
                    if (!pane.isAutoHideBars()) return;
                    vHide.stop();
                    vShow.play();
                }),
            onInvalidated(vBar.hoverProperty())
                .condition(v -> pane.isAutoHideBars())
                .then(v -> {
                    if (!v && vBar.isPressed()) {
                        onChanged(vBar.pressedProperty())
                            .then((ov, nv) -> {
                                if (!nv) vHide.playFromStart();
                            })
                            .oneShot()
                            .listen();
                        return;
                    }

                    if (v) {
                        vHide.stop();
                        vShow.playFromStart();
                    } else if (!Animations.isPlaying(vShow)) {
                        vHide.playFromStart();
                    }
                })
        );
    }

    /**
     * Responsible for binding the viewport's translation properties if the content is not a virtualized container.
     * <p>
     * This is necessary to make the scroll pane work with traditional nodes. That's because virtualized containers have
     * an in-built virtual scroll mechanism, so, in their case, it's not this viewport to scroll.
     */
    protected void updateScrollBindings() {
        VFXScrollPane pane = getSkinnable();
        viewport.translateXProperty().unbind();
        viewport.translateYProperty().unbind();
        clip.translateXProperty().unbind();
        clip.translateYProperty().unbind();

        Node content = pane.getContent();
        if (content != null && !(content instanceof VFXContainer<?>)) {
            viewport.translateXProperty().bind(hBar.valueProperty().multiply(-1.0));
            viewport.translateYProperty().bind(vBar.valueProperty().multiply(-1.0));
            clip.translateXProperty().bind(hBar.valueProperty());
            clip.translateYProperty().bind(vBar.valueProperty());
        }

    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /**
     * Initializes the behavior by calling {@link VFXScrollPaneBehavior#init()} and by registering the following handlers:
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} to call {@link VFXScrollBarBehavior#mousePressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_DRAGGED} to call {@link VFXScrollBarBehavior#mouseDragged(MouseEvent)}
     * <p> - intercepts events of type {@link ScrollEvent#SCROLL} and re-routes such events to the appropriate scroll bar,
     * see {@link VFXScrollBarBehavior#scroll(ScrollEvent)}
     */
    @Override
    protected void initBehavior(VFXScrollPaneBehavior behavior) {
        VFXScrollPane pane = getSkinnable();
        behavior.init();
        events(
            intercept(viewport, MouseEvent.MOUSE_PRESSED)
                .asFilter()
                .process(behavior::mousePressed),
            intercept(viewport, MouseEvent.MOUSE_DRAGGED)
                .asFilter()
                .process(behavior::mouseDragged),
            intercept(viewport, ScrollEvent.SCROLL).process(e -> behavior.scroll(e, c -> {
                // Re-route scroll events to the appropriate scroll bar's behavior
                Orientation o = pane.getOrientation();
                VFXScrollBar target = switch (o) {
                    case VERTICAL -> c.isShiftDown() ? hBar : vBar;
                    case HORIZONTAL -> c.isShiftDown() ? vBar : hBar;
                };
                if (target.isVisible()) target.getBehavior().scroll(c);
            })),
            intercept(pane, KeyEvent.KEY_PRESSED).process(behavior::keyPressed)
        );
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + DEFAULT_SIZE + LayoutUtils.boundWidth(vBar) + rightInset;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + DEFAULT_SIZE + LayoutUtils.boundHeight(hBar) + bottomInset;
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollPane pane = getSkinnable();
        Node content = pane.getContent();
        LayoutMode layoutMode = pane.getLayoutMode();
        ScrollBarPolicy vBarPolicy = pane.getVBarPolicy();

        double contentW = (content != null) ? LayoutUtils.boundWidth(content) : 0.0;
        double prefW = leftInset + contentW + rightInset;
        if (layoutMode == LayoutMode.DEFAULT && vBarPolicy != ScrollBarPolicy.NEVER)
            prefW += LayoutUtils.boundWidth(vBar);
        return prefW;
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollPane pane = getSkinnable();
        Node content = pane.getContent();
        LayoutMode layoutMode = pane.getLayoutMode();
        ScrollBarPolicy hBarPolicy = pane.getHBarPolicy();

        double contentH = (content != null) ? LayoutUtils.boundHeight(content) : 0.0;
        double prefH = topInset + contentH + bottomInset;
        if (layoutMode == LayoutMode.DEFAULT && hBarPolicy != ScrollBarPolicy.NEVER)
            prefH += LayoutUtils.boundHeight(hBar);
        return prefH;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXScrollPane vsp = getSkinnable();

        // VBar
        boolean vVisible = vBar.isVisible() && vsp.getVBarPolicy() != ScrollBarPolicy.NEVER;
        VBarPos vPos = vsp.getVBarPos();
        double vBarX = 0, vBarY = 0, vBarW = 0, vBarH = 0;
        if (vVisible) {
            Insets vPadding = vsp.getVBarPadding();
            double vOffset = vsp.getVBarOffset();
            vBarW = LayoutUtils.snappedBoundWidth(vBar);
            vBarH = h - vPadding.getTop() - vPadding.getBottom() - vOffset;
            vBarX = vPos == VBarPos.LEFT ? snappedLeftInset() + vPadding.getLeft() : w - vBarW - vPadding.getRight();
            vBarY = snappedTopInset() + vPadding.getTop() + vOffset;
        }

        // HBar
        boolean hVisible = hBar.isVisible() && vsp.getHBarPolicy() != ScrollBarPolicy.NEVER;
        HBarPos hPos = vsp.getHBarPos();
        double hBarX = 0, hBarY = 0, hBarW = 0, hBarH = 0;
        if (hVisible) {
            Insets hPadding = vsp.getHBarPadding();
            double hOffset = vsp.getHBarOffset();
            hBarW = w - hPadding.getLeft() - hPadding.getRight() - hOffset;
            hBarH = LayoutUtils.snappedBoundHeight(hBar);
            hBarX = snappedLeftInset() + hPadding.getLeft() + hOffset;
            hBarY = hPos == HBarPos.TOP ? snappedTopInset() + hPadding.getTop() : h - hBarH - hPadding.getBottom();
        }

        // Prevent bars from overlapping
        if (hVisible) {
            if (hPos == HBarPos.TOP) vBarY += hBarH;
            vBarH -= hBarH;
        }
        if (vVisible) {
            if (vPos == VBarPos.LEFT) hBarX += vBarW;
            hBarW -= vBarW;
        }

        vBar.resizeRelocate(vBarX, vBarY, vBarW, vBarH);
        hBar.resizeRelocate(hBarX, hBarY, hBarW, hBarH);

        // Viewport
        LayoutMode mode = vsp.getLayoutMode();
        if (mode == LayoutMode.DEFAULT) {
            double vwX = snappedLeftInset() + ((vPos == VBarPos.LEFT) ? vBarW : 0.0);
            double vwY = snappedTopInset() + ((hPos == HBarPos.TOP) ? hBarH : 0.0);
            double vwW = w - vBarW;
            double vwH = h - hBarH;
            viewport.resizeRelocate(vwX, vwY, vwW, vwH);
        } else {
            viewport.resizeRelocate(x, y, w, h);
        }
    }

    @Override
    public void dispose() {
        vShow.stop();
        hShow.stop();
        vHide.stop();
        hHide.stop();
        vShow = null;
        hShow = null;
        vHide = null;
        hHide = null;
        content = null;
        super.dispose();
    }
}
