package io.github.palexdev.virtualizedfx.base;

import java.util.function.Function;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;
import io.github.palexdev.virtualizedfx.table.VFXTable;

/// Not all containers are directly responsible for creating their cells. For example, in the [VFXTable], each
/// column builds cells for a specific data type. This design prevents the [VFXContainer] API from exposing a cell
/// factory directly.
///
/// This interface makes those classes that are directly responsible for building cells expose the cell factory function.
///
/// **Note:** implementations must expose the wrapper class [CellFactory] rather than the function directly.
public interface WithCellFactory<T, C extends VFXCell<T>> {

    /// Convenience method, shortcut for `getCellFactory().create(...)`
    default C create(T item) {
        return getCellFactory().create(item);
    }

    /// Specifies the wrapper class [CellFactory] for the cell factory function
    CellFactory<T, C> getCellFactory();

    /// Sets the cell factory function used to create cells.
    default void setCellFactory(Function<T, C> cellFactory) {
        getCellFactory().setValue(cellFactory);
    }
}
