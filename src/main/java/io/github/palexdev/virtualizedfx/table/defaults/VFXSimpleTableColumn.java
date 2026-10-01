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

import java.util.List;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;

/// Concrete and simple implementation of [VFXTableColumn]. Has its own skin: [VFXSimpleTableColumnSkin].
///
/// These are the features this implementation offers:
///
/// - the header's text and graphic can be arranged in several ways, through the column's [#alignmentProperty()], its
///   [#contentDisplayProperty()] and the [#graphicDetachedProperty()]. See [VFXSimpleTableColumnSkin] for how they combine
/// - the [#enableOverlayProperty()] makes the column display an extra node which can be used to indicate selection or hovering
/// - the [#overlayOnHeaderProperty()] makes the aforementioned node cover the column's header too
///
/// The column starts with the graphic on the right of the text, [ContentDisplay#RIGHT], and a gap of 8 between the two.
/// Both can be changed as usual, in code or in CSS.
public class VFXSimpleTableColumn<T, C extends VFXTableCell<T>> extends VFXTableColumn<T, C> {

    //================================================================================
    // Constructors
    //================================================================================

    public VFXSimpleTableColumn() {}

    public VFXSimpleTableColumn(String text) {
        super(text);
    }

    public VFXSimpleTableColumn(String text, Node graphic) {
        super(text, graphic);
    }

    {
        // Prevent overlay from capturing mouse events on rows (mouse transparent is not enough)
        setPickOnBounds(false);
        // Init defaults
        StyleUtils.initProperty(graphicTextGapProperty(), 8.0);
        StyleUtils.initProperty(contentDisplayProperty(), ContentDisplay.RIGHT);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public Supplier<MFXSkinBase<? extends Node>> defaultSkinFactory() {
        return () -> new VFXSimpleTableColumnSkin<>(this);
    }

    //================================================================================
    // Styleable Properties
    //================================================================================

    private final StyleableBooleanProperty graphicDetached = new StyleableBooleanProperty(
        StyleableProperties.GRAPHIC_DETACHED,
        this,
        "graphicDetached",
        true
    );

    private final StyleableBooleanProperty enableOverlay = new StyleableBooleanProperty(
        StyleableProperties.ENABLE_OVERLAY,
        this,
        "enableOverlay",
        true
    );

    private final StyleableBooleanProperty overlayOnHeader = new StyleableBooleanProperty(
        StyleableProperties.OVERLAY_ON_HEADER,
        this,
        "overlayOnHeader",
        false
    );

    public boolean isGraphicDetached() {
        return graphicDetached.get();
    }

    /// Specifies whether the graphic is detached from the text.
    ///
    /// When detached, the default skin, [VFXSimpleTableColumnSkin], places the graphic at the edge of the column and
    /// aligns the text on its own. Otherwise, the two stay together, one gap apart, and are aligned as a group.<br >
    /// It only matters when both are shown, side by side. See the skin for the details.
    ///
    /// Can be set in CSS via the property: '-vfx-graphic-detached'.
    public StyleableBooleanProperty graphicDetachedProperty() {
        return graphicDetached;
    }

    public void setGraphicDetached(boolean graphicDetached) {
        this.graphicDetached.set(graphicDetached);
    }

    public boolean isEnableOverlay() {
        return enableOverlay.get();
    }

    /// Specifies whether the default skin, [VFXSimpleTableColumnSkin], should show the overlay.
    ///
    /// [VFXTable] is organized by rows. This means that by default, there is no way in the UI to display when a column
    /// is selected or hovered by the mouse. The default skin allows to do this by adding an extra node that extends from
    /// the column all the way down to the table's bottom. This allows doing cool tricks with CSS. The node has no style
    /// by default, so it's not visible until you define one, it can be selected in CSS as '.overlay'.
    ///
    /// One thing to keep in mind, though, is that the overlay sits above the rows. So, if you define a background color
    /// for it, make sure that it is translucent, otherwise it will end up covering the cells.
    ///
    /// Can be set in CSS via the property: '-vfx-enable-overlay'.
    public StyleableBooleanProperty enableOverlayProperty() {
        return enableOverlay;
    }

    public void setEnableOverlay(boolean enableOverlay) {
        this.enableOverlay.set(enableOverlay);
    }

    public boolean isOverlayOnHeader() {
        return overlayOnHeader.get();
    }

    /// Specifies whether the overlay should also cover the header of the column, the part where the text and the graphic
    /// reside. See [#enableOverlayProperty()].
    ///
    /// Can be set in CSS via the property: '-vfx-overlay-on-header'.
    public StyleableBooleanProperty overlayOnHeaderProperty() {
        return overlayOnHeader;
    }

    public void setOverlayOnHeader(boolean overlayOnHeader) {
        this.overlayOnHeader.set(overlayOnHeader);
    }

    //================================================================================
    // CssMetaData
    //================================================================================

    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXSimpleTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(VFXTableColumn.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, Boolean> GRAPHIC_DETACHED =
            FACTORY.createBooleanCssMetaData(
                "-vfx-graphic-detached",
                VFXSimpleTableColumn::graphicDetachedProperty,
                true
            );

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, Boolean> ENABLE_OVERLAY =
            FACTORY.createBooleanCssMetaData(
                "-vfx-enable-overlay",
                VFXSimpleTableColumn::enableOverlayProperty,
                true
            );

        private static final CssMetaData<VFXSimpleTableColumn<?, ?>, Boolean> OVERLAY_ON_HEADER =
            FACTORY.createBooleanCssMetaData(
                "-vfx-overlay-on-header",
                VFXSimpleTableColumn::overlayOnHeaderProperty,
                false
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                VFXTableColumn.getClassCssMetaData(),
                GRAPHIC_DETACHED, ENABLE_OVERLAY, OVERLAY_ON_HEADER
            );
        }
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }
}
