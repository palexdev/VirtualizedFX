/*
 * Copyright (C) 2022 Parisi Alessandro
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

package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.bindings.ExternalSource;
import io.github.palexdev.mfxcore.base.bindings.MFXBindings;
import io.github.palexdev.mfxcore.base.bindings.Mapper;
import io.github.palexdev.mfxcore.base.bindings.MappingSource;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxcore.utils.resize.RegionDragResizer;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.TableState;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.scene.control.Labeled;
import javafx.scene.control.Skin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.function.Function;

/**
 * Default, simple implementation of {@link TableColumn}, extends {@link Labeled} and has its own
 * skin {@link DefaultTableColumnSkin}.
 * <p></p>
 * You may or may not use this implementation, or you could use it as a base to build more complex types of
 * columns.
 * <p></p>
 * This simple implementation allows to specify the name of the column (since it extends {@link Labeled}) and to
 * add one icon to it with the {@link #graphicProperty()}. It also allows to set on which side the icon is placed with
 * {@link #iconAlignmentProperty()}.
 * <p></p>
 * <b>More info on the "inViewport" property implementation</b>
 * <p>
 * The "inViewport" feature is implemented by using custom bindings provided by MFXCore, {@link MFXBindings}.
 * When the column has not been laid out yet the property is false. When it's placed in a Scene, the value is computed
 * depending on the table's current {@link VirtualTable#columnsLayoutModeProperty()}.
 * <p>
 * <p> - For {@link ColumnsLayoutMode#FIXED} this simply checks if its index (retrieved from {@link VirtualTable#getIndexedColumns()}),
 * is in the current range of displayed columns, taken from the current {@link VirtualTable#stateProperty()}
 * <p></p>
 * <p> - For {@link ColumnsLayoutMode#VARIABLE} the computation is a bit more elaborated, it uses a simpler variation
 * of {@link Bounds#intersects(double, double, double, double)} (just needs to check the x). The formula is:
 * {@code (x + w >= viewBounds.getMin()) && (x <= viewBounds.getMax())}. The x is retrieved from the {@link #boundsInParentProperty()},
 * the w is the current width, and the viewBounds are given by: {@code table.getHPos(), table.getHPos() + table.getWidth()}
 *
 * @param <T> the type of items in the table
 * @param <C> the type of cells this column will produce
 */
