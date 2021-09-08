package io.github.palexdev.virtualizedfx.flow.base;

import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;

import java.util.function.Function;

/**
 * Public API for every Virtual Flow
 *
 * @param <T> the type of objects to represent
 * @param <C> the type of Cell to use
 */
public interface VirtualFlow<T, C extends Cell> {

    /**
     * @return the items list
     */
    ObservableList<T> getItems();

    /**
     * Replaces the items list with the given one.
     */
    void setItems(ObservableList<T> items);

    /**
     * @return the function used to build a Cell from an object of type T
     */
    Function<T, C> getCellFactory();

    /**
     * Property for the cell factory function.
     */
    ObjectProperty<Function<T, C>> cellFactoryProperty();

    /**
     * Sets the function used to build a Cell from an object of type T.
     */
    void setCellFactory(Function<T, C> cellFactory);
}
