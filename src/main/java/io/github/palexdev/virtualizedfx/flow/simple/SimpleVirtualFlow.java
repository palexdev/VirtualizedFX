/*
 * Copyright (C) 2021 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.ResourceManager;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.base.OrientationHelper;
import io.github.palexdev.virtualizedfx.flow.base.OrientationHelper.HorizontalHelper;
import io.github.palexdev.virtualizedfx.flow.base.OrientationHelper.VerticalHelper;
import io.github.palexdev.virtualizedfx.flow.base.VirtualFlow;
import io.github.palexdev.virtualizedfx.utils.AnimationUtils;
import io.github.palexdev.virtualizedfx.utils.ExecutionUtils;
import io.github.palexdev.virtualizedfx.utils.NumberUtils;
import io.github.palexdev.virtualizedfx.utils.ScrollUtils.ScrollDirection;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.github.palexdev.virtualizedfx.utils.ScrollUtils.determineScrollDirection;
import static io.github.palexdev.virtualizedfx.utils.ScrollUtils.isTrackPad;

/**
 * Simple implementation of {@link VirtualFlow}.
 * <p>
 * This VirtualFlow creates Cells of type {@link Cell}, it's features are:
 * <p> - The items list is managed automatically (insertions, removals or updates to the items)
 * <p> - The cell factory can be changed any time
 * <p> - Can show the cells from TOP to BOTTOM or from LEFT to RIGHT, this is the orientation property
 * <p> - It's possible to change the orientation event at runtime (but it's also recommended resizing both the
 * VirtualFlow and the cells)
 * <p> - It's not necessary to wrap the flow in a scroll pane as it already includes both the scroll bars
 * <p> - It's possible to set the speed of both the scroll bars
 * <p> - It's possible to scroll manually by pixels or to cell index
 * <p> - It's possible to get the currently shown/built cells or a specific cell by index
 * <p></p>
 * To build a SimpleVirtualFlow use the {@link SimpleVirtualFlow.Builder} class.
 * <p></p>
 * The cells are contained in a {@link Group} which is a {@link SimpleVirtualFlowContainer}.
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of Cell to use
 */
public class SimpleVirtualFlow<T, C extends Cell<T>> extends Region implements VirtualFlow<T, C> {
    //================================================================================
    // Properties
    //================================================================================
    private final String STYLE_CLASS = "virtual-flow";
    private final String STYLESHEET = ResourceManager.loadResource("SimpleVirtualFlow.css");
    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Function<T, C>> cellFactory = new SimpleObjectProperty<>();

    private final ScrollBar hBar = new ScrollBar();
    private final DoubleProperty horizontalPosition = new SimpleDoubleProperty();

    private final ScrollBar vBar = new ScrollBar();
    private final DoubleProperty verticalPosition = new SimpleDoubleProperty();

    private OrientationHelper orientationHelper;
    private final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(Orientation.VERTICAL);
    private final SimpleVirtualFlowContainer<T, C> container = new SimpleVirtualFlowContainer<>(this);

    private final Features features = new Features();

    //================================================================================
    // Constructors
    //================================================================================
    protected SimpleVirtualFlow() {
    }

    //================================================================================
    // Initialization
    //================================================================================

