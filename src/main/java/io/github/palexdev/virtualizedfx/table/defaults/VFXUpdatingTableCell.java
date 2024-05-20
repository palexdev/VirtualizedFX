package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.converters.FunctionalStringConverter;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import java.util.Objects;
import java.util.function.Function;

// TODO move to cells package? (also make base sub-package)

/**
 * Extension of {@link VFXSimpleTableCell} which is intended to be used with models that make use of JavaFX properties.
 * <p>
 * The extractor function is expected to extract a property of type {@link ObservableValue}.
 * <p>
 * The {@link StringConverter} is intended to convert the value of the observable, {@link ObservableValue#getValue()},
 * to a String. By default, this returns {@code null} if the observable is {@code null},
 * otherwise uses {@link Objects#toString(Object)} on its value.
 * <p></p>
 * Also note that this cell keeps a reference to the extracted {@link ObservableValue} thus avoiding continuous calls
 * to the extractor. The reference is disposed by {@link #dispose()}
 * <p>
 * Because this cell type relies on JavaFX's properties, it is capable of automatically updating itself when the property changes.
 * In other words {@link #invalidate()} is automatically called.
 */
public class VFXUpdatingTableCell<T, E> extends VFXSimpleTableCell<T, ObservableValue<E>> {
	//================================================================================
	// Properties
	//================================================================================
	private ObservableValue<E> property;
	private When<E> updateWhen;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXUpdatingTableCell(T item, Function<T, ObservableValue<E>> extractor) {
		this(item, extractor, FunctionalStringConverter.to(p ->
			(p != null) ? Objects.toString(p.getValue()) : "null")
		);
	}

	public VFXUpdatingTableCell(T item, Function<T, ObservableValue<E>> extractor, StringConverter<ObservableValue<E>> converter) {
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
	 * <p></p>
	 * Also, when an observable is extracted a listener is attached to it to automatically call {@link #invalidate()}
	 * when its value changes, using {@link When#onInvalidated(ObservableValue)}.
	 */
	protected ObservableValue<E> getProperty() {
		if (property == null) {
			property = getExtractor().apply(getItem());
			updateWhen = When.onInvalidated(property)
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
	 * Before updating the item and proceeding with {@link #invalidate()}, this disposes the {@link ObservableValue}
	 * previously extracted by {@link #getProperty()} (also disposes the listener).
	 */
	@Override
	public void updateItem(T item) {
		if (property != null) {
			updateWhen.dispose();
			updateWhen = null;
			property = null;
		}
		setItem(item);
		invalidate();
	}


	/**
	 * Gets the {@link ObservableValue} with {@link #getProperty()}, then the converter function converts is to
	 * a string and the label's text is updated.
	 */
	@Override
	public void invalidate() {
		String toString = getConverter().toString(getProperty());
		label.setText(toString);
	}

	/**
	 * Disposes the listener attached to the extracted {@link ObservableValue} (see {@link #getProperty()}),
	 * then its the local reference to {@code null}.
	 */
	@Override
	public void dispose() {
		if (updateWhen != null) updateWhen.dispose();
		property = null;
		updateWhen = null;
		super.dispose();
	}
}
