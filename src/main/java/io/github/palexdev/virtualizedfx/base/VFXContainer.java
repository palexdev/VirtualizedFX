package io.github.palexdev.virtualizedfx.base;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

/**
 * Defines the common API for every virtualized container offered by VirtualizedFX.
 */
public interface VFXContainer<T> {

	default ObservableList<T> getItems() {
		return itemsProperty().get();
	}

	/**
	 * Specifies the {@link ObservableList} used to store the items.
	 * <p>
	 * We use a {@link ListProperty} because it offers many commodities such as both the size and emptiness of the list
	 * as observable properties, as well as the possibility of adding an {@link InvalidationListener} that will both inform
	 * about changes of the property and in the list.
	 */
	ListProperty<T> itemsProperty();

	default void setItems(ObservableList<T> items) {
		itemsProperty().set(items);
	}

	default int size() {
		return sizeProperty().get();
	}

	/**
	 * Specifies the number of items in the data structure.
	 */
	default ReadOnlyIntegerProperty sizeProperty() {
		return itemsProperty().sizeProperty();
	}

	default boolean isEmpty() {
		return emptyProperty().get();
	}

	/**
	 * Specifies whether the data set is empty.
	 */
	default ReadOnlyBooleanProperty emptyProperty() {
		return itemsProperty().emptyProperty();
	}

	default double getVirtualMaxX() {
		return virtualMaxXProperty().get();
	}

	ReadOnlyDoubleProperty virtualMaxXProperty();

	default double getVirtualMaxY() {
		return virtualMaxYProperty().get();
	}

	ReadOnlyDoubleProperty virtualMaxYProperty();

	default double getVPos() {
		return vPosProperty().get();
	}

	/**
	 * Specifies the container's vertical position.
	 */
	DoubleProperty vPosProperty();

	default void setVPos(double vPos) {
		vPosProperty().set(vPos);
	}

	default double getHPos() {
		return hPosProperty().get();
	}

	/**
	 * Specifies the container's horizontal position.
	 */
	DoubleProperty hPosProperty();

	default void setHPos(double hPos) {
		hPosProperty().set(hPos);
	}

	default BufferSize getBufferSize() {
		return bufferSizeProperty().get();
	}

	/**
	 * Specifies the number of extra cells to add to the container; they act as a buffer, allowing scroll to be smoother.
	 * To avoid edge cases due to the users abusing the system, this is done by using an enumerator which allows up to
	 * three buffer cells.
	 */
	StyleableObjectProperty<BufferSize> bufferSizeProperty();

	default void setBufferSize(BufferSize bufferSize) {
		bufferSizeProperty().set(bufferSize);
	}
}
