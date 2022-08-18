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
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;

/**
 * An {@code InvalidationAction} is a convenience bean which implements {@link DisposableAction}
 * used by {@link BehaviorBase} to register an {@link InvalidationListener} on a certain {@link ObservableValue}
 * and dispose it once it's not needed anymore.
 */
public class InvalidationAction<T> implements DisposableAction {
	//================================================================================
	// Properties
	//================================================================================
	private ObservableValue<T> observable;
	private InvalidationListener listener;

	//================================================================================
	// Constructors
	//================================================================================
	public InvalidationAction(ObservableValue<T> observable, InvalidationListener listener) {
		this.observable = observable;
		this.listener = listener;
	}

	//================================================================================
	// Static Methods
	//================================================================================

	/**
	 * Equivalent to {@link #InvalidationAction(ObservableValue, InvalidationListener)} but this also
	 * adds the listener to the observable already.
	 */
	public static <T> InvalidationAction<T> of(ObservableValue<T> observable, InvalidationListener listener) {
		InvalidationAction<T> ia = new InvalidationAction<>(observable, listener);
		observable.addListener(listener);
		return ia;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Removes the {@link InvalidationListener} from the observable then
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

	public InvalidationListener getListener() {
		return listener;
	}
}
