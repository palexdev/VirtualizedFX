package io.github.palexdev.virtualizedfx.base;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.virtualizedfx.enums.BufferSize;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

import java.util.List;

/**
 * Defines the common API for every virtualized container offered by VirtualizedFX.
 */
public interface VFXContainer<T> {

	/**
	 * @return a list containing all the component's default style classes
	 */
	List<String> defaultStyleClasses();

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
	ReadOnlyIntegerProperty sizeProperty();

	default boolean isEmpty() {
		return emptyProperty().get();
	}

	/**
	 * Specifies whether the data set is empty.
	 */
	ReadOnlyBooleanProperty emptyProperty();

	/**
	 * Delegate for {@link VFXListHelper#getVirtualMaxX()}
	 */
	default double getVirtualMaxX() {
		return virtualMaxXProperty().get();
	}

	/**
	 * Delegate for {@link VFXListHelper#virtualMaxXProperty()}.
	 */
	ReadOnlyDoubleProperty virtualMaxXProperty();

	/**
	 * Delegate for {@link VFXListHelper#getVirtualMaxY()}
	 */
	default double getVirtualMaxY() {
		return virtualMaxYProperty().get();
	}

	/**
	 * Delegate for {@link VFXListHelper#virtualMaxYProperty()}.
	 */
	ReadOnlyDoubleProperty virtualMaxYProperty();

	default double getVPos() {
		return vPosProperty().get();
	}

	/**
	 * Specifies the container's vertical position. In case the orientation is set to {@link Orientation#VERTICAL}, this
	 * is to be considered a 'virtual' position, as the container will never reach unreasonably high values for performance
	 * reasons. See {@link VFXListHelper.VerticalHelper} to understand how virtual scroll is handled.
	 */
	DoubleProperty vPosProperty();

	default void setVPos(double vPos) {
		vPosProperty().set(vPos);
	}

	default double getHPos() {
		return hPosProperty().get();
	}

	/**
	 * Specifies the container's horizontal position. In case the orientation is set to {@link Orientation#HORIZONTAL}, this
	 * is to be considered a 'virtual' position, as the container will never reach unreasonably high values for performance
	 * reasons. See {@link VFXListHelper.HorizontalHelper} to understand how virtual scroll is handled.
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
	 * three buffer cells. Also, the default implementation (see {@link VFXListHelper.VerticalHelper} or {@link VFXListHelper.HorizontalHelper}), adds
	 * double the number specified by the enum constant, because these buffer cells are added both at the top and at the
	 * bottom of the container. The default value is {@link BufferSize#MEDIUM}.
	 */
	StyleableObjectProperty<BufferSize> bufferSizeProperty();

	default void setBufferSize(BufferSize bufferSize) {
		bufferSizeProperty().set(bufferSize);
	}
}
