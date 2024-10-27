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

package io.github.palexdev.virtualizedfx.base;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

/**
 * Defines the common API for every virtualized container offered by VirtualizedFX.
 */
public interface VFXContainer<T> {

    /**
     * This method should be used by implementations to "manually" update the container.
     * <p>
     * This can be useful when working with models that do not use JavaFX properties.
     * <p>
     * Note: the {@code indexes} var-arg parameter can be used to specify which cells need to be updated. An empty
     * array should update all of them.
     * <p>
     * More details: some cells may use an update mechanism which relies on property invalidation. Follow this example
     * to better understand what I mean:
     * <pre>
     * {@code
     * // Let's say I have a User class with 'firstName' and 'lastName' fields (we also have both getters and setters)
     * // Now, let's assume I have a UserCell class used by the VFXContainer to display User objects (in a label for example)
     * // This is a part of its implementation...
     * public class UserCell extends Label implements VFXCell<User> {
     *     private final ObjectProperty<User> item = new SimpleObjectProperty<>() {
     *         @Overridden
     *         protected void invalidated() {
     *             update();
     *         }
     *     };
     *
     *     protected void update() {
     *         // This will update the cell's text based on the current item
     *     }
     * }
     *
     * // Remember, the 'invalidated()' method is called only when the reference changes, because internally it does not
     * // check for equality but for identity
     *
     * // Now let's say I want to change a User's 'lastName' field like this...
     * container.getItems().get(i).setLastName("NewLastName");
     *
     * // Question: how can we tell the cell to force the update?
     * // There are two possible ways...
     * // 1) For the invalidation to occur, we first set the item property to 'null', and then back to the old value
     * // 2) We use an event-based mechanism to tell cells to force update themselves. This solution requires cells to
     * // subscribe to such events to support "manual" updates
     *
     * // Solution 2 is more flexible, see VFXContainerEvent class
     * }
     * </pre>
     *
     * @see VFXContainerEvent
     */
    void update(int... indexes);

    default ObservableList<T> getItems() {
        return itemsProperty().get();
    }

    /**
     * Specifies the {@link ObservableList} used to store the items.
     * <p>
     * We use a {@link ListProperty} because it offers many commodities such as both the size and emptiness of the list
     * as observable properties, as well as the possibility of adding an {@link InvalidationListener} that will both inform
     * about changes of the property and in the list.
     */
    ListProperty<T> itemsProperty();

    default void setItems(ObservableList<T> items) {
        itemsProperty().set(items);
    }

    default int size() {
        return sizeProperty().get();
    }

    /**
     * Specifies the number of items in the data structure.
     */
    default ReadOnlyIntegerProperty sizeProperty() {
        return itemsProperty().sizeProperty();
    }

    default boolean isEmpty() {
        return emptyProperty().get();
    }

    /**
     * Specifies whether the data set is empty.
     */
    default ReadOnlyBooleanProperty emptyProperty() {
        return itemsProperty().emptyProperty();
    }

    default double getVirtualMaxX() {
        return virtualMaxXProperty().get();
    }

    ReadOnlyDoubleProperty virtualMaxXProperty();

    default double getVirtualMaxY() {
        return virtualMaxYProperty().get();
    }

    ReadOnlyDoubleProperty virtualMaxYProperty();

    default double getMaxVScroll() {
        return maxVScrollProperty().get();
    }

    /**
     * Specifies the maximum possible value for {@link #vPosProperty()}.
     */
    ReadOnlyDoubleProperty maxVScrollProperty();

    default double getMaxHScroll() {
        return maxHScrollProperty().get();
    }

    /**
     * Specifies the maximum possible value for {@link #hPosProperty()}
     */
    ReadOnlyDoubleProperty maxHScrollProperty();

    default double getVPos() {
        return vPosProperty().get();
    }

    /**
     * Specifies the container's vertical position.
     */
    DoubleProperty vPosProperty();

    default void setVPos(double vPos) {
        vPosProperty().set(vPos);
    }

    default double getHPos() {
        return hPosProperty().get();
    }

    /**
     * Specifies the container's horizontal position.
     */
    DoubleProperty hPosProperty();

    default void setHPos(double hPos) {
        hPosProperty().set(hPos);
    }

    default BufferSize getBufferSize() {
        return bufferSizeProperty().get();
    }

    /**
     * Specifies the number of extra cells to add to the container; they act as a buffer, allowing scroll to be smoother.
     * To avoid edge cases due to the users abusing the system, this is done by using an enumerator which allows up to
     * three buffer cells.
     */
    StyleableObjectProperty<BufferSize> bufferSizeProperty();

    default void setBufferSize(BufferSize bufferSize) {
        bufferSizeProperty().set(bufferSize);
    }
}
