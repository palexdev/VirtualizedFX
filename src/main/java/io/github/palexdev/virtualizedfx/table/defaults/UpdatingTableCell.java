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
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.function.Function;

/**
 * Extension of {@link SimpleTableCell} which is intended to be used with models that make use of JavaFX properties.
 * <p></p>
 * The extractor is expected to extract a property of type {@link ObservableValue}.
 * <p>
 * The {@link StringConverter} is intended to convert the value of the observable, {@link ObservableValue#getValue()},
 * to a String. By default, this returns "null" if the observable is null, otherwise uses {@link Objects#toString(Object)}
 * on the observable's value.
 * <p></p>
 * Also note that this cell keeps a reference to the extracted {@link ObservableValue} thus avoiding continuous calls
 * to the extractor. The reference is disposed by {@link #dispose()}
 * <p></p>
 * As this works on JavaFX's properties, the cells is capable of automatically updating itself when the property changes.
 *
 * @param <T> the type of items in the table
 * @param <E> the type of value wrapped in the {@link ObservableValue}
 */
public class UpdatingTableCell<T, E> extends SimpleTableCell<T, ObservableValue<E>> {
	//================================================================================
	// Properties
	//================================================================================
	private ObservableValue<E> property;
	protected When<E> updWhen;

	//================================================================================
	// Constructors
	//================================================================================
	public UpdatingTableCell(T item, Function<T, ObservableValue<E>> extractor) {
		this(item, extractor, FunctionalStringConverter.to(p ->
			(p != null) ? Objects.toString(p.getValue()) : "null")
		);
	}

	public UpdatingTableCell(T item, Function<T, ObservableValue<E>> extractor, StringConverter<ObservableValue<E>> converter) {
		super(item, extractor, converter);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Returns the {@link ObservableValue} extracted by the extractor function. If it is null
	 * it first attempts at extracting it, then stores the reference, so that subsequent calls
	 * will just return the already extracted one.
	 * <p></p>
	 * Also, when an observable is extracted a listener is attached to it to automatically call {@link #invalidate()}
	 * when its value changes, using {@link When#onInvalidated(ObservableValue)}.
	 */
	protected ObservableValue<E> getProperty() {
		if (property == null) {
			property = getExtractor().apply(getItem());
			updWhen = When.onInvalidated(property)
				.then(o -> invalidate())
				.executeNow()
				.listen();
		}
		return property;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * When the item is updated the {@link ObservableValue} changes too.
	 * First listener attached to the old one is disposed with {@link When#dispose()},
	 * then the local reference to the old one is set to null.
	 * <p>
	 * Afterwards the item is updated and {@link #invalidate()} called.
	 */
	@Override
	public void updateItem(T item) {
		if (ivwWhen != null) ivwWhen.dispose();
		ivwWhen = null;
		property = null;

		setItem(item);
		invalidate();
	}

	/**
	 * This is a way for cells to specify the behavior for when any of its properties are updated.
	 * <p></p>
	 * Gets the {@link ObservableValue} with {@link #getProperty()}, then the converter function converts is to
	 * a string and the label's text is updated.
	 */
	@Override
	public void invalidate() {
		String toString = getConverter().toString(getProperty());
		label.setText(toString);
	}

	/**
	 * Calls {@link When#dispose()} on the stored observable, {@link #getProperty()},
	 * then sets it to null.
	 */
	@Override
	public void dispose() {
		if (ivwWhen != null) ivwWhen.dispose();
		ivwWhen = null;
		property = null;
	}
}
