package io.github.palexdev.virtualizedfx.properties;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.grid.VFXGridState;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Convenience property that extends {@link ReadOnlyObjectWrapper} for {@link VFXGridState}.
 */
public class VFXGridStateProperty<T, C extends VFXCell<T>> extends ReadOnlyObjectWrapper<VFXGridState<T, C>> {

	//================================================================================
	// Constructors
	//================================================================================
	public VFXGridStateProperty() {}

	public VFXGridStateProperty(VFXGridState<T, C> initialValue) {
		super(initialValue);
	}

	public VFXGridStateProperty(Object bean, String name) {
		super(bean, name);
	}

	public VFXGridStateProperty(Object bean, String name, VFXGridState<T, C> initialValue) {
		super(bean, name, initialValue);
	}
}
