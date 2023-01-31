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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.Collection;

/**
 * Default skin implementation for {@link VirtualTable}.
 * <p></p>
 * The table's layout is quite complex as there is a rather unique case about the scrolling.
 * Vertical scrolling should affect only the rows, but th Horizontal scrolling has to affect
 * both the columns and the rows.
 * <p>
 * For this reason this skin is organized in three containers and two clips:
 * <p> - The "cContainer" is a Pane responsible for containing the columns (style class: columns-container)
 * <p> - The "rContainer" is a Pane responsible for containing the rows (style class: rows-container)
 * <p> - The top container which contains the above ones is the "viewport" (style class: viewport)
 * <p></p>
 * The two clips:
 * <p> - One clip is assigned to the viewport and avoid content leaking during horizontal scrolling and at the
 * bottom of the table
 * <p> - The other clip is assigned to the rContainer and avoids rows leaking on top of the cContainer,
 * <p></p>
 * <p>
 * So we have two moving parts:
 * <p> - The viewport moves only horizontally through its translateX property
 * <p> - The rContainer moves only vertically through its translateY property
 * <p> Both positions are given by the bindings created in the {@link TableHelper}.
 */
public class VirtualTableSkin<T> extends SkinBase<VirtualTable<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final Pane viewport;
	private final Rectangle clip;

	private final Pane cContainer;
	private final Pane rContainer;
	private final Rectangle rClip;

	protected TableManager<T> manager;
	protected final double DEFAULT_SIZE = 100.0;

	private ListChangeListener<? super T> itemsChanged;
	private ChangeListener<? super ObservableList<T>> listChanged;
	private ChangeListener<? super TableState<T>> stateChanged;
	private ChangeListener<? super TableHelper> helperChanged;
	private ChangeListener<? super Boolean> layoutRequestListener;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualTableSkin(VirtualTable<T> table) {
		super(table);
		manager = table.getViewportManager();

		// Initialize Containers
		cContainer = new Pane() {
			@Override
			protected void layoutChildren() {
				if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) {
					TableHelper helper = table.getTableHelper();
					if (helper == null)
						throw new IllegalStateException("Could not lay out columns as TableHelper is null");
					helper.computePositions(table.getState(), true, false);
					helper.computeEstimatedSize();
					table.requestViewportLayout();
					helper.computePositions(table.getState(), true, false);
					helper.layout();
					layoutCompleted();
				}
			}
		};
		cContainer.getStyleClass().add("columns-container");

		rContainer = new Pane() {
			@Override
			protected void layoutChildren() {
			}
		};
		rContainer.getStyleClass().add("rows-container");

		// Initialize Viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				VirtualTable<T> table = getSkinnable();

				// First layout the columns and rows containers
				double w = table.getWidth();
				double h = table.getHeight();
				double cH = table.getColumnSize().getHeight();
				double rH = Math.max(h - cH, LayoutUtils.boundHeight(rContainer));
				Position cPos = LayoutUtils.computePosition(
						table, cContainer,
						0, 0, w, cH, 0,
						Insets.EMPTY, HPos.LEFT, VPos.TOP
				);
				cContainer.resizeRelocate(cPos.getX(), cPos.getY(), w, cH);

				Position rPos = LayoutUtils.computePosition(
						table, rContainer,
						0, cH, w, rH, 0,
						Insets.EMPTY, HPos.LEFT, VPos.TOP
				);
				rContainer.resizeRelocate(rPos.getX(), rPos.getY(), w, rH);

				// Then proceed with columns/rows/cells layout
				TableHelper helper = table.getTableHelper();
				if (helper == null)
					throw new IllegalStateException("Cannot process layout request as TableHelper is null");
				helper.layout();
				layoutCompleted();
			}
		};
		viewport.getStyleClass().add("viewport");

		// Initialize Clips
		clip = new Rectangle();
		clip.widthProperty().bind(viewport.widthProperty());
		clip.heightProperty().bind(viewport.heightProperty());
		clip.translateXProperty().bind(viewport.translateXProperty().multiply(-1));
		clip.arcWidthProperty().bind(table.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(table.clipBorderRadiusProperty());
		viewport.setClip(clip);

		rClip = new Rectangle();
		rClip.widthProperty().bind(rContainer.widthProperty());
		rClip.heightProperty().bind(rContainer.heightProperty());
		rClip.translateXProperty().bind(viewport.translateXProperty().multiply(-1));
		rClip.translateYProperty().bind(rContainer.translateYProperty().multiply(-1));
		rContainer.setClip(rClip);

		// Build Listeners
		itemsChanged = this::onItemsChanged;
		listChanged = (observable, oldValue, newValue) -> onListChanged(oldValue, newValue);
		stateChanged = (observable, oldValue, newValue) -> onStateChanged(oldValue, newValue);
		helperChanged = (observable, oldValue, newValue) -> onHelperChanged(newValue);
		layoutRequestListener = (observable, oldValue, newValue) -> onLayoutRequest(newValue);

		// Initialize Bindings
		TableHelper helper = table.getTableHelper();
		viewport.translateXProperty().bind(helper.xPosBinding());
		rContainer.translateYProperty().bind(helper.yPosBinding());

		// Finalize Initialization
		helper.computeEstimatedSize();
		viewport.getChildren().addAll(cContainer, rContainer);
		getChildren().addAll(viewport);
		addListeners();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Registers the following listeners:
	 * <p> - A listener on the {@link VirtualTable#getItems()} list which calls {@link #onItemsChanged(Change)}
	 * <p> - A listener on the {@link VirtualTable#itemsProperty()} which calls {@link #onListChanged(ObservableList, ObservableList)}
	 * <p> - A listener on the {@link VirtualTable#stateProperty()} which calls {@link #onStateChanged(TableState, TableState)}
	 * <p> - A listener on the {@link VirtualTable#tableHelperProperty()} which calls {@link #onHelperChanged(TableHelper)}
	 * <p> - A listener on the {@link VirtualTable#needsViewportLayoutProperty()} which calls {@link #onLayoutRequest(boolean)}
	 */
	private void addListeners() {
		VirtualTable<T> table = getSkinnable();

		table.getItems().addListener(itemsChanged);
		table.itemsProperty().addListener(listChanged);
		table.stateProperty().addListener(stateChanged);
		table.tableHelperProperty().addListener(helperChanged);
		table.needsViewportLayoutProperty().addListener(layoutRequestListener);
	}

	/**
	 * The default implementation is responsible for telling the {@link TableManager} to process the occurred
	 * {@link Change} and produce eventually a new state.
	 * <p>
	 * This also ensures after the change that the viewport's estimated size is correct by calling {@link TableHelper#computeEstimatedSize()}.
	 */
	protected void onItemsChanged(Change<? extends T> change) {
		VirtualTable<T> table = getSkinnable();
		manager.onChange(change);

		TableHelper helper = table.getTableHelper();
		helper.computeEstimatedSize();
	}

	/**
	 * Tells the table's components what to do when the items data structure changes.
	 * By default, this causes the removal of the itemsChanged listener from the old structure,
	 * which is then added to the new one. The estimated size is also recomputed and the viewport reset.
	 */
	protected void onListChanged(ObservableList<T> oldValue, ObservableList<T> newValue) {
		if (oldValue != null) oldValue.removeListener(itemsChanged);

		TableHelper helper = getSkinnable().getTableHelper();
		if (newValue != null) {
			newValue.addListener(itemsChanged);
			helper.computeEstimatedSize();
			manager.reset();
		}
	}

	/**
	 * The default implementation is responsible for updating the cContainer and rContainer children lists.
	 * <p></p>
	 * Columns are updated only if the range of columns of the new state is not equal to the one in the old state.
	 * Columns are retrieved as nodes with {@link TableState#getColumnsAsNodes()}
	 * <p>
	 * Rows are updated only if {@link TableState#haveRowsChanged()} is true, rows are retrieved with
	 * {@link TableState#getRows()}
	 */
	protected void onStateChanged(TableState<T> oldValue, TableState<T> newValue) {
		if (newValue.isEmpty()) {
			rContainer.getChildren().clear();
		}

		if (IntegerRange.of(-1).equals(newValue.getColumnsRange())) {
			cContainer.getChildren().clear();
		} else if (!oldValue.getColumnsRange().equals(newValue.getColumnsRange()) || cContainer.getChildren().isEmpty()) {
			cContainer.getChildren().setAll(newValue.getColumnsAsNodes());
		}

		if (!newValue.isEmpty() && newValue.haveRowsChanged()) {
			Collection<TableRow<T>> rows = newValue.getRows().values();
			rContainer.getChildren().setAll(rows);
		}
	}

	/**
	 * The default implementation is responsible for re-binding the viewport's translateX and the rContainer's translateY
	 * properties to the bindings created by the new {@link TableHelper}.
	 *
	 * @throws IllegalStateException if the new helper is null
	 */
	protected void onHelperChanged(TableHelper newValue) {
		if (newValue == null)
			throw new IllegalStateException("The new provided TableHelper is null, you will encounter problems");

		viewport.translateXProperty().bind(newValue.xPosBinding());
		rContainer.translateYProperty().bind(newValue.yPosBinding());
	}

	/**
	 * The default implementation is responsible for calling {@link Parent#requestLayout()} on the viewport.
	 */
	protected void onLayoutRequest(boolean newValue) {
		if (newValue) viewport.requestLayout();
	}

	/**
	 * This method simply sets {@link VirtualTable#needsViewportLayoutProperty()} to false.
	 * This should be used by custom skin implementations to indicate that a layout request has been completed.
	 */
	protected void layoutCompleted() {
		getSkinnable().setNeedsViewportLayout(false);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + DEFAULT_SIZE + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return topInset + DEFAULT_SIZE + bottomInset;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VirtualTable<T> table = getSkinnable();
		Position vPos = LayoutUtils.computePosition(
				table, viewport,
				0, 0, w, h, 0,
				Insets.EMPTY, HPos.LEFT, VPos.TOP
		);
		viewport.resizeRelocate(
				vPos.getX(), vPos.getY(),
				w + snappedLeftInset() + snappedRightInset(),
				h + snappedTopInset() + snappedBottomInset()
		);
	}

	@Override
	public void dispose() {
		VirtualTable<T> table = getSkinnable();

		table.getItems().removeListener(itemsChanged);
		table.itemsProperty().removeListener(listChanged);
		table.stateProperty().removeListener(stateChanged);
		table.tableHelperProperty().removeListener(helperChanged);
		table.needsViewportLayoutProperty().removeListener(layoutRequestListener);

		itemsChanged = null;
		listChanged = null;
		stateChanged = null;
		helperChanged = null;
		layoutRequestListener = null;
		manager = null;
		super.dispose();
	}
}