public class DefaultTableColumn<T, C extends TableCell<T>> extends Labeled implements TableColumn<T, C> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "table-column";
	private final VirtualTable<T> table;

	private final FunctionProperty<T, C> cellFactory = new FunctionProperty<>();
	private final BooleanProperty inViewport = new SimpleBooleanProperty(false) {
		@Override
		public void set(boolean newValue) {
			boolean oldValue = get();
			super.set(newValue);

			if (oldValue != newValue) {
				onVisibilityChanged(oldValue, newValue);
			}
		}

		@Override
		public boolean isBound() {
			MFXBindings bindings = MFXBindings.instance();
			return super.isBound() ||
					(bindings.isBound(this) && !bindings.isIgnoreBinding(this));
		}
	};

	//================================================================================
	// Constructors
	//================================================================================
	public DefaultTableColumn(VirtualTable<T> table) {
		this(table, "");
	}

	public DefaultTableColumn(VirtualTable<T> table, String text) {
		super(text);
		this.table = table;
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().setAll(STYLE_CLASS);
		defaultCellFactory();
		setupRegionDragResizer();

		MFXBindings bindings = MFXBindings.instance();
		MappingSource<TableState<T>, Boolean> mainSource = MappingSource.<TableState<T>, Boolean>of(table.stateProperty())
				.setTargetUpdater(Mapper.of(val -> {
					VirtualTable<T> table = getTable();
					if (table == null || getScene() == null || getParent() == null) return false;

					if (table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) {
						TableState<T> state = table.getState();
						if (state.isEmptyAll()) return false;

						int index = table.getIndexedColumns().getOrDefault(this, -2);
						return IntegerRange.inRangeOf(index, state.getColumnsRange());
					}

					try {
						double tW = table.getWidth();
						double hPos = table.getHPos();
						DoubleRange viewBounds = DoubleRange.of(hPos, hPos + tW);

						double x = getBoundsInParent().getMinX();
						double w = getWidth();
						return (x + w >= viewBounds.getMin()) && (x <= viewBounds.getMax());
					} catch (Exception ex) {
						return false;
					}
				}), (o, n) -> inViewport.set(n));
		bindings.bind(inViewport)
				.source(mainSource)
				.addInvalidatingSource(ExternalSource.of(table.stateProperty(), (o, n) -> {
					if (table.getColumnsLayoutMode() != ColumnsLayoutMode.FIXED) return;
					bindings.invTarget(inViewport);
				}))
				.addInvalidatingSource(ExternalSource.of(boundsInParentProperty(), (o, n) -> {
					if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) return;
					if (o.getMinX() != n.getMinX()) bindings.invTarget(inViewport);
				}))
				.addInvalidatingSource(ExternalSource.of(table.positionProperty(), (o, n) -> {
					if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) return;
					if (o.getX() != n.getX()) bindings.invTarget(inViewport);
				}))
				.addInvalidatingSource(ExternalSource.of(table.needsViewportLayoutProperty(), (o, n) -> {
					if (!n) bindings.invTarget(inViewport);
				}))
				.get();
	}

	/**
	 * Sets the default cell factory. Produces cells of type {@link SimpleTableCell}.
	 */
	@SuppressWarnings("unchecked")
	public void defaultCellFactory() {
		setCellFactory(t -> (C) new SimpleTableCell<>(t, e -> e));
	}

	/**
	 * Initializes the 'controller' responsible for resizing this column when {@link VirtualTable#columnsLayoutModeProperty()}
	 * is set to {@link ColumnsLayoutMode#VARIABLE}.
	 * <p></p>
	 * By overriding this you can specify your own {@link RegionDragResizer}.
	 */
	protected void setupRegionDragResizer() {
		PseudoClass dragged = PseudoClass.getPseudoClass("dragged");
		RegionDragResizer rdr = new RegionDragResizer(this, (node, x, y, w, h) -> {
			node.setPrefWidth(w);
			table.requestViewportLayout();
		}) {
			@Override
			protected void handleDragged(MouseEvent event) {
				if (table == null || table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
				super.handleDragged(event);
				pseudoClassStateChanged(dragged, true);
			}

			@Override
			protected void handleMoved(MouseEvent event) {
				if (table == null || table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
				super.handleMoved(event);
			}

			@Override
			protected void handlePressed(MouseEvent event) {
				if (table == null || table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;
				super.handlePressed(event);
			}

			@Override
			protected void handleReleased(MouseEvent event) {
				super.handleReleased(event);
				pseudoClassStateChanged(dragged, false);
			}
		};
		rdr.setAllowedZones(Zone.CENTER_RIGHT);
		rdr.makeResizable();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected Skin<?> createDefaultSkin() {
		return new DefaultTableColumnSkin<>(this);
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	@Override
	public Region getRegion() {
		return this;
	}

	@Override
	public VirtualTable<T> getTable() {
		return table;
	}

	@Override
	public Function<T, C> getCellFactory() {
		return cellFactory.get();
	}

	@Override
	public FunctionProperty<T, C> cellFactoryProperty() {
		return cellFactory;
	}

	@Override
	public void setCellFactory(Function<T, C> cellFactory) {
		this.cellFactory.set(cellFactory);
	}

	@Override
	public boolean isInViewport() {
		return inViewport.get();
	}

	@Override
	public ReadOnlyBooleanProperty inViewportProperty() {
		return inViewport;
	}

	@Override
	public void onVisibilityChanged(boolean before, boolean now) {
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableObjectProperty<HPos> iconAlignment = new StyleableObjectProperty<>(
			StyleableProperties.ICON_ALIGNMENT,
			this,
			"iconAlignment",
			HPos.RIGHT
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

	public HPos getIconAlignment() {
		return iconAlignment.get();
	}

	/**
	 * Specifies the side on which the icon will be placed.
	 * <p></p>
	 * {@link HPos#CENTER} is ignored by the default skin, the icon will be placed to the right
	 * <p></p>
	 * This is settable via CSS with the "-fx-icon-alignment" property.
	 */
	public StyleableObjectProperty<HPos> iconAlignmentProperty() {
		return iconAlignment;
	}

	public void setIconAlignment(HPos iconAlignment) {
		this.iconAlignment.set(iconAlignment);
	}

	public boolean isEnableOverlay() {
		return enableOverlay.get();
	}

	/**
	 * Specifies whether the default skin should enable the overlay.
	 * <p></p>
	 * {@link VirtualTable} is organized by rows. This means that by default there is no way in the UI to display
	 * when a column is selected or hovered by the mouse. The default skin allows to do this by adding an extra node that
	 * extends from the column all the way down to the table's bottom. This allows to do cool tricks with CSS.
	 * <p></p>
	 * One thing to keep in mind though is that if you define a background color for the overlay make sure that it is
	 * opaque otherwise it will end up covering the rows cells.
	 * <p></p>
	 * This is settable via CSS with the "-fx-enable-overlay" property.
	 */
	public StyleableBooleanProperty enableOverlayProperty() {
		return enableOverlay;
	}

	public void setEnableOverlay(boolean enableOverlay) {
		this.enableOverlay.set(enableOverlay);
	}

	public boolean isOverlayOnHeader() {
		return overlayOnHeader.get();
	}

	/**
	 * Specifies whether the overlay should also cover the header of the column,
	 * the part where the text and the icon reside.
	 * <p></p>
	 * This is settable via CSS with the "-fx-overlay-on-header" property.
	 */
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
		private static final StyleablePropertyFactory<DefaultTableColumn<?, ?>> FACTORY = new StyleablePropertyFactory<>(Labeled.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<DefaultTableColumn<?, ?>, HPos> ICON_ALIGNMENT =
				FACTORY.createEnumCssMetaData(
						HPos.class,
						"-fx-icon-alignment",
						DefaultTableColumn::iconAlignmentProperty,
						HPos.RIGHT
				);

		private static final CssMetaData<DefaultTableColumn<?, ?>, Boolean> ENABLE_OVERLAY =
				FACTORY.createBooleanCssMetaData(
						"-fx-enable-overlay",
						DefaultTableColumn::enableOverlayProperty,
						true
				);

		private static final CssMetaData<DefaultTableColumn<?, ?>, Boolean> OVERLAY_ON_HEADER =
				FACTORY.createBooleanCssMetaData(
						"-fx-overlay-on-header",
						DefaultTableColumn::overlayOnHeaderProperty,
						false
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Labeled.getClassCssMetaData(),
					ICON_ALIGNMENT, ENABLE_OVERLAY, OVERLAY_ON_HEADER
			);
		}

		public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
			return StyleableProperties.cssMetaDataList;
		}
	}
}
