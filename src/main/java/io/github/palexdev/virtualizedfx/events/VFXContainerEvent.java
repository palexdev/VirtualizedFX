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

package io.github.palexdev.virtualizedfx.events;

import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/**
 * Custom event implementation to be used with {@link VFXContainer}s. Event-based systems are very useful for loose coupling,
 * because it allows components to communicate with each other without needing to know the specifics of their implementations.
 * <p>
 * This mechanism should be used only when there are no other reasonable ways as the performance implications of this system
 * are not very clear.
 */
public class VFXContainerEvent extends Event {
    //================================================================================
    // Event Types
    //================================================================================

    /**
     * This event type can be used to tell cells to forcefully update. Especially useful when the model's data does not
     * use JavaFX's properties.
     *
     * @see VFXContainer#update(int...)
     */
    public static final EventType<VFXContainerEvent> UPDATE = new EventType<>("UPDATE");

    //================================================================================
    // Constructors
    //================================================================================
    public VFXContainerEvent(Object source, EventTarget target, EventType<? extends Event> eventType) {
        super(source, target, eventType);
    }

    //================================================================================
    // Static Methods
    //================================================================================
    public static <T> void update(VFXCell<T> cell) {
        if (cell == null) return; // Avoid null targets
        Node node = cell.toNode();
        fireEvent(node, new VFXContainerEvent(null, node, UPDATE));
    }
}
