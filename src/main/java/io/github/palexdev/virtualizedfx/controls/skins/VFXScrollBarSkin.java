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

package io.github.palexdev.virtualizedfx.controls.skins;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.input.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollBar.HORIZONTAL_PSEUDO_CLASS;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollBar.VERTICAL_PSEUDO_CLASS;

/**
 * Default skin implementation for {@link VFXScrollBar}.
 * <p></p>
 * There are four components: the track, the thumb and the two buttons.
 *
 * @see LayoutHandler
 */
public class VFXScrollBarSkin extends MFXSkinBase<VFXScrollBar> {
    //================================================================================
    // Properties
    //================================================================================
    private final Region track;
    private final Region thumb;
    private final Region decIcon;
    private final Region incIcon;

    protected LayoutHandler layoutHandler;

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
        decIcon = new IconButton("decrement-icon");
        incIcon = new IconButton("increment-icon");

        // Finalize init
        updateChildren();
        addListeners();
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Adds the following listeners:
     * <p> - A listener to update the layout when these properties change:
     * {@link  VFXScrollBar#buttonsGapProperty()}, {@link VFXScrollBar#visibleAmountProperty()}
     * <p> - A listener on the {@link VFXScrollBar#showButtonsProperty()} to call {@link #updateChildren()} and update the layout
     * <p> - A listener on the {@link VFXScrollBar#orientationProperty()} update the layout, bind the correct translation
     * property for the thumb and rotate the buttons for the icon to face the right direction
     */
    protected void addListeners() {
        VFXScrollBar bar = getSkinnable();
        InvalidationListener ll = i -> bar.requestLayout();

        listeners(
            withListener(bar.buttonsGapProperty(), ll),
            withListener(bar.visibleAmountProperty(), ll),
            onInvalidated(bar.showButtonsProperty())
                .then(b -> updateChildren()),
            onInvalidated(bar.orientationProperty())
                .then(o -> {
                    if (o == Orientation.VERTICAL) {
                        layoutHandler = new VerticalHandler();
                        decIcon.setRotate(0.0);
                        incIcon.setRotate(180.0);
                    } else {
                        layoutHandler = new HorizontalHandler();
                        decIcon.setRotate(-90.0);
                        incIcon.setRotate(90.0);
                    }
                    layoutHandler.init();
                    bar.requestLayout();
                })
                .executeNow()
        );
    }

    /**
     * This is responsible for updating the children list according to the {@link VFXScrollBar#showButtonsProperty()}.
     */
    protected void updateChildren() {
        VFXScrollBar bar = getSkinnable();
        if (bar.isShowButtons()) {
            getChildren().setAll(track, thumb, decIcon, incIcon);
        } else {
            getChildren().setAll(track, thumb);
        }
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
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the decrease icon to call {@link VFXScrollBarBehavior#buttonPressed(MouseEvent, int)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the decrease icon to call {@link VFXScrollBarBehavior#buttonReleased(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_EXITED} on the decrease icon to call {@link VFXScrollBarBehavior#buttonReleased(MouseEvent)}
     * <p>
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} on the increase icon to call {@link VFXScrollBarBehavior#buttonPressed(MouseEvent, int)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} on the increase icon to call {@link VFXScrollBarBehavior#buttonReleased(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_EXITED} on the increase icon to call {@link VFXScrollBarBehavior#buttonReleased(MouseEvent)}
     */
    @Override
    protected void registerBehavior() {
        super.registerBehavior();
        VFXScrollBar bar = getSkinnable();
        VFXScrollBarBehavior behavior = getBehavior();
        events(
            // Scroll Bar
            intercept(bar, ScrollEvent.SCROLL).handle(behavior::scroll),

            // Thumb
            intercept(thumb, MouseEvent.MOUSE_PRESSED).handle(behavior::thumbPressed),
            intercept(thumb, MouseEvent.MOUSE_DRAGGED).handle(behavior::thumbDragged),
            intercept(thumb, MouseEvent.MOUSE_RELEASED).handle(behavior::thumbReleased),

            // Track
            intercept(track, MouseEvent.MOUSE_PRESSED).handle(behavior::trackPressed),
            intercept(track, MouseEvent.MOUSE_RELEASED).handle(behavior::trackReleased),

            // Buttons
            intercept(decIcon, MouseEvent.MOUSE_PRESSED).handle(me -> behavior.buttonPressed(me, -1)),
            intercept(decIcon, MouseEvent.MOUSE_RELEASED).handle(behavior::buttonReleased),
            intercept(decIcon, MouseEvent.MOUSE_EXITED).handle(behavior::buttonReleased),
            intercept(incIcon, MouseEvent.MOUSE_PRESSED).handle(me -> behavior.buttonPressed(me, 1)),
            intercept(incIcon, MouseEvent.MOUSE_RELEASED).handle(behavior::buttonReleased),
            intercept(incIcon, MouseEvent.MOUSE_EXITED).handle(behavior::buttonReleased)
        );
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return layoutHandler != null
            ? layoutHandler.minWidth()
            : super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return layoutHandler != null
            ? layoutHandler.minHeight()
            : super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
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
        if (layoutHandler != null)
            layoutHandler.layout(x, y, w, h);
    }

    @Override
    protected VFXScrollBarBehavior getBehavior() {
        return (VFXScrollBarBehavior) super.getBehavior();
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /**
     * Simplifies the layout algorithm by splitting it in two cases which depend on the {@link VFXScrollBar#orientationProperty()}.
     *
     * @see VerticalHandler
     * @see HorizontalHandler
     */
    public interface LayoutHandler {
        void init();

        void layout(double x, double y, double w, double h);

        default double minWidth() {
            return Region.USE_COMPUTED_SIZE;
        }

        default double minHeight() {
            return Region.USE_COMPUTED_SIZE;
        }
    }

    /**
     * Manages the layout when the scroll bar is vertical.
     */
    public class VerticalHandler implements LayoutHandler {

        /**
         * Makes the thumb translate vertically depending on the bar's value and the heights of the track and thumb itself.
         * <p>
         * Also disables the ":horizontal" {@link PseudoClass} and activates ":vertical".
         */
        @Override
        public void init() {
            VFXScrollBar bar = getSkinnable();
            thumb.translateXProperty().unbind();
            thumb.setTranslateX(0.0);
            thumb.translateYProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double value = bar.getValue();
                    double trackL = track.getHeight() - thumb.getHeight();
                    return NumberUtils.clamp(trackL * value, 0.0, trackL);
                })
                .addSources(bar.valueProperty())
                .addSources(track.heightProperty(), thumb.heightProperty())
                .get()
            );

            pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, true);
            pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, false);
        }

        @Override
        public void layout(double x, double y, double w, double h) {
            VFXScrollBar bar = getSkinnable();

            // Buttons
            boolean showButtons = bar.isShowButtons();
            if (showButtons) {
                // By auto-sizing and then positioning we ignore the bar's padding for the buttons
                // To resize them, we scale them in CSS
                decIcon.autosize();
                positionInArea(decIcon, x, y, w, h, 0, HPos.CENTER, VPos.TOP);
                incIcon.autosize();
                positionInArea(incIcon, x, y, w, h, 0, HPos.CENTER, VPos.BOTTOM);
            }

            double bGap = bar.getButtonsGap();
            double decBtnSize = showButtons ? decIcon.getHeight() + bGap : 0.0;
            double incBtnSize = showButtons ? incIcon.getHeight() + bGap : 0.0;

            // Track
            double trackLength = Math.max(0.0, snapSizeY(h - (decBtnSize + incBtnSize)));
            layoutInArea(track, x, y + decBtnSize, w, trackLength, 0, HPos.CENTER, VPos.TOP);

            // Thumb
            double visibleAmount = bar.getVisibleAmount();
            double thumbW = bar.getWidth();
            double thumbLength = snapSizeY(NumberUtils.clamp(
                trackLength * visibleAmount,
                LayoutUtils.snappedBoundHeight(thumb),
                trackLength
            ));
            thumb.resize(thumbW, thumbLength);
            Position thumbPos = LayoutUtils.computePosition(
                bar, thumb,
                x, y + decBtnSize, w, thumbLength, 0,
                Insets.EMPTY, HPos.CENTER, VPos.TOP,
                false, false
            );
            thumb.relocate(thumbPos.x(), thumbPos.y());
        }

        @Override
        public double minWidth() {
            VFXScrollBar bar = getSkinnable();
            return bar.isShowButtons()
                ? Math.max(LayoutUtils.snappedBoundWidth(decIcon), LayoutUtils.snappedBoundWidth(incIcon))
                : 0.0;
        }

        @Override
        public double minHeight() {
            VFXScrollBar bar = getSkinnable();
            double bSize = bar.isShowButtons()
                ? LayoutUtils.snappedBoundHeight(decIcon) + LayoutUtils.snappedBoundHeight(decIcon) + bar.getButtonsGap() * 2
                : 0.0;
            return snappedTopInset() + bSize + snappedBottomInset();
        }
    }

    /**
     * Manages the layout when the scroll bar is horizontal.
     */
    public class HorizontalHandler implements LayoutHandler {

        /**
         * Makes the thumb translate horizontally depending on the bar's value and the widths of the track and thumb itself.
         * <p>
         * Also disables the ":vertical" {@link PseudoClass} and activates ":horizontal".
         */
        @Override
        public void init() {
            VFXScrollBar bar = getSkinnable();
            thumb.translateYProperty().unbind();
            thumb.setTranslateY(0.0);
            thumb.translateXProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double value = bar.getValue();
                    double trackL = track.getWidth() - thumb.getWidth();
                    return NumberUtils.clamp(trackL * value, 0.0, trackL);
                })
                .addSources(bar.valueProperty())
                .addSources(track.widthProperty(), thumb.widthProperty())
                .get()
            );

            pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, false);
            pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, true);
        }

        @Override
        public void layout(double x, double y, double w, double h) {
            VFXScrollBar bar = getSkinnable();

            // Buttons
            boolean showButtons = bar.isShowButtons();
            if (showButtons) {
                // By auto-sizing and then positioning we ignore the bar's padding for the buttons
                // To resize them, we scale them in CSS
                decIcon.autosize();
                positionInArea(decIcon, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
                incIcon.autosize();
                positionInArea(incIcon, x, y, w, h, 0, HPos.RIGHT, VPos.CENTER);
            }

            double bGap = bar.getButtonsGap();
            double decBtnSize = showButtons ? decIcon.getWidth() + bGap : 0.0;
            double incBtnSize = showButtons ? incIcon.getWidth() + bGap : 0.0;

            // Track
            double trackLength = Math.max(0.0, snapSizeX(w - (decBtnSize + incBtnSize)));
            layoutInArea(track, x + decBtnSize, y, trackLength, h, 0, HPos.LEFT, VPos.CENTER);

            // Thumb
            double visibleAmount = bar.getVisibleAmount();
            double thumbH = bar.getHeight();
            double thumbLength = snapSizeX(NumberUtils.clamp(
                trackLength * visibleAmount,
                LayoutUtils.snappedBoundWidth(thumb),
                trackLength
            ));
            thumb.resize(thumbLength, thumbH);
            Position thumbPos = LayoutUtils.computePosition(
                bar, thumb,
                x + decBtnSize, y, thumbLength, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.CENTER,
                false, false
            );
            thumb.relocate(thumbPos.x(), thumbPos.y());
        }

        @Override
        public double minWidth() {
            VFXScrollBar bar = getSkinnable();
            double bSize = bar.isShowButtons()
                ? LayoutUtils.snappedBoundWidth(decIcon) + LayoutUtils.snappedBoundWidth(incIcon) + bar.getButtonsGap() * 2
                : 0.0;
            return snappedLeftInset() + bSize + snappedRightInset();
        }

        @Override
        public double minHeight() {
            VFXScrollBar bar = getSkinnable();
            return bar.isShowButtons()
                ? Math.max(LayoutUtils.snappedBoundHeight(decIcon), LayoutUtils.snappedBoundHeight(incIcon))
                : 0.0;
        }
    }

    protected static class IconButton extends Region {
        public IconButton(String iconStyleClass) {
            getStyleClass().addAll("icon", iconStyleClass);
            setFocusTraversable(true);
        }

        @Override
        protected double computeMaxWidth(double height) {
            return prefWidth(-1);
        }

        @Override
        protected double computeMaxHeight(double width) {
            return prefHeight(-1);
        }
    }
}
