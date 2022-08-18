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

package io.github.palexdev.virtualizedfx.controls.behavior.actions;

import io.github.palexdev.virtualizedfx.controls.behavior.base.BehaviorBase;
import io.github.palexdev.virtualizedfx.controls.behavior.base.DisposableAction;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * A {@code ChangeAction} is a convenience bean which implements {@link DisposableAction}
 * used by {@link BehaviorBase} to register a {@link ChangeListener} on a certain {@link ObservableValue}
 * and dispose it once it's not needed anymore.
 */
public class ChangeAction<T> implements DisposableAction {
	//================================================================================
	// Properties
	//================================================================================
	private ObservableValue<T> observable;
	private ChangeListener<T> listener;

	//================================================================================
	// Constructors
	//================================================================================
	public ChangeAction(ObservableValue<T> observable, ChangeListener<T> listener) {
		this.observable = observable;
		this.listener = listener;
	}

	//================================================================================
	// Static Methods
	//================================================================================

	/**
	 * Equivalent to {@link #ChangeAction(ObservableValue, ChangeListener)} but this also
	 * adds the listener to the observable already.
	 */
	public static <T> ChangeAction<T> of(ObservableValue<T> observable, ChangeListener<T> listener) {
		ChangeAction<T> ca = new ChangeAction<>(observable, listener);
		observable.addListener(listener);
		return ca;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Removes the {@link ChangeListener} from the observable then
	 * sets both the fields to null.
	 */
	@Override
	public void dispose() {
		observable.removeListener(listener);
		listener = null;
		observable = null;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public ObservableValue<T> getObservable() {
		return observable;
	}

	public ChangeListener<T> getListener() {
		return listener;
	}
}