    /**
     * Adds the container to the children list, calls {@link #setupScrollBars()}.
     * <p>
     * Also adds listeners for the cellFactory property and the orientation property.
     * Also initializes the orientation helper and the container when {@link #needsLayoutProperty()}
     * is becomes false.
     * <p></p>
     * A little note: the orientation helper is initialized manually at this specific point
     * because otherwise the added listeners cause an infinite loop that will eat RAM for some reason.
     */
    protected void initialize() {
        getStyleClass().add(STYLE_CLASS);
        getChildren().add(container);
        setupScrollBars();

        cellFactory.addListener((observable, oldValue, newValue) -> {
            container.getLayoutManager().setInitialized(false);
            container.getCellsManager().clear();
            scrollToPixel(0.0);
            container.getLayoutManager().initialize();
        });

        orientation.addListener((observable, oldValue, newValue) -> {
            container.getLayoutManager().setInitialized(false);
            features.orientationChanged = true;
            container.getCellsManager().clear();
            scrollToPixel(0.0);
            orientationHelper.dispose();
            if (newValue == Orientation.VERTICAL) {
                VerticalHelper verticalHelper = new VerticalHelper(this, container);
                orientationHelper = verticalHelper;
                verticalHelper.initialize();
            } else {
                HorizontalHelper horizontalHelper = new HorizontalHelper(this, container);
                orientationHelper = horizontalHelper;
                horizontalHelper.initialize();
            }
            container.getLayoutManager().initialize();
        });

        ExecutionUtils.executeWhen(
                needsLayoutProperty(),
                (oldValue, newValue) -> {
                    if (orientationHelper instanceof HorizontalHelper) {
                        HorizontalHelper helper = (HorizontalHelper) orientationHelper;
                        helper.initialize();
                    } else {
                        VerticalHelper helper = (VerticalHelper) orientationHelper;
                        helper.initialize();
                    }
                    container.initialize();
                },
                false,
                (oldValue, newValue) -> !newValue,
                true
        );
    }

