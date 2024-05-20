package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * Extension of {@link TableCell} to propose users a specific way of using {@link VFXTable}.
 * I structured this virtualized container with this API in mind. Each {@link VFXTableColumn} should produce cells
 * that display from an object {@link  T} that comes from the model, a piece of data {@link E} which is part of {@link T}.
 * Consider this example:
 * <pre>
 * {@code
 * // My model class...
 * record User(int id, String name) {}
 * // My columns (assuming UserCell is a custom cell)
 * VFXTableColumn<User, UserCell> idColumn = new VFXDefaultTableColumn<>("ID");
 * VFXTableColumn<User, UserCell> nameColumn = new VFXDefaultTableColumn<>("Name");
 * // Let's set their cell factories
 * idColumn.setCellFactory(user -> new UserCell(user, User::id));
 * nameColumn.setCellFactory(user -> new UserCell(user, User::name));
 * // See the second parameter in the constructors? I call that function the 'extractor' which basically tells the cell
 * // which piece of data it should display from the a User object
 * }
 * </pre>
 * For this API to work, we need two additional requirements:
 * <p> 1) Cells need to have an 'extractor' function
 * <p> 2) Cells may want to also have a 'converter' function. This is going to be used to convert extracted data of
 * type {@link E} to string to be used, for example, in a label. Of course, this is optional if the data is not going
 * to be displayed as a string
 *
 * @param <T> the data type in the table
 * @param <E> the data type the cell is going to use from items of type {@link T}
 */
public interface MappingTableCell<T, E> extends TableCell<T> {

	/**
	 * @return the function used to extract a value E from an item T
	 */
	Function<T, E> getExtractor();

	/**
	 * Sets the function used to extract a value E from an item T
	 */
	void setExtractor(Function<T, E> extractor);

	/**
	 * @return the {@link StringConverter} used to convert an extracted value E to a String
	 */
	StringConverter<E> getConverter();

	/**
	 * Sets the {@link StringConverter} used to convert an extracted value E to a String
	 */
	void setConverter(StringConverter<E> converter);

	/**
	 * Allows easily setting a {@link StringConverter} for the cell by just giving a {@link Function} as parameter,
	 * makes use of {@link FunctionalStringConverter#to(Function)}.
	 */
	default void setConverter(Function<E, String> converter) {
		setConverter(FunctionalStringConverter.to(converter));
	}

	/**
	 * This method should be used by cells to define the logic to update themselves when any of their properties change.
	 * Depends on the cell implementation.
	 */
	default void invalidate() {}
}
