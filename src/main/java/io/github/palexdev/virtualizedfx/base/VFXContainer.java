package io.github.palexdev.virtualizedfx.base;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;

/**
 * Defines the common API for every virtualized container offered by VirtualizedFX.
 */
public interface VFXContainer {
	// TODO maybe more methods can be added here, do it once all containers are implemented
	int size();

	/**
	 * Specifies the number of items in the data structure.
	 */
	ReadOnlyIntegerProperty sizeProperty();

	boolean isEmpty();

	/**
	 * Specifies whether the data set is empty.
	 */
	ReadOnlyBooleanProperty emptyProperty();
}
