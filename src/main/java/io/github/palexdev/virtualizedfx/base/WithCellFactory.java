package io.github.palexdev.virtualizedfx.base;

import java.util.function.Function;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.table.VFXTable;

/**
 * Not all containers are directly responsible for creating their cells. For example, in the {@link VFXTable}, each
 * column builds cells for a specific data type. This design prevents the {@link VFXContainer} API from exposing a cell
 * factory directly.
 * <p>
 * This interface makes those classes that are directly responsible for building cells expose the cell factory function.
 * <p></p>
 * <b>Note:</b> implementations must expose the wrapper class {@link CellFactory} rather than the function directly.
 */
public interface WithCellFactory<T, C extends VFXCell<T>> {

    /**
     * Convenience method, shortcut for {@code getCellFactory().create(...)}
     */
    default C create(T item) {
        return getCellFactory().create(item);
    }

    /**
     * Specifies the wrapper class {@link CellFactory} for the cell factory function
     */
    CellFactory<T, C> getCellFactory();

    /**
     * Sets the cell factory function used to create cells.
     */
    default void setCellFactory(Function<T, C> cellFactory) {
        getCellFactory().setValue(cellFactory);
    }
}
