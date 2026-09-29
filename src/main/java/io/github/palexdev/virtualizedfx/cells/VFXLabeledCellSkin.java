/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.input.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.VPos;

import static io.github.palexdev.mfxcore.input.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/// Simple skin implementation to be used with any descendant of [VFXCellBase].
///
/// This will display the data specified by the [VFXCellBase#itemProperty()] as a `String` in a [Label].
/// It's the only child of this skin, takes all the available space and has the following properties bound to the cell:
///
/// - the alignment property bound to [VFXCellBase#alignmentProperty()]
/// - the graphic property bound to [VFXCellBase#graphicProperty()].
///
/// The label's text will be updated on two occasions:
///
/// 1) when the [VFXCellBase#itemProperty()] is invalidated
///
/// 2) when an event of type [VFXContainerEvent#UPDATE] reaches the cell
///
/// You can override [#install()] to change such behavior, but read its docs first. For example, rather than using an [InvalidationListener]
/// you could use a [ChangeListener] instead, add your own logic, etc. (useful when you want to optimize update performance).
///
/// (It's recommended to use [#listen(When\[\])], [#onInput(WhenEvent\[\])] and in general [When] constructs.
/// Simply because they make your life easier, also disposal would be automatic this way).
///
/// Last but not least, the label's text is updated by the [#update()] method.
public class VFXLabeledCellSkin<T> extends MFXSkinBase<VFXCellBase<T>> {

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
        getChildren().setAll(label);
    }

    //================================================================================
    // Methods
    //================================================================================

    /// This is responsible for updating the label's text using the value specified by the [VFXCellBase#itemProperty()].
    ///
    /// If the item is `null` sets the text to an empty string, otherwise calls `toString()` on it.
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

    /// Registers the two things that update the label's text, see the class docs: a listener on the
    /// [VFXCellBase#itemProperty()], which also runs immediately, and a handler for the [VFXContainerEvent#UPDATE]
    /// events that reach the cell. Also makes the skin not consume mouse events, so that they can reach the cell's container.
    @Override
    public void install() {
        VFXCellBase<T> cell = getSkinnable();

        // Listeners
        listen(
            onInvalidated(cell.itemProperty())
                .then(t -> update())
                .executeNow()
        );

        // Input
        onInput(
            intercept(cell, VFXContainerEvent.UPDATE)
                .handle(e -> {
                    update();
                    e.consume();
                })
        );
        consumeMouseEvents(false); // JavaFX bullshit
    }

    /// @return the label's pref width plus the left and right insets
    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + label.prefWidth(-1) + rightInset;
    }

    /// {@inheritDoc}
    ///
    /// The label takes all the available space.
    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        label.resize(w, h);
        positionInArea(label, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
    }
}
