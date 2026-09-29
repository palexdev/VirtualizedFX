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

package io.github.palexdev.virtualizedfx.table.defaults;

import java.util.ArrayList;
import java.util.List;

import io.github.palexdev.mfxcore.controls.BoundLabel;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.TextMeasurementCache;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.observables.When.observe;

/// Default skin implementation for [VFXSimpleTableColumn], extends [MFXSkinBase].
///
/// The layout is simple, there are at max three nodes:
///
/// - a [BoundLabel] to show the column's text
/// - a generic [Node] specified by the column's [VFXTableColumn#graphicProperty()]
/// - a [Region] called 'overlay' which can be used to indicate selection, hovering or other states for the column.
///   This region can be selected in CSS by the selector '.overlay'. See [VFXSimpleTableColumn#enableOverlayProperty()]
///   and [VFXSimpleTableColumn#overlayOnHeaderProperty()]
///
/// There are three ways to arrange the text and the graphic, as specified by the
/// [VFXSimpleTableColumn#graphicAlignmentProperty()]. For [HPos#LEFT] and [HPos#RIGHT], the graphic is going to be
/// placed to the left and right respectively of the label. For [HPos#CENTER] only the graphic will be visible at the
/// center of the area, the label will be hidden.
///
/// Mouse events are not consumed by the skin, so that they reach the column's behavior and the table, where the
/// resize handlers of the default behavior are, see [VFXTableColumnBehavior].
public class VFXSimpleTableColumnSkin<T, C extends VFXTableCell<T>> extends MFXSkinBase<VFXSimpleTableColumn<T, C>> {

    //================================================================================
    // Properties
    //================================================================================

    private final BoundLabel label;
    private final TextMeasurementCache tmc;
    private final Region overlay;

    //================================================================================
    // Constructors
    //================================================================================

    public VFXSimpleTableColumnSkin(VFXSimpleTableColumn<T, C> column) {
        super(column);

        // Init label
        label = new BoundLabel(column);
        label.graphicProperty().unbind();
        label.setGraphic(null);
        label.graphicTextGapProperty().unbind();
        label.setGraphicTextGap(0.0);
        label.setManaged(false);
        tmc = new TextMeasurementCache(column);

        // Init overlay
        overlay = new Region();
        overlay.getStyleClass().add("overlay");
        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        overlay.setFocusTraversable(false);

        // Finalize
        updateChildren();
        consumeMouseEvents(false); // JavaFX bullshit
    }

    //================================================================================
    // Methods
    //================================================================================

    protected void updateChildren() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        List<Node> children = new ArrayList<>();
        if (column.isEnableOverlay()) children.add(overlay);
        children.add(label);
        if (column.getGraphic() != null) children.add(column.getGraphic());
        getChildren().setAll(children);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Registers the skin's listeners:
    ///
    /// - on [VFXTableColumn#graphicProperty()] and [VFXSimpleTableColumn#enableOverlayProperty()], calls
    ///   [#updateChildren()]
    ///
    /// - on [VFXSimpleTableColumn#graphicAlignmentProperty()] and [VFXSimpleTableColumn#overlayOnHeaderProperty()],
    ///   requests a layout of the column
    @Override
    public void install() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        listen(
            observe(this::updateChildren, column.graphicProperty(), column.enableOverlayProperty()),
            observe(column::requestLayout, column.graphicAlignmentProperty(), column.overlayOnHeaderProperty())
        );
    }

    /// @return the insets plus the graphic's width and the gap, if there is a graphic. The text is not included, it can
    /// be truncated
    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        return leftInset +
               ((graphic != null) ? LayoutUtils.boundWidth(graphic) + column.getGraphicTextGap() : 0.0) +
               rightInset;
    }

    /// @return the insets, the graphic's width and the gap (if there is a graphic), plus the text's width, measured by a
    /// [TextMeasurementCache]. This is also what [VFXTable]'s default skin measures for the header when autosizing the
    /// column, see [VFXTableColumn#sizeToContent()]
    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        return leftInset +
               ((graphic != null) ? LayoutUtils.boundWidth(graphic) + column.getGraphicTextGap() : 0.0) +
               tmc.getSnappedWidth() +
               rightInset;
    }

    /// {@inheritDoc}
    ///
    /// The graphic is aligned in the whole area as specified by the [VFXSimpleTableColumn#graphicAlignmentProperty()].
    /// The label takes the remaining width, after the graphic with [HPos#LEFT], before it with [HPos#RIGHT], and is
    /// hidden with [HPos#CENTER].
    ///
    /// The overlay is laid out only if the column is in a table. It's as wide as the column, and goes from the column's
    /// top (or bottom, if [VFXSimpleTableColumn#overlayOnHeaderProperty()] is `false`) to the table's bottom.
    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        HPos graphicAlignment = column.getGraphicAlignment();
        double gap = graphic != null ? column.getGraphicTextGap() : 0.0;

        double gw = 0;
        if (graphic != null) {
            switch (graphicAlignment) {
                case LEFT -> layoutInArea(graphic, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
                case CENTER -> layoutInArea(graphic, x, y, w, h, 0, HPos.CENTER, VPos.CENTER);
                case RIGHT -> layoutInArea(graphic, x, y, w, h, 0, HPos.RIGHT, VPos.CENTER);
            }
            gw = graphic.getLayoutBounds().getWidth();
        }

        double remainingW = Math.max(0, w - gw - gap);
        switch (graphicAlignment) {
            case LEFT -> {
                layoutInArea(label, x + gap + gw, y, remainingW, h, 0, HPos.LEFT, VPos.CENTER);
                label.setVisible(true);
            }
            case CENTER -> label.setVisible(false);
            case RIGHT -> {
                layoutInArea(label, x, y, remainingW, h, 0, HPos.LEFT, VPos.CENTER);
                label.setVisible(true);
            }
        }

        // Overlay layout
        VFXTable<T> table = column.getTable();
        if (table != null) {
            double minY = column.isOverlayOnHeader() ? 0 : column.getHeight();
            double oW = column.getWidth();
            double oH = table.getLayoutBounds().getHeight() - table.snappedBottomInset() - table.snappedTopInset() - minY;
            overlay.resizeRelocate(0.0, minY, oW, oH);
        }
    }
}
