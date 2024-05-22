package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.cells.base.MappingTableCell;
import io.github.palexdev.virtualizedfx.cells.base.TableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableRow;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Extension of {@link CellBase} which also implements {@link MappingTableCell}. Uses an inline extension of {@link VFXLabeledCellSkin}
 * as the default skin (see below why). This is intended to be used with models that do not use JavaFX's properties.
 * Expands the default style classes to be: ".cell-base" and ".table-cell".
 * <p>
 * There are four new properties:
 * <p> 1) The {@link #columnProperty()} holds the column that created the cell.
 * <p> 2) The {@link #rowProperty()} holds the row containing the cell.
 * <p> 3) The {@link #getExtractor()} is the function used to extract a piece of data {@link E} from an item of type {@link T}.
 * <b>Note</b> that the extractor function should take into account {@code null} inputs!
 * <p> 4) The {@link #getConverter()} is a helper class which converts data of type {@link E} to a {@code String}.
 * The method {@link #buildSkin()} will build an inline extension of {@link VFXLabeledCellSkin} to override its
 * {@link VFXLabeledCellSkin#update()} method and make use of the converter.
 */
public class VFXSimpleTableCell<T, E> extends CellBase<T> implements MappingTableCell<T, E> {
	//================================================================================
	// Properties
	//================================================================================
	private final ReadOnlyObjectWrapper<VFXTableColumn<T, ? extends TableCell<T>>> column = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<VFXTableRow<T>> row = new ReadOnlyObjectWrapper<>();
	private Function<T, E> extractor;
	private StringConverter<E> converter;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXSimpleTableCell(T item, Function<T, E> extractor) {
		this(item, extractor, FunctionalStringConverter.to(t -> (t == null) ? "" : t.toString()));
	}

	public VFXSimpleTableCell(T item, Function<T, E> extractor, StringConverter<E> converter) {
		super(item);
		this.extractor = extractor;
		this.converter = converter;
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
	protected SkinBase<?, ?> buildSkin() {
		return new VFXLabeledCellSkin<>(this) {
			@Override
			protected void update() {
				T item = getItem();
				E e = extractor.apply(item);
				String s = converter.toString(e);
				label.setText(s);
			}
		};
	}

	@Override
	public List<String> defaultStyleClasses() {
		return List.of("cell-base", "table-cell");
	}

	@Override
	public void updateColumn(VFXTableColumn<T, ? extends TableCell<T>> column) {
		setColumn(column);
	}

	@Override
	public void updateRow(VFXTableRow<T> row) {
		setRow(row);
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


	@Override
	public Function<T, E> getExtractor() {
		return extractor;
	}

	@Override
	public VFXSimpleTableCell<T, E> setExtractor(Function<T, E> extractor) {
		this.extractor = extractor;
		return this;
	}

	@Override
	public StringConverter<E> getConverter() {
		return converter;
	}

	@Override
	public VFXSimpleTableCell<T, E> setConverter(StringConverter<E> converter) {
		this.converter = converter;
		return this;
	}

}
