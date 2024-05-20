package io.github.palexdev.virtualizedfx.properties;

import io.github.palexdev.virtualizedfx.table.VFXTableState;
import javafx.beans.property.ReadOnlyObjectWrapper;

/**
 * Convenience property that extends {@link ReadOnlyObjectWrapper} for {@link VFXTableState}.
 */
public class VFXTableStateProperty<T> extends ReadOnlyObjectWrapper<VFXTableState<T>> {

	//================================================================================
	// Constructors
	//================================================================================
	public VFXTableStateProperty() {}

	public VFXTableStateProperty(VFXTableState<T> initialValue) {
		super(initialValue);
	}

	public VFXTableStateProperty(Object bean, String name) {
		super(bean, name);
	}

	public VFXTableStateProperty(Object bean, String name, VFXTableState<T> initialValue) {
		super(bean, name, initialValue);
	}
}
