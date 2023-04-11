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

import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.cell.MappingTableCell;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.TableRow;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.function.Function;

/**
 * Basic implementation of {@link MappingTableCell}. This is intended to be used with models that do not
 * use JavaFX's properties.
 * <p></p>
 * Extends {@link HBox} and has a {@link Label} to represent the item as text.
 * <p></p>
 * If the item change, the cell cannot automatically detect it, but you can use {@link VirtualTable#updateTable(boolean)}
 * to programmatically update it.
 * <p></p>
 * This implementation has the following properties:
 * <p> - Holds the column this cell is relative to, {@link #columnProperty()}
 * <p> - Holds the row containing this cell, {@link #rowProperty()}
 * <p> - Holds the item visualized by the cell, {@link #itemProperty()}
 * <p> - Holds the column index this cell is relative to, {@link #indexProperty()}
 * <p></p>
 * Since the control will visualize tabular data we generally want to extract a certain property from a certain object
 * of the model. The cell specifies a {@link Function} called "extractor" that does exactly this.
 * <p>
 * <pre>
 * {@code
 * // Lets suppose this cell will represent the name of a User
 * // The extractor function will look like this...
 * user -> user.getName();
 * }
 * </pre>
 * <p></p>
 * This also specifies a way to convert the extracted value to a {@link String} that will be used as the cell's text,
 * {@link StringConverter}. By default, this is set to use {@link Objects#toString(Object)}.
 * <p>
 * The cells text is updated by the {@link #invalidate()} method.
 * <p></p>
 * Last but not least. Note that this cell implementation makes use of the {@link TableColumn#inViewportProperty()} when
 * the {@link VirtualTable#columnsLayoutModeProperty()} is set to {@link ColumnsLayoutMode#VARIABLE}. If the column is not
 * visible (therefore this cell is not visible either) it hides itself automatically.
 *
 * @param <T> the type of items in the table
 * @param <E> the type of property extracted from the T objects
 */
public class SimpleTableCell<T, E> extends HBox implements MappingTableCell<T, E> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "vtable-cell";
	private final ReadOnlyObjectWrapper<TableColumn<T, ? extends TableCell<T>>> column = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<TableRow<T>> row = new ReadOnlyObjectWrapper<>();
	private final ObjectProperty<T> item = new SimpleObjectProperty<>();
	private final IntegerProperty index = new SimpleIntegerProperty();
	private Function<T, E> extractor;
	private StringConverter<E> converter;

	protected final Label label;
	protected When<Boolean> ivwWhen;

	//================================================================================
	// Constructors
	//================================================================================
	public SimpleTableCell(T item, Function<T, E> extractor) {
		this(item, extractor, FunctionalStringConverter.to(Objects::toString));
	}

	public SimpleTableCell(T item, Function<T, E> extractor, StringConverter<E> converter) {
		this.extractor = extractor;
		this.converter = converter;
		setItem(item);
		label = new Label();
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		setHgrow(label, Priority.ALWAYS);
		getChildren().setAll(label);
		init();
	}

	//================================================================================
	// Methods
	//================================================================================
	protected void init() {
		getStyleClass().add(STYLE_CLASS);
		invalidate();

		columnProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue != null && ivwWhen != null) ivwWhen.dispose();
			if (newValue != null) {
				VirtualTable<T> table = newValue.getTable();
				if (table == null || table.getColumnsLayoutMode() == ColumnsLayoutMode.FIXED) return;

				ivwWhen = When.onChanged(newValue.inViewportProperty())
					.then((o, n) -> setVisible(getColumn() != null && getColumn().isInViewport()))
					.executeNow()
					.listen();
			}
		});
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public Node getNode() {
		return this;
	}

	@Override
	public void updateItem(T item) {
		setItem(item);
		invalidate();
	}

	@Override
	public void updateIndex(int index) {
		setIndex(index);
	}

	@Override
	public void updateColumn(TableColumn<T, ? extends TableCell<T>> column) {
		setColumn(column);
	}

	@Override
	public void updateRow(int rIndex, DefaultTableRow<T> row) {
		setRow(row);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * By default this is only invoked after {@link #updateItem(Object)} has also been called.
	 * This is responsible for extracting the value E from the current {@link #itemProperty()}, converting it
	 * to a {@link String} with the specified {@link StringConverter} and then updating the label's text
	 */
	@Override
	public void invalidate() {
		T item = getItem();
		E e = extractor.apply(item);
		String s = converter.toString(e);
		label.setText(s);
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public TableColumn<T, ? extends TableCell<T>> getColumn() {
		return column.get();
	}

	/**
	 * Holds the reference to the {@link TableColumn} this cells is associated to.
	 */
	public ReadOnlyObjectProperty<TableColumn<T, ? extends TableCell<T>>> columnProperty() {
		return column.getReadOnlyProperty();
	}

	protected void setColumn(TableColumn<T, ? extends TableCell<T>> column) {
		this.column.set(column);
	}

	public TableRow<T> getRow() {
		return row.get();
	}

	/**
	 * Specifies the {@link TableRow} which contains this cell.
	 */
	public ReadOnlyObjectProperty<TableRow<T>> rowProperty() {
		return row.getReadOnlyProperty();
	}

	protected void setRow(DefaultTableRow<T> row) {
		this.row.set(row);
	}

	public T getItem() {
		return item.get();
	}

	/**
	 * Specifies the item represented by this cell.
	 */
	public ObjectProperty<T> itemProperty() {
		return item;
	}

	public void setItem(T item) {
		this.item.set(item);
	}

	public int getIndex() {
		return index.get();
	}

	/**
	 * Specifies the index of the column this cell is associated to.
	 */
	public IntegerProperty indexProperty() {
		return index;
	}

	public void setIndex(int index) {
		this.index.set(index);
	}

	@Override
	public Function<T, E> getExtractor() {
		return extractor;
	}

	@Override
	public void setExtractor(Function<T, E> extractor) {
		this.extractor = extractor;
	}

	@Override
	public StringConverter<E> getConverter() {
		return converter;
	}

	@Override
	public void setConverter(StringConverter<E> converter) {
		this.converter = converter;
	}
}