    /**
     * Sets up the scroll bars. By the default the unit increment of both bars is set to 15.
     * Redirects any ScrollEvent to the scroll bars by modifying the {@link EventDispatcher} and
     * then adds the scroll bars to the children list.
     */
    private void setupScrollBars() {
        // Managed
        hBar.setManaged(false);
        vBar.setManaged(false);

        // Orientation
        hBar.setOrientation(Orientation.HORIZONTAL);
        vBar.setOrientation(Orientation.VERTICAL);

        // Unit Increment
        hBar.setUnitIncrement(15);
        vBar.setUnitIncrement(15);

        // Max
        hBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> container.getEstimatedWidth() - getWidth(),
                container.estimatedWidthProperty(), widthProperty()
        ));
        vBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> container.getEstimatedHeight() - getHeight(),
                container.estimatedHeightProperty(), heightProperty()
        ));

        // Visibility
        hBar.visibleAmountProperty().bind(Bindings.createDoubleBinding(
                () -> getWidth() - hBar.getWidth(),
                widthProperty(), hBar.widthProperty()
        ));
        hBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getEstimatedWidth() > getWidth(),
                container.estimatedWidthProperty(), widthProperty()
        ));

        vBar.visibleAmountProperty().bind(Bindings.createDoubleBinding(
                () -> getHeight() - vBar.getHeight(),
                heightProperty(), vBar.heightProperty()
        ));
        vBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getEstimatedHeight() > getHeight(),
                container.estimatedHeightProperty(), heightProperty()
        ));

        // Positions
        horizontalPosition.bindBidirectional(hBar.valueProperty());
        verticalPosition.bindBidirectional(vBar.valueProperty());

        // Event Dispatching
        EventDispatcher original = getEventDispatcher();
        setEventDispatcher((event, tail) -> {
            if (event instanceof ScrollEvent) {
                if (getOrientation() == Orientation.VERTICAL) {
                    tail.prepend(vBar.getEventDispatcher());
                } else {
                    tail.prepend(hBar.getEventDispatcher());
                }
            }
            return original.dispatchEvent(event, tail);
        });

        getChildren().addAll(hBar, vBar);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * @return the cell at the specified index, null if not found
     */
    public C getCell(int index) {
        try {
            return getCells().get(index);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return all the currently shown cells
     */
    public Map<Integer, C> getCells() {
        return Collections.unmodifiableMap(container.getCells());
    }

    /**
     * Scrolls by the given amount of pixels.
     */
    public void scrollBy(double pixels) {
        orientationHelper.scrollBy(pixels);
    }

    /**
     * Scrolls to the given cell index.
     */
    public void scrollTo(int index) {
        orientationHelper.scrollTo(index);
    }

    /**
     * Scrolls to the first cell.
     */
    public void scrollToFirst() {
        orientationHelper.scrollToFirst();
    }

    /**
     * Scrolls to the last cell.
     */
    public void scrollToLast() {
        orientationHelper.scrollToLast();
    }

    /**
     * Scrolls to the given pixel value.
     */
    public void scrollToPixel(double pixel) {
        orientationHelper.scrollToPixel(pixel);
    }

    /**
     * Sets the horizontal scroll bar speeds.
     */
    public void setHSpeed(double unit, double block) {
        hBar.setUnitIncrement(unit);
        hBar.setBlockIncrement(block);
    }

    /**
     * Sets the vertical scroll bar speeds.
     */
    public void setVSpeed(double unit, double block) {
        vBar.setUnitIncrement(unit);
        vBar.setBlockIncrement(block);
    }

    //================================================================================
    // Override Methods
    //================================================================================

    @Override
    public String getUserAgentStylesheet() {
        return STYLESHEET;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double prefVerticalWidth = vBar.prefWidth(-1);
        double prefHorizontalHeight = hBar.prefHeight(-1);
        vBar.resizeRelocate(getWidth() - prefVerticalWidth, 0, prefVerticalWidth, getHeight());
        hBar.resizeRelocate(0, getHeight() - prefHorizontalHeight, getWidth(), prefHorizontalHeight);
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * {@inheritDoc}
     */
    public ObservableList<T> getItems() {
        return items.get();
    }

    /**
     * Property for the items list.
     */
    public ObjectProperty<ObservableList<T>> itemsProperty() {
        return items;
    }

    /**
     * {@inheritDoc}
     */
    public void setItems(ObservableList<T> items) {
        this.items.set(items);
    }

    /**
     * {@inheritDoc}
     */
    public Function<T, C> getCellFactory() {
        return cellFactory.get();
    }

    /**
     * {@inheritDoc}
     */
    public ObjectProperty<Function<T, C>> cellFactoryProperty() {
        return cellFactory;
    }

    /**
     * {@inheritDoc}
     */
    public void setCellFactory(Function<T, C> cellFactory) {
        this.cellFactory.set(cellFactory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScrollBar getHBar() {
        return hBar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScrollBar getVBar() {
        return vBar;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getVerticalPosition() {
        return verticalPosition.get();
    }

    /**
     * {@inheritDoc}
     */
    public DoubleProperty verticalPositionProperty() {
        return verticalPosition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVerticalPosition(double vValue) {
        this.verticalPosition.set(vValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getHorizontalPosition() {
        return horizontalPosition.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    public DoubleProperty horizontalPositionProperty() {
        return horizontalPosition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHorizontalPosition(double hValue) {
        this.horizontalPosition.set(hValue);
    }

    /**
     * @return the orientation of the VirtualFlow
     */
    public Orientation getOrientation() {
        return orientation.get();
    }

    /**
     * The orientation property of the VirtualFlow.
     */
    public ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /**
     * Sets the orientation of the VirtualFlow.
     */
    public void setOrientation(Orientation orientation) {
        this.orientation.set(orientation);
    }

    /**
     * @return the current {@link OrientationHelper} instance
     */
    protected OrientationHelper getOrientationHelper() {
        return orientationHelper;
    }

    /**
     * @return an instance of {@link Features}
     */
    public Features features() {
        return features;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimpleVirtualFlow<T, C> getVirtualFlow() {
        return this;
    }

    //================================================================================
    // Builder
    //================================================================================

    /**
     * Builder class to create {@link SimpleVirtualFlow}s.
     */
    public static class Builder {

        private Builder() {
        }

        /**
         * @param items       The items list property
         * @param cellFactory The function to convert items to cells
         * @param orientation The orientation
         * @param <T>         The type of objects
         * @param <C>         The type of cells
         */
        public static <T, C extends Cell<T>> SimpleVirtualFlow<T, C> create(ObjectProperty<? extends ObservableList<T>> items, Function<T, C> cellFactory, Orientation orientation) {
            SimpleVirtualFlow<T, C> virtualFlow = new SimpleVirtualFlow<>();
            virtualFlow.items.bind(items);
            virtualFlow.setCellFactory(cellFactory);
            virtualFlow.setOrientation(orientation);
            virtualFlow.orientationHelper = (orientation == Orientation.HORIZONTAL) ?
                    new HorizontalHelper(virtualFlow, virtualFlow.container) :
                    new VerticalHelper(virtualFlow, virtualFlow.container);
            virtualFlow.initialize();
            return virtualFlow;
        }

        /**
         * @param items       The items list
         * @param cellFactory The function to convert items to cells
         * @param orientation The orientation
         * @param <T>         The type of objects
         * @param <C>         The type of cells
         */
        public static <T, C extends Cell<T>> SimpleVirtualFlow<T, C> create(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
            SimpleVirtualFlow<T, C> virtualFlow = new SimpleVirtualFlow<>();
            virtualFlow.setItems(items);
            virtualFlow.setCellFactory(cellFactory);
            virtualFlow.setOrientation(orientation);
            virtualFlow.orientationHelper = (orientation == Orientation.HORIZONTAL) ?
                    new HorizontalHelper(virtualFlow, virtualFlow.container) :
                    new VerticalHelper(virtualFlow, virtualFlow.container);
            virtualFlow.initialize();
            return virtualFlow;
        }
    }

    //================================================================================
    // Extra Features
    //================================================================================

    /**
     * Helper class to manage extra features of the VirtualFlow.
     */
    public class Features {
        //================================================================================
        // Properties
        //================================================================================
        private Timeline overScrollAnimation;
        private double overScroll = 0;
        private boolean overScrollEnabled = false;
        private boolean orientationChanged = false;

        //================================================================================
        // Constructors
        //================================================================================
        private Features() {
        }

        //================================================================================
        // Methods
        //================================================================================

        /**
         * Calls {@link #enableBounceEffect(double, double)} with 5 and 40 values for
         * strength and maxOverscroll respectively.
         */
        public void enableBounceEffect() {
            enableBounceEffect(5, 40);
        }

        /**
         * Adds a bounce effect when the start or end of the flow is reached.
         */
        public void enableBounceEffect(double strength, double maxOverscroll) {
            overScrollEnabled = true;
            overScrollAnimation = new Timeline();
            overScrollAnimation.setOnFinished(event -> overScroll = 0);

            vBar.addEventHandler(ScrollEvent.ANY, event -> {
                int mul;
                if (vBar.getValue() == 0) {
                    mul = -1;
                } else if (vBar.getValue() == vBar.getMax()) {
                    mul = 1;
                } else {
                    overScrollAnimation.stop();
                    overScroll = 0;
                    return;
                }

                overScroll = NumberUtils.clamp(overScroll - strength, -maxOverscroll, maxOverscroll);
                KeyFrame kf0 = new KeyFrame(Duration.millis(100), new KeyValue(container.layoutYProperty(), -getVerticalPosition() + (overScroll * mul), AnimationUtils.INTERPOLATOR_V2));
                KeyFrame kf1 = new KeyFrame(Duration.millis(350), new KeyValue(container.layoutYProperty(), -getVerticalPosition(), AnimationUtils.INTERPOLATOR_V2));
                overScrollAnimation.getKeyFrames().setAll(kf0, kf1);
                overScrollAnimation.playFromStart();
            });

            hBar.addEventHandler(ScrollEvent.ANY, event -> {
                int mul;
                if (hBar.getValue() == 0) {
                    mul = -1;
                } else if (hBar.getValue() == hBar.getMax()) {
                    mul = 1;
                } else {
                    overScroll = 0;
                    overScrollAnimation.stop();
                    return;
                }

                overScroll = NumberUtils.clamp(overScroll - strength, -maxOverscroll, maxOverscroll);
                KeyFrame kf0 = new KeyFrame(Duration.millis(100), new KeyValue(container.layoutXProperty(), -getHorizontalPosition() + (overScroll * mul), AnimationUtils.INTERPOLATOR_V2));
                KeyFrame kf1 = new KeyFrame(Duration.millis(350), new KeyValue(container.layoutXProperty(), -getHorizontalPosition(), AnimationUtils.INTERPOLATOR_V2));
                overScrollAnimation.getKeyFrames().setAll(kf0, kf1);
                overScrollAnimation.playFromStart();
            });
        }

        /**
         * Calls {@link #enableSmoothScrolling(double, double, double)} with 7 and 0.05 values
         * for trackPadAdjustment and scrollThreshold as second and third parameters.
         */
        public void enableSmoothScrolling(double speed) {
            enableSmoothScrolling(speed, 7, 0.05);
        }

        /**
         * Calls {@link #enableSmoothScrolling(double, double, double)} with 0.05 value
         * for scrollThreshold as third parameter.
         */
        public void enableSmoothScrolling(double speed, double trackpadAdjustment) {
            enableSmoothScrolling(speed, trackpadAdjustment, 0.05);
        }

        /**
         * Enables smooth scrolling for this VirtualFlow.
         *
         * @param speed              parameter to adjust the speed
         * @param trackPadAdjustment parameter to adjust the scrolling with the trackpad (formula is '(speed / (trackPadAdjustment * 100))')
         * @param scrollThreshold    computed values lesser than this threshold will stop the scroll, recommended values are lesser than 1,
         *                           it's ignored if using a trackpad
         */
        public void enableSmoothScrolling(double speed, double trackPadAdjustment, double scrollThreshold) {
            final double[] frictions = {0.99, 0.1, 0.05, 0.04, 0.03, 0.02, 0.01, 0.04, 0.01, 0.008, 0.008, 0.008, 0.008, 0.0006, 0.0005, 0.00003, 0.00001};
            final double[] derivatives = new double[frictions.length];
            AtomicReference<Double> atomicSpeed = new AtomicReference<>(speed);

            Timeline timeline = new Timeline();
            AtomicReference<ScrollDirection> scrollDirection = new AtomicReference<>();
            AtomicBoolean isTrackpad = new AtomicBoolean(false);
            final ChangeListener<? super Orientation> orientationChangeListener = (observable, oldValue, newValue) -> {
                timeline.stop();
                setVerticalPosition(0);
                setHorizontalPosition(0);
            };
            final EventHandler<MouseEvent> mouseHandler = event -> timeline.stop();
            final EventHandler<ScrollEvent> scrollHandler = event -> {
                if (overScrollEnabled) {
                    overScrollAnimation.stop();
                }

                if (orientationChanged) {
                    timeline.stop();
                    orientationChanged = false;
                    return;
                }

                if (event.getEventType() == ScrollEvent.SCROLL) {
                    scrollDirection.set(determineScrollDirection(getOrientation(), event.getDeltaY()));
                    isTrackpad.set(isTrackPad(event.getDeltaY()));
                    if (isTrackpad.get()) {
                        atomicSpeed.set(speed / (trackPadAdjustment * 100));
                    } else {
                        atomicSpeed.set(speed);
                    }
                    derivatives[0] += scrollDirection.get().intDirection() * atomicSpeed.get();
                    if (timeline.getStatus() == Animation.Status.STOPPED) {
                        timeline.play();
                    }

                    if (!overScrollEnabled) {
                        event.consume();
                    }
                }
            };

            if (getParent() != null) {
                getParent().addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
            }
            parentProperty().addListener((observable, oldValue, newValue) -> {
                if (oldValue != null) {
                    oldValue.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
                }
                if (newValue != null) {
                    newValue.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
                }
            });

            orientation.addListener(orientationChangeListener);
            addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
            addEventFilter(ScrollEvent.ANY, scrollHandler);

            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(3), (event) -> {
                for (int i = 0; i < derivatives.length; i++) {
                    derivatives[i] *= frictions[i];
                }
                for (int i = 1; i < derivatives.length; i++) {
                    derivatives[i] += derivatives[i - 1];
                }

                double dy = NumberUtils.formatTo(derivatives[derivatives.length - 1], 2);

                DoubleProperty positionProperty = getOrientation() == Orientation.VERTICAL ? verticalPosition : horizontalPosition;
                double max = getOrientation() == Orientation.VERTICAL ? vBar.getMax() : hBar.getMax();
                positionProperty.set(NumberUtils.clamp(positionProperty.get() + dy, 0, max));

                if (!isTrackpad.get() && Math.abs(dy) < scrollThreshold) {
                    timeline.stop();
                }
            }));
            timeline.setCycleCount(Animation.INDEFINITE);
        }
    }
}
