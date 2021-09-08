package io.github.palexdev.virtualizedfx.flow.simple;

import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.utils.ExecutionUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.shape.Rectangle;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * This is the {@link Group} used by {@link SimpleVirtualFlow} that contains the cells.
 * <p>
 * Keeps a reference to the VirtualFlow to communicate with it. It's needed to listen to changes to the
 * items list, get its sizes, also acts as a bridge for the {@link LayoutManager} and the {@link CellsManager}.
 * <p></p>
 * To keep things easy and organized this container makes use of two helper classes, which are
 * {@link LayoutManager} and {@link CellsManager} to keep track of layout changes (scroll for example) and
 * update the visible cells accordingly.
 *
 * @param <T> the type of object to represent
 * @param <C> the type of Cell to use
 */
public class SimpleVirtualFlowContainer<T, C extends ISimpleCell> extends Group {
    //================================================================================
    // Properties
    //================================================================================
    private final SimpleVirtualFlow<T, C> virtualFlow;
    final CellsManager<T, C> cellsManger;
    final LayoutManager<T, C> layoutManager;

    private final ChangeListener<? super ObservableList<T>> listChanged;
    private final ListChangeListener<? super T> itemsChanged;

    //================================================================================
    // Constructors
    //================================================================================
    public SimpleVirtualFlowContainer(SimpleVirtualFlow<T, C> virtualFlow) {
        this.virtualFlow = virtualFlow;
        this.layoutManager = new LayoutManager<>(virtualFlow);
        this.cellsManger = new CellsManager<>(virtualFlow, layoutManager);
        itemsChanged = cellsManger::itemsChanged;
        listChanged = (observable, oldValue, newValue) -> {
            layoutManager.reinitialize();
            cellsManger.clear();
            virtualFlow.scrollTo(0);
            oldValue.removeListener(itemsChanged);
            newValue.addListener(itemsChanged);
            layoutManager.computeIndexes();
        };
        initialize();
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Calls {@link #buildClip()} and {@link ExecutionUtils#executeWhen(ObservableValue, BiConsumer, boolean, BiFunction, boolean)}
     * when the VirtualFlow is initialized (listens to the {@link Parent#needsLayoutProperty()}) to initialize the layout manager,
     * the cells manager and add the needed listeners.
     */
    private void initialize() {
        setStyle("-fx-border-color: blue");
        buildClip();
        ExecutionUtils.executeWhen(
                virtualFlow.needsLayoutProperty(),
                (oldValue, newValue) -> {
                    layoutManager.initialize();
                    cellsManger.updateContent();
                    virtualFlow.getItems().addListener(itemsChanged);
                    virtualFlow.itemsProperty().addListener(listChanged);
                },
                false,
                (oldValue, newValue) -> !newValue,
                true
        );
    }

    /**
     * Builds and sets the container's clip to hide the cells outside the viewport.
     * The clip not only has its width and height bound to the VirtualFlow's ones but also
     * the layoutX and layoutY properties bound to the container's ones (values are inverted, multiplied by -1).
     */
    private void buildClip() {
        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(virtualFlow.widthProperty());
        rectangle.heightProperty().bind(virtualFlow.heightProperty());
        rectangle.layoutXProperty().bind(layoutXProperty().multiply(-1));
        rectangle.layoutYProperty().bind(layoutYProperty().multiply(-1));
        setClip(rectangle);
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * Delegate method to {@link LayoutManager#getTotalHeight()}.
     */
    public double getTotalHeight() {
        return layoutManager.getTotalHeight();
    }

    /**
     * Delegate Method to {@link LayoutManager#totalHeightProperty()}.
     */
    public DoubleProperty totalHeightProperty() {
        return layoutManager.totalHeightProperty();
    }

    /**
     * Delegate method to {@link LayoutManager#getTotalWidth()}.
     */
    public double getTotalWidth() {
        return layoutManager.getTotalWidth();
    }

    /**
     * Delegate method to {@link LayoutManager#totalWidthProperty()}.
     */
    public DoubleProperty totalWidthProperty() {
        return layoutManager.totalWidthProperty();
    }

    //================================================================================
    // Override Methods
    //================================================================================

    /**
     * Overridden to be empty, the layout is done manually by the {@link CellsManager}
     * when needed.
     */
    @Override
    protected void layoutChildren() {}
}
