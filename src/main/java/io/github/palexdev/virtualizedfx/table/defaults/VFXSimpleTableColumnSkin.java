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
import java.util.Collection;
import java.util.List;

import io.github.palexdev.mfxcore.controls.BoundLabel;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.TextMeasurementCache;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
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
/// Mouse events are not consumed by the skin, so that they reach the column's behavior and the table, where the
/// resize handlers of the default behavior are, see [VFXTableColumnBehavior].
///
/// ## Text and graphic
///
/// How the two are arranged depends on three of the column's properties.
///
/// The [content display][VFXTableColumn#contentDisplayProperty()] tells what is shown, and on which side of the text
/// the graphic goes:
///
/// | Value | Result |
/// |---|---|
/// | [ContentDisplay#LEFT], [ContentDisplay#RIGHT] | Both are shown, the graphic on that side of the text |
/// | [ContentDisplay#GRAPHIC_ONLY] | Only the graphic is shown |
/// | [ContentDisplay#TEXT_ONLY] | Only the text is shown |
/// | [ContentDisplay#TOP], [ContentDisplay#BOTTOM], [ContentDisplay#CENTER] | Not supported, treated as [ContentDisplay#RIGHT] |
///
/// The column's [alignment][VFXTableColumn#alignmentProperty()] places the content, both horizontally and vertically.
/// What it places horizontally depends on the [VFXSimpleTableColumn#graphicDetachedProperty()]:
///
/// - **attached**, text and graphic stay together, one [gap][VFXTableColumn#graphicTextGapProperty()] apart, and are
///   aligned as a group
/// - **detached**, the graphic goes to the edge of the column, and the text is aligned on its own against the whole
///   column, as long as it stays one gap away from the graphic. So, a centered text is at the column's center whatever
///   the graphic's size is, lined up with the cells below it if they are centered too
///
/// With the graphic on the right of the text:
/// ```
///              detached           attached
/// LEFT      |T.........G|      |T G........|
/// CENTER    |.....T....G|      |....T G....|
/// RIGHT     |........T G|      |........T G|
///```
///
/// When there is not enough room for both, the text is the one that gets truncated. The computation is in
/// [SlottedLayout].
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

    /// Sets the skin's children: the overlay if enabled, the label unless the content display is
    /// [ContentDisplay#GRAPHIC_ONLY], and the graphic if there is one and the content display is not
    /// [ContentDisplay#TEXT_ONLY].
    protected void updateChildren() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        ContentDisplay display = column.getContentDisplay();
        List<Node> children = new ArrayList<>();
        if (column.isEnableOverlay()) children.add(overlay);
        if (display != ContentDisplay.GRAPHIC_ONLY) children.add(label);
        if (graphic != null && display != ContentDisplay.TEXT_ONLY) children.add(graphic);
        getChildren().setAll(children);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Registers the skin's listeners:
    ///
    /// - on [VFXTableColumn#graphicProperty()], the content display and
    ///   [VFXSimpleTableColumn#enableOverlayProperty()], calls [#updateChildren()]
    ///
    /// - on the alignment, the content display, [VFXSimpleTableColumn#graphicDetachedProperty()] and
    ///   [VFXSimpleTableColumn#overlayOnHeaderProperty()], requests a layout of the column
    @Override
    public void install() {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        listen(
            observe(this::updateChildren, column.graphicProperty(), column.contentDisplayProperty(), column.enableOverlayProperty()),
            observe(column::requestLayout,
                column.alignmentProperty(), column.contentDisplayProperty(),
                column.graphicDetachedProperty(), column.overlayOnHeaderProperty()
            )
        );
    }

    /// @return the insets plus the graphic's width and the gap. The graphic counts only if it is shown, the gap only if
    /// the text is shown too. The text is not included, it can be truncated
    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        Node graphic = column.getGraphic();
        ContentDisplay display = column.getContentDisplay();
        if (graphic == null || display == ContentDisplay.TEXT_ONLY) return leftInset + rightInset;
        double gap = (display != ContentDisplay.GRAPHIC_ONLY) ? column.getGraphicTextGap() : 0.0;
        return leftInset + LayoutUtils.boundWidth(graphic) + gap + rightInset;
    }

    /// @return the min width, plus the text's width if the text is shown, measured by a [TextMeasurementCache]. This is
    /// also what [VFXTable]'s default skin measures for the header when autosizing the column, see
    /// [VFXTableColumn#sizeToContent()]
    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        double min = computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
        return (getSkinnable().getContentDisplay() != ContentDisplay.GRAPHIC_ONLY) ? min + tmc.getSnappedWidth() : min;
    }

    /// {@inheritDoc}
    ///
    /// The label and the graphic are laid out in the slots computed by a [SlottedLayout], configured from the column's
    /// properties. See the class docs for the result.
    ///
    /// The overlay is laid out only if the column is in a table. It's as wide as the column, and goes from the column's
    /// top (or bottom, if [VFXSimpleTableColumn#overlayOnHeaderProperty()] is `false`) to the table's bottom.
    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXSimpleTableColumn<T, C> column = getSkinnable();
        new SlottedLayout(label, column.getGraphic())
            .gap(column.getGraphicTextGap())
            .alignment(column.getAlignment())
            .contentDisplay(column.getContentDisplay())
            .detached(column.isGraphicDetached())
            .compute(x, y, w, h)
            .forEach(Slot::layout);

        // Overlay layout
        VFXTable<T> table = column.getTable();
        if (table != null) {
            double minY = column.isOverlayOnHeader() ? 0 : column.getHeight();
            double oW = column.getWidth();
            double oH = table.getLayoutBounds().getHeight() - table.snappedBottomInset() - table.snappedTopInset() - minY;
            overlay.resizeRelocate(0.0, minY, oW, oH);
        }
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// Lays out a text and a graphic side by side in an area. It knows nothing about the column: it's given the two
    /// nodes, it's configured through its fluent setters, and [#compute(double,double,double,double)] gives back a
    /// [Slot] for each node that is shown.
    ///
    /// Either node can be `null`, but not both, there would be nothing to lay out.
    /// ```java
    /// new SlottedLayout(label, icon)
    ///     .gap(8.0)
    ///     .alignment(Pos.CENTER)
    ///     .contentDisplay(ContentDisplay.RIGHT)
    ///     .detached(true)
    ///     .compute(x, y, w, h)
    ///     .forEach(Slot::layout);
    ///```
    public static class SlottedLayout {
        private final Node text;
        private final Node graphic;
        private double gap;
        private Pos alignment = Pos.CENTER_LEFT;
        private ContentDisplay contentDisplay;
        private boolean detached;

        public SlottedLayout(Node text, Node graphic) {
            if (text == null && graphic == null)
                throw new IllegalArgumentException("Nothing to lay out, both the text and the graphic are null!");
            this.text = text;
            this.graphic = graphic;
        }

        /// Sets the space between the text and the graphic. It counts only when both are shown.
        public SlottedLayout gap(double gap) {
            this.gap = gap;
            return this;
        }

        /// Sets how the content is aligned in the area, both horizontally and vertically. [Pos#CENTER_LEFT] by default.
        public SlottedLayout alignment(Pos alignment) {
            this.alignment = alignment;
            return this;
        }

        /// Sets what is shown and on which side of the text the graphic goes:
        ///
        /// - [ContentDisplay#LEFT], both, with the graphic before the text
        /// - [ContentDisplay#GRAPHIC_ONLY] and [ContentDisplay#TEXT_ONLY], only one of the two
        /// - anything else, `null` included, both, with the graphic after the text
        public SlottedLayout contentDisplay(ContentDisplay contentDisplay) {
            this.contentDisplay = contentDisplay;
            return this;
        }

        /// Sets whether the graphic is detached from the text. It counts only when both are shown.
        public SlottedLayout detached(boolean detached) {
            this.detached = detached;
            return this;
        }

        /// Computes the slots for the given area, one for each node that is shown. A node is shown if it's not `null`
        /// and the content display does not hide it. If none is, the result is empty.
        ///
        /// The text gets the width it asks for, or what the graphic and the gap leave of the area, whichever is smaller.
        /// Then the two are placed horizontally:
        ///
        /// - if the graphic is detached, and both are shown, the graphic's slot is the whole area, with the graphic
        ///   aligned at its edge. The text is aligned against the whole area too, but its slot is moved if needed, so
        ///   that it stays at least one gap away from the graphic
        /// - otherwise, text, gap and graphic make a row as wide as its content, which is aligned in the area. When only
        ///   one of the two is shown, the row is just that node
        ///
        /// Vertically every slot takes the whole area, and the node is aligned inside it. [VPos#BASELINE] is treated as
        /// [VPos#CENTER].
        ///
        /// The nodes' widths are snapped as their parent would, if they have one.
        public Collection<Slot> compute(double x, double y, double w, double h) {
            boolean showText = text != null && contentDisplay != ContentDisplay.GRAPHIC_ONLY;
            boolean showGraphic = graphic != null && contentDisplay != ContentDisplay.TEXT_ONLY;
            if (!showText && !showGraphic) return List.of();

            boolean graphicFirst = contentDisplay == ContentDisplay.LEFT;
            VPos vpos = (alignment.getVpos() == VPos.BASELINE) ? VPos.CENTER : alignment.getVpos();
            double align = switch (alignment.getHpos()) {
                case LEFT -> 0.0;
                case CENTER -> 0.5;
                case RIGHT -> 1.0;
            };

            double gw = showGraphic ? LayoutUtils.snappedBoundWidth(graphic) : 0.0;
            double gap = (showText && showGraphic) ? this.gap : 0.0;
            double tw = showText ? NumberUtils.clamp(LayoutUtils.snappedBoundWidth(text), 0.0, w - gw - gap) : 0.0;

            List<Slot> slots = new ArrayList<>(2);
            if (detached && showText && showGraphic) {
                double min = graphicFirst ? x + gw + gap : x;
                double max = (graphicFirst ? x + w : x + w - gw - gap) - tw;
                double tx = NumberUtils.clamp(x + (w - tw) * align, min, max);
                slots.add(new Slot(text, tx, y, tw, h, HPos.LEFT, vpos));
                slots.add(new Slot(graphic, x, y, w, h, graphicFirst ? HPos.LEFT : HPos.RIGHT, vpos));
                return slots;
            }

            double rx = x + Math.max(0.0, w - (tw + gap + gw)) * align;
            if (showText) slots.add(new Slot(text, graphicFirst ? rx + gw + gap : rx, y, tw, h, HPos.LEFT, vpos));
            if (showGraphic) slots.add(new Slot(graphic, graphicFirst ? rx : rx + tw + gap, y, gw, h, HPos.LEFT, vpos));
            return slots;
        }
    }

    /// An area, the node that goes in it and how the node is aligned inside it.
    ///
    /// @param node the node to lay out
    /// @param x where the area starts horizontally
    /// @param y where the area starts vertically
    /// @param w the area's width
    /// @param h the area's height
    /// @param hpos the node's horizontal alignment in the area
    /// @param vpos the node's vertical alignment in the area
    public record Slot(Node node, double x, double y, double w, double h, HPos hpos, VPos vpos) {

        /// Sizes the node to fit the area, up to its max size, and positions it as specified by the two alignments.
        /// Positions and sizes are snapped. See [Region#layoutInArea(Node,double,double,double,double,double,Insets,boolean,boolean,HPos,VPos,boolean)].
        public void layout() {
            Region.layoutInArea(node, x, y, w, h, 0, Insets.EMPTY, true, true, hpos, vpos, true);
        }
    }
}
