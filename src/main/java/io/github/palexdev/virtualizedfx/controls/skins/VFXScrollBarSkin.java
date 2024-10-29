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

import java.util.function.Consumer;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.mfxresources.builders.IconWrapperBuilder;
import io.github.palexdev.mfxresources.fonts.MFXIconWrapper;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Default skin implementation for {@link VFXScrollBar}.
 * <p></p>
 * There are four components: the track, the thumb and the two buttons.
 */
public class VFXScrollBarSkin extends SkinBase<VFXScrollBar, VFXScrollBarBehavior> {
    //================================================================================
    // Properties
    //================================================================================
    private final Region track;
    private final Region thumb;
    private final MFXIconWrapper decIcon;
    private final MFXIconWrapper incIcon;

    private final double DEFAULT_LENGTH = 100.0;
    private double trackLength;
    private double thumbLength;
    protected DoubleBinding thumbPos;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollBarSkin(VFXScrollBar bar) {
        super(bar);

        // Init track and thumb
        track = new Region();
        track.getStyleClass().add("track");
        thumb = new Region();
        thumb.getStyleClass().add("thumb");

        // Init buttons
        decIcon = IconWrapperBuilder.build()
            .setIcon(IconBuilder.build().get())
            .enableRippleGenerator(true)
            .addStyleClasses("decrement-icon")
            .get();
        incIcon = IconWrapperBuilder.build()
            .setIcon(IconBuilder.build().get())
            .enableRippleGenerator(true)
            .addStyleClasses("increment-icon")
            .get();

        // Finalize init
        updateChildren();
        addListeners();
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Creates the following bindings:
     * <p> - binds the {@link VFXScrollBar#visibleProperty()} to make the component visible only if the visible amount
     * is greater than 0 and lesser than 1.0. The visible amount is given by {@link VFXScrollBar#getVisibleAmount()}
     * (only if it is not already bound!!).
     * <p></p>
     * <p>
     * Adds the following listeners:
     * <p> - A listener to update the layout to the following properties: {@link VFXScrollBar#scrollBoundsProperty()},
     * {@link VFXScrollBar#buttonsGapProperty()}
     * <p> - A listener on the {@link VFXScrollBar#showButtonsProperty()} to call {@link #updateChildren()} and update the layout
     * <p> - A listener on the {@link VFXScrollBar#orientationProperty()} to call
     * {@link VFXScrollBarBehavior#onOrientationChanged(Consumer)} and update the layout. The callback action will bind
     * the translateX/Y (depending on the orientation) properties of the thumb node to the {@code thumbPos} property
     * <p></p>
     * Uses a one-shot listener ({@link When} construct) to initialize the thumb position once the skin has been initialized
     * (to be more precise, once both width and height are greater than 0).
     */
    private void addListeners() {
        VFXScrollBar bar = getSkinnable();
        InvalidationListener ll = i -> bar.requestLayout();

        // Bindings
        thumbPos = DoubleBindingBuilder.build()
            .setMapper(() -> valToPos(bar.getValue()))
            .addSources(bar.valueProperty(), bar.buttonsGapProperty())
            .addSources(thumb.widthProperty(), thumb.heightProperty())
            .get();
        // Bind visibility only if it's not already bound
        // Quick workaround for visibility bound overwrite in VFXScrollPaneSkin
        if (!bar.visibleProperty().isBound()) {
            bar.visibleProperty().bind(BooleanBindingBuilder.build()
                .setMapper(() -> {
                    double visibleAmount = bar.getVisibleAmount();
                    return visibleAmount > 0 && visibleAmount < 1.0;
                })
                .addSources(bar.scrollBoundsProperty(), bar.orientationProperty())
                .get()
            );
        }

        // Listeners
        listeners(
            withListener(bar.scrollBoundsProperty(), ll),
            withListener(bar.buttonsGapProperty(), ll),
            onInvalidated(bar.showButtonsProperty())
                .then(b -> {
                    updateChildren();
                    thumbPos.invalidate();
                    bar.requestLayout();
                }),
            onInvalidated(bar.orientationProperty())
                .then(o -> {
                    getBehavior().onOrientationChanged(v -> {
                        if (o == Orientation.VERTICAL) {
                            thumb.translateXProperty().unbind();
                            thumb.translateYProperty().bind(thumbPos);
                        } else {
                            thumb.translateYProperty().unbind();
                            thumb.translateXProperty().bind(thumbPos);
                        }
                    });
                    bar.requestLayout();
                })
        );

        onChanged(track.layoutBoundsProperty())
            .condition((o, n) -> n.getWidth() > 0 && n.getHeight() > 0)
            .then((o, n) -> thumbPos.invalidate())
            .oneShot();
    }

    /**
     * This is responsible for updating the children list according to the {@link VFXScrollBar#showButtonsProperty()}.
     */
    protected void updateChildren() {
        VFXScrollBar bar = getSkinnable();
        boolean showButtons = bar.isShowButtons();
        if (showButtons) {
            getChildren().setAll(track, thumb, decIcon, incIcon);
        } else {
            getChildren().setAll(track, thumb);
        }
    }

    /**
     * This core method is responsible for converting the {@link VFXScrollBar#valueProperty()} to the coordinate
     * (x or y depending on the orientation) at which the thumb must be positioned. This is done by converting the value
     * from the values' range given by {@code [0.0, maxScroll]} to the positions' range given by {@code [0.0, trackLength - thumbLength]}.
     * <p>
     * The final value will also take into account the {@link VFXScrollBar#buttonsGapProperty()} if the
     * {@link VFXScrollBar#showButtonsProperty()} is {@code true}.
     */
    private double valToPos(double val) {
        VFXScrollBar bar = getSkinnable();
        double bGap = bar.isShowButtons() ? bar.getButtonsGap() : 0.0;
        double max = bar.getMaxScroll();
        try {
            DoubleRange from = DoubleRange.of(0.0, max);
            DoubleRange to = DoubleRange.of(0.0, trackLength - thumbLength);
            double pos = NumberUtils.mapOneRangeToAnother(val, from, to);
            return Double.isNaN(pos) ? 0.0 : NumberUtils.clamp(pos, bGap, trackLength - thumbLength - bGap);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    /**
     * Responsible for computing the scroll bar's length (width for VERTICAL, height for HORIZONTAL).
     */
    private double getLength(Orientation orientation) {
        VFXScrollBar bar = getSkinnable();
        double length;
        double padding;
        if (orientation == Orientation.VERTICAL) {
            padding = snappedLeftInset() + snappedRightInset();
            length = LayoutUtils.boundWidth(thumb) + padding;
        } else {
            padding = snappedTopInset() + snappedBottomInset();
            length = LayoutUtils.boundHeight(thumb) + padding;
        }

        if (bar.isShowButtons())
            length = Math.max(length, Math.max(decIcon.getSize(), incIcon.getSize()) + padding);
        return length;
    }

    /**
     * @return the minimum allowed track length, 2 * {@link #getLength(Orientation)}
     */
    private double minTrackLength(Orientation orientation) {
        return getLength(orientation) * 2.0;
    }

    /**
     * @return the minimum allowed thumb length, 1.5 * {@link #getLength(Orientation)}
     */
    private double minThumbLength(Orientation orientation) {
        return getLength(orientation) * 1.5;
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /**
     * Initializes the behavior by calling {@link VFXScrollBarBehavior#init()} and by registering the following handlers:
     * <p> - intercepts events of type {@link ScrollEvent#SCROLL} to call {@link VFXScrollBarBehavior#scroll(ScrollEvent)}
     * <p>
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the thumb to call {@link VFXScrollBarBehavior#thumbPressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_DRAGGED} on the thumb to call {@link VFXScrollBarBehavior#thumbDragged(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the thumb to call {@link VFXScrollBarBehavior#thumbReleased(MouseEvent)}
     * <p>
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the track to call {@link VFXScrollBarBehavior#trackPressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the track to call {@link VFXScrollBarBehavior#trackReleased(MouseEvent)}
     * <p>
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the decrease icon to call {@link VFXScrollBarBehavior#decreasePressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the decrease icon to call {@link VFXScrollBarBehavior#decreaseReleased(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_EXITED} on the decrease icon to call {@link VFXScrollBarBehavior#decreaseReleased(MouseEvent)}
     * <p>
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the increase icon to call {@link VFXScrollBarBehavior#increasePressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the increase icon to call {@link VFXScrollBarBehavior#increaseReleased(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_EXITED} on the increase icon to call {@link VFXScrollBarBehavior#increaseReleased(MouseEvent)}
     * <p></p>
     * Also calls {@link VFXScrollBarBehavior#onOrientationChanged(Consumer)} to initialize the pseudo classes and bind
     * the thumb's translate position.
     */
    @Override
    protected void initBehavior(VFXScrollBarBehavior behavior) {
        behavior.init();
        behavior.onOrientationChanged(v -> {
            if (v == Orientation.VERTICAL) {
                thumb.translateXProperty().unbind();
                thumb.translateYProperty().bind(thumbPos);
            } else {
                thumb.translateYProperty().unbind();
                thumb.translateXProperty().bind(thumbPos);
            }
        });

        VFXScrollBar bar = getSkinnable();
        events(
            // Scroll Bar
            intercept(bar, ScrollEvent.SCROLL).process(behavior::scroll),

            // Thumb
            intercept(thumb, MouseEvent.MOUSE_PRESSED).process(behavior::thumbPressed),
            intercept(thumb, MouseEvent.MOUSE_DRAGGED).process(behavior::thumbDragged),
            intercept(thumb, MouseEvent.MOUSE_RELEASED).process(behavior::thumbReleased),

            // Track
            intercept(track, MouseEvent.MOUSE_PRESSED).process(behavior::trackPressed),
            intercept(track, MouseEvent.MOUSE_RELEASED).process(behavior::trackReleased),

            // Icons
            intercept(decIcon, MouseEvent.MOUSE_PRESSED).process(behavior::decreasePressed),
            intercept(decIcon, MouseEvent.MOUSE_RELEASED).process(behavior::decreaseReleased),
            intercept(decIcon, MouseEvent.MOUSE_EXITED).process(behavior::decreaseReleased),
            intercept(incIcon, MouseEvent.MOUSE_PRESSED).process(behavior::increasePressed),
            intercept(incIcon, MouseEvent.MOUSE_RELEASED).process(behavior::increaseReleased),
            intercept(incIcon, MouseEvent.MOUSE_EXITED).process(behavior::increaseReleased)
        );
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar bar = getSkinnable();
        Orientation o = bar.getOrientation();
        boolean showButtons = bar.isShowButtons();
        double bGap = showButtons ? bar.getButtonsGap() : 0.0;
        return (o == Orientation.VERTICAL) ?
            getLength(Orientation.VERTICAL) :
            leftInset + decIcon.getSize() + minTrackLength(Orientation.HORIZONTAL) + incIcon.getSize() + rightInset + (bGap * 2);
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar bar = getSkinnable();
        Orientation o = bar.getOrientation();
        boolean showButtons = bar.isShowButtons();
        double bGap = showButtons ? bar.getButtonsGap() : 0.0;
        return (o == Orientation.VERTICAL) ?
            topInset + decIcon.getSize() + minTrackLength(Orientation.VERTICAL) + incIcon.getSize() + bottomInset + (bGap * 2) :
            getLength(Orientation.HORIZONTAL);
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar bar = getSkinnable();
        Orientation o = bar.getOrientation();
        boolean showButtons = bar.isShowButtons();
        double bGap = showButtons ? bar.getButtonsGap() : 0.0;
        return (o == Orientation.VERTICAL) ?
            getLength(Orientation.VERTICAL) :
            leftInset + DEFAULT_LENGTH + rightInset + (bGap * 2);
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar bar = getSkinnable();
        Orientation o = bar.getOrientation();
        boolean showButtons = bar.isShowButtons();
        double bGap = showButtons ? bar.getButtonsGap() : 0.0;
        return (o == Orientation.VERTICAL) ?
            topInset + DEFAULT_LENGTH + bottomInset + (bGap * 2) :
            getLength(Orientation.HORIZONTAL);
    }

    @Override
    protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar scrollBar = getSkinnable();
        return (scrollBar.getOrientation() == Orientation.VERTICAL) ?
            scrollBar.prefWidth(-1) :
            Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXScrollBar scrollBar = getSkinnable();
        return (scrollBar.getOrientation() == Orientation.VERTICAL) ?
            Double.MAX_VALUE :
            scrollBar.prefHeight(-1);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXScrollBar bar = getSkinnable();
        Orientation o = bar.getOrientation();
        boolean showButtons = bar.isShowButtons();

        double decSize = showButtons ? decIcon.getSize() : 0.0;
        double incSize = showButtons ? incIcon.getSize() : 0.0;

        if (showButtons) {
            HPos decHPos = (o == Orientation.VERTICAL) ? HPos.CENTER : HPos.LEFT;
            HPos incHPos = (o == Orientation.VERTICAL) ? HPos.CENTER : HPos.RIGHT;
            VPos decVPos = (o == Orientation.VERTICAL) ? VPos.TOP : VPos.CENTER;
            VPos incVPos = (o == Orientation.VERTICAL) ? VPos.BOTTOM : VPos.CENTER;
            layoutInArea(decIcon, x, y, w, h, 0, decHPos, decVPos);
            layoutInArea(incIcon, x, y, w, h, 0, incHPos, incVPos);
        }

        double visibleAmount = bar.getVisibleAmount();
        if (o == Orientation.VERTICAL) {
            trackLength = snapSizeY(h - (decSize + incSize));
            layoutInArea(track, x, y + decSize, w, trackLength, 0, HPos.CENTER, VPos.TOP);

            thumbLength = snapSizeY(NumberUtils.clamp(trackLength * visibleAmount, minThumbLength(Orientation.VERTICAL), trackLength));
            Position position = LayoutUtils.computePosition(
                bar, thumb,
                x, y + decSize, w, thumbLength, 0,
                Insets.EMPTY, HPos.CENTER, VPos.TOP,
                false, true
            );
            double thumbW = LayoutUtils.boundWidth(thumb);
            thumb.resizeRelocate(position.getX(), position.getY(), thumbW, thumbLength);
        } else {
            trackLength = snapSizeX(w - (decSize + incSize));
            layoutInArea(track, x + decSize, y, trackLength, h, 0, HPos.LEFT, VPos.CENTER);

            thumbLength = snapSizeX(NumberUtils.clamp(trackLength * visibleAmount, minThumbLength(Orientation.HORIZONTAL), trackLength));
            Position position = LayoutUtils.computePosition(
                bar, thumb,
                x + decSize, y, thumbLength, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.CENTER,
                false, true
            );
            double thumbH = LayoutUtils.boundWidth(thumb);
            thumb.resizeRelocate(position.getX(), position.getY(), thumbLength, thumbH);
        }
    }

    @Override
    public void dispose() {
        thumb.translateXProperty().unbind();
        thumb.translateYProperty().unbind();
        if (thumbPos != null) thumbPos.dispose();
        thumbPos = null;
        super.dispose();
    }
}
