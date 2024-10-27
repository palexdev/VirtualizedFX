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

package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;

import java.beans.EventHandler;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Simple skin implementation to be used with any descendant of {@link VFXCellBase}.
 * <p>
 * This will display the data specified by the {@link VFXCellBase#itemProperty()} as a {@code String} in a {@link Label}.
 * It's the only children of this skin, takes all the available space and has the following properties bound to the cell:
 * <p> - the alignment property bound to {@link VFXCellBase#alignmentProperty()}
 * <p> - the graphic property bound to {@link VFXCellBase#graphicProperty()}.
 * <p>
 * The label's text will be updated on two occasions:
 * <p> 1) when the {@link VFXCellBase#itemProperty()} is invalidated
 * <p> 2) when an event of type {@link VFXContainerEvent#UPDATE} reaches the cell
 * <p>
 * You can modify {@link #addListeners()} to change such behavior. For example, rather than using an {@link InvalidationListener}
 * you could use a {@link ChangeListener} instead, add your own logic, etc. (useful when you want to optimize update performance).
 * <p>
 * (It's recommended to use {@link #listeners(When[])}, {@link #events(WhenEvent[])} and in general {@link When} constructs.
 * Simply because they make your life easier, also disposal would be automatic this way).
 * <p>
 * Last but not least, the label's text is updated by the {@link #update()} method.
 */
public class VFXLabeledCellSkin<T> extends SkinBase<VFXCellBase<T>, CellBaseBehavior<T>> {
    //================================================================================
    // Properties
    //================================================================================
    protected final Label label;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXLabeledCellSkin(VFXCellBase<T> cell) {
        super(cell);

        // Init label
        label = new Label();
        label.alignmentProperty().bind(cell.alignmentProperty());
        label.graphicProperty().bind(cell.graphicProperty());

        // Finalize init
        addListeners();
        getChildren().add(label);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Adds an {@link InvalidationListener} on the {@link VFXCellBase#itemProperty()} to call {@link #update()} when it changes,
     * and an {@link EventHandler} to support "manual" updates through events of type {@link VFXContainerEvent#UPDATE}.
     * <p>
     * (Uses {@link When} and {@link WhenEvent} constructs).
     *
     * @see #listeners(When[])
     * @see #events(WhenEvent[])
     */
    protected void addListeners() {
        VFXCellBase<T> cell = getSkinnable();
        listeners(
            onInvalidated(cell.itemProperty())
                .then(t -> update())
                .executeNow()
        );
    }

    /**
     * This is responsible for updating the label's text using the value specified by the {@link VFXCellBase#itemProperty()}.
     * <p>
     * If the item is {@code null} sets the text to an empty string, otherwise calls {@code toString()} on it.
     */
    protected void update() {
        VFXCellBase<T> cell = getSkinnable();
        T item = cell.getItem();
        if (item == null) {
            label.setText("");
            return;
        }
        label.setText(item.toString());
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected void initBehavior(CellBaseBehavior<T> behavior) {
        VFXCellBase<T> cell = getSkinnable();
        behavior.init();
        events(
            intercept(cell, VFXContainerEvent.UPDATE)
                .process(e -> {
                    update();
                    e.consume();
                })
        );
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + label.prefWidth(-1) + rightInset;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        label.resize(w, h);
        positionInArea(label, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
    }
}
