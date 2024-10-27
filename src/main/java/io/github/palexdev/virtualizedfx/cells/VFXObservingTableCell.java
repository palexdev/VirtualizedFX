/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.cells;

import java.beans.EventHandler;
import java.util.Objects;
import java.util.function.Function;

import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Extension of {@link VFXSimpleTableCell} which is intended to be used with models that make use of JavaFX properties.
 * Uses {@link VFXObservingTableCellSkin} as the default skin implementation.
 * <p>
 * The extractor function is expected to extract a property of type {@link ObservableValue}.
 * <p>
 * The {@link StringConverter} is intended to convert the value of the observable, {@link ObservableValue#getValue()},
 * to a {@code String}. By default, this returns an empty string if the observable is {@code null},
 * otherwise uses {@link Objects#toString(Object)} on its value.
 * <p></p>
 * Also note that this cell keeps a reference to the extracted {@link ObservableValue} thus avoiding continuous calls
 * to the extractor. The reference is disposed by {@link #dispose()}
 */
public class VFXObservingTableCell<T, E> extends VFXSimpleTableCell<T, ObservableValue<E>> {
	//================================================================================
	// Properties
	//================================================================================
	protected ObservableValue<E> property;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXObservingTableCell(T item, Function<T, ObservableValue<E>> extractor) {
		this(item, extractor, FunctionalStringConverter.to(p ->
			(p != null) ? Objects.toString(p.getValue()) : "")
		);
	}

	public VFXObservingTableCell(T item, Function<T, ObservableValue<E>> extractor, StringConverter<ObservableValue<E>> converter) {
		super(item, extractor, converter);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Returns the {@link ObservableValue} extracted by the extractor function.
	 * <p>
	 * If the local reference is still {@code null} it first attempts at extracting it, and then updates it
	 * (so that later calls will just return the already extracted observable).
	 */
	protected ObservableValue<E> getProperty() {
		if (property == null) property = getExtractor().apply(getItem());
		return property;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Before updating the item this disposes the {@link ObservableValue} previously extracted by {@link #getProperty()}.
	 */
	@Override
	public void updateItem(T item) {
		property = null;
		super.updateItem(item);
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXObservingTableCellSkin(this);
	}

	@Override
	public void dispose() {
		property = null;
		super.dispose();
	}

	//================================================================================
	// Internal Classes
	//================================================================================

	/**
	 * Default skin implementation used by {@link VFXObservingTableCell} and extension of {@link VFXLabeledCellSkin}.
	 * <p>
	 * I could have overridden the base class in-line, but I decided to make it a separate class to make it easier
	 * to customize.
	 * <p></p>
	 * The {@link #update()} method is overridden and functions quite differently, make sure to carefully read the documentation.
	 * The {@link #addListeners()} method has also been modified to call {@link #onItemChanged()} rather than {@link #update()}
	 * directly when the {@link VFXCellBase#itemProperty()} changes.
	 */
	public class VFXObservingTableCellSkin extends VFXLabeledCellSkin<T> {
		//================================================================================
		// Properties
		//================================================================================
		protected When<E> listener;

		//================================================================================
		// Constructors
		//================================================================================
		public VFXObservingTableCellSkin(VFXCellBase<T> cell) {
			super(cell);
		}

		//================================================================================
		// Methods
		//================================================================================

		/**
		 * The auto-updating feature of this cell implementation is no magic. A simple {@link InvalidationListener}
		 * is attached to the extracted property to call {@link #update()} when it changes.
		 * <p>
		 * This method does exactly this. When the {@link VFXCellBase#itemProperty()} changes it also means that the property
		 * extracted by {@link #getProperty()} is now invalid, and so is the previous listener.
		 * <p>
		 * Simply re-builds the listener when this happens, and by invoking {@link #getProperty()} we also validate the
		 * local reference to the property (to be precise the reference is stored in the cell not here in the skin).
		 * Note that this will also call {@link #update()}.
		 */
		protected void onItemChanged() {
			if (listener != null) listener.dispose();
			listener = onInvalidated(getProperty())
				.then(p -> update())
				.executeNow()
				.listen();
		}

		//================================================================================
		// Overridden Methods
		//================================================================================

		/**
		 * Adds an {@link InvalidationListener} on the {@link VFXCellBase#itemProperty()} to call {@link #onItemChanged()} when it changes,
		 * and an {@link EventHandler} to support "manual" updates through events of type {@link VFXContainerEvent#UPDATE}
		 * (although this should not be needed here).
		 * <p>
		 * (Uses {@link When} and {@link WhenEvent} constructs).
		 *
		 * @see #listeners(When[])
		 * @see #events(WhenEvent[])
		 */
		@Override
		protected void addListeners() {
			listeners(
				onInvalidated(itemProperty())
					.then(t -> onItemChanged())
					.executeNow()
			);
		}

		/**
		 * Updates the label's text by using the converter on the property extracted by {@link #getProperty()}.
		 *
		 * @see #getConverter()
		 */
		@Override
		protected void update() {
			String toString = getConverter().toString(getProperty());
			label.setText(toString);
		}

		@Override
		public void dispose() {
			if (listener != null) listener.dispose();
			listener = null;
			super.dispose();
		}
	}
}
