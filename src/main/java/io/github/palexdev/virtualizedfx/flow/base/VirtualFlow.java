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
