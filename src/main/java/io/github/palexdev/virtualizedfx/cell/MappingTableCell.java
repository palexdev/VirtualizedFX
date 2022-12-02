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

package io.github.palexdev.virtualizedfx.cell;

import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * Whilst {@link TableCell} defines the basic API for cells used by {@link VirtualTable},
 * this is probably the most useful type of cell.
 * <p></p>
 * This extension specifies additional APIs to extract a certain value E from the table's items
 * of type T (each column represents a certain value E of a given item T), as well as a way to
 * convert the extracted values E to a string.
 *
 * @param <T> the type of items in the table
 * @param <E> the type of value extracted by the cell from an item of type T
 */
public interface MappingTableCell<T, E> extends TableCell<T> {

	/**
	 * @return the function used to extract a property E from an item T
	 */
	Function<T, E> getExtractor();

	/**
	 * Sets the function used to extract a property E from an item T
	 */
	void setExtractor(Function<T, E> extractor);

	/**
	 * @return the {@link StringConverter} used to convert an extracted value E to a string
	 */
	StringConverter<E> getConverter();

	/**
	 * Sets the {@link StringConverter} used to convert an extracted value E to a string
	 */
	void setConverter(StringConverter<E> converter);

	/**
	 * Allows to easily set a {@link StringConverter} for the cell by just
	 * giving a {@link Function} as parameter, makes use of {@link FunctionalStringConverter#to(Function)}.
	 */
	default void setConverter(Function<E, String> converter) {
		setConverter(FunctionalStringConverter.to(converter));
	}
}
