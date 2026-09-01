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
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.observables.When.observe;

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
        addListeners();
        consumeMouseEvents(false); // JavaFX bullshit
    }

    //================================================================================
    // Methods
    //================================================================================

    private void addListeners() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        listeners(
            observe(this::updateChildren, column.graphicProperty(), column.enableOverlayProperty()),
            observe(column::requestLayout, column.graphicAlignmentProperty(), column.overlayOnHeaderProperty())
        );
    }

    protected void updateChildren() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        List<Node> children = new ArrayList<>();
        if (column.isEnableOverlay()) children.add(overlay);
        if (column.getGraphic() != null) children.add(column.getGraphic());
        children.add(label);
        getChildren().setAll(children);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        return leftInset +
               ((graphic != null) ? LayoutUtils.boundWidth(graphic) + column.getGraphicTextGap() : 0.0) +
               rightInset;
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        return leftInset +
               ((graphic != null) ? LayoutUtils.boundWidth(graphic) + column.getGraphicTextGap() : 0.0) +
               tmc.getSnappedWidth() +
               rightInset;
    }

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
