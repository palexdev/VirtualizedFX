package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.cells.MappingTableCell;
import io.github.palexdev.virtualizedfx.cells.TableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;
import javafx.beans.property.*;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

// TODO move to cells package? (also make base sub-package)
// TODO can we avoid the item property and use the one from the row instead? (modify point 2 VFXTableRow eventually)
// TODO we could, by the same strategy, also avoid the index property

/**
 * Basic implementation of {@link MappingTableCell}. This is intended to be used with models that do not
 * use JavaFX's properties. Extends {@link HBox} and has a {@link Label} to display the extracted {@link E} data as text.
 * The default style class is set to ".table-cell".
 * <p>
 * The cell's text is updated by the {@link #invalidate()} method.
 * <p>
 * If the data changes, the cell cannot automatically detect it, but you can use {@link VFXTable#update()}
 * to programmatically update it.
 * <p></p>
 * This implementation has the following properties:
 * <p> - The {@link #columnProperty()} holds the column that created the cell
 * <p> - The {@link #rowProperty()} holds the row containing the cell
 * <p> - The {@link #itemProperty()} holds the item {@link T} from which the cells extract data of type {@link E}
 * <p> - The {@link #indexProperty()} holds the index of the related column
 * <p> - The default {@link StringConverter} is set to use {@link Objects#toString(Object)}.
 */
public class VFXSimpleTableCell<T, E> extends HBox implements MappingTableCell<T, E>, VFXStyleable {
	//================================================================================
	// Properties
	//================================================================================
	private final ReadOnlyObjectWrapper<VFXTableColumn<T, ? extends TableCell<T>>> column = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<VFXTableRow<T>> row = new ReadOnlyObjectWrapper<>();
	private final IntegerProperty index = new SimpleIntegerProperty(-1);
	private final ObjectProperty<T> item = new SimpleObjectProperty<>();
	private Function<T, E> extractor;
	private StringConverter<E> converter;

	protected final Label label;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXSimpleTableCell(T item, Function<T, E> extractor) {
		this(item, extractor, FunctionalStringConverter.to(Objects::toString));
	}

	public VFXSimpleTableCell(T item, Function<T, E> extractor, StringConverter<E> converter) {
		this.extractor = extractor;
		this.converter = converter;

		label = new Label();
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		setHgrow(label, Priority.ALWAYS);
		getChildren().add(label);

		updateItem(item);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().setAll(defaultStyleClasses());
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * @return the {@link VFXTable} instance by using the {@link #rowProperty()}. {@code Null} if the row instance has
	 * not been set yet.
	 */
	public VFXTable<T> getTable() {
		return Optional.ofNullable(getRow())
			.map(VFXTableRow::getTable)
			.orElse(null);
	}

	/**
	 * @return the index of the row containing this cell by using the {@link #rowProperty()}. -1 if the row instance has
	 * not been set yet.
	 */
	public int getRowIndex() {
		return Optional.ofNullable(getRow())
			.map(VFXTableRow::getIndex)
			.orElse(-1);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public List<String> defaultStyleClasses() {
		return List.of("table-cell");
	}

	@Override
	public Node toNode() {
		return this;
	}

	/**
	 * Updates the cell's label text by first getting the {@link T} item, then extracting a piece of data {@link E} from
	 * it by using the 'extractor' function, and finally converting this data to a String by using the set {@link StringConverter}.
	 * <p>
	 * If the item {@link T} is {@code null} then the label's text will be an empty String.
	 */
	@Override
	public void invalidate() {
		T item = getItem();
		if (item == null) {
			label.setText("");
			return;
		}
		E e = extractor.apply(item);
		String s = converter.toString(e);
		label.setText(s);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Note that this won't call {@link #invalidate()}.
	 */
	@Override
	public void updateIndex(int index) {
		setIndex(index);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Note that this will automatically call {@link #invalidate()}.
	 */
	@Override
	public void updateItem(T item) {
		setItem(item);
		invalidate();
	}

	@Override
	public void updateColumn(VFXTableColumn<T, ? extends TableCell<T>> column) {
		setColumn(column);
	}

	@Override
	public void updateRow(VFXTableRow<T> row) {
		setRow(row);
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

	//================================================================================
	// Getters/Setters
	//================================================================================
	public VFXTableColumn<T, ? extends TableCell<T>> getColumn() {
		return column.get();
	}

	/**
	 * Specifies the instance of the column that created this cell.
	 */
	public ReadOnlyObjectProperty<VFXTableColumn<T, ? extends TableCell<T>>> columnProperty() {
		return column.getReadOnlyProperty();
	}

	protected void setColumn(VFXTableColumn<T, ? extends TableCell<T>> column) {
		this.column.set(column);
	}

	public VFXTableRow<T> getRow() {
		return row.get();
	}

	/**
	 * Specifies the instance of the row that contains this cell.
	 */
	public ReadOnlyObjectProperty<VFXTableRow<T>> rowProperty() {
		return row.getReadOnlyProperty();
	}

	protected void setRow(VFXTableRow<T> row) {
		this.row.set(row);
	}

	public int getIndex() {
		return index.get();
	}

	/**
	 * Specifies the index of the related column.
	 */
	public IntegerProperty indexProperty() {
		return index;
	}

	public void setIndex(int index) {
		this.index.set(index);
	}

	public T getItem() {
		return item.get();
	}

	/**
	 * Specifies the item from which the cell extracts and displays data of type {@link E}.
	 */
	public ObjectProperty<T> itemProperty() {
		return item;
	}

	public void setItem(T item) {
		this.item.set(item);
	}
}
