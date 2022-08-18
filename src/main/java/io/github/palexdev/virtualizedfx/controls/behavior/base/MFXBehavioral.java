/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of MaterialFX (https://github.com/palexdev/MaterialFX).
 *
 * MaterialFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MaterialFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.controls.behavior.base;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

/**
 * Public API for all controls that rely on a {@link BehaviorBase} to specify
 * their behavior, also allowing to change it with custom implementations,
 *
 * @param <N> the type of Node
 * @param <B> the type of behavior, implementation of {@link BehaviorBase}
 */
public interface MFXBehavioral<N extends Node, B extends BehaviorBase<N>> {
	/**
	 * @return the default behavior object for the Node
	 */
	B defaultBehavior();

	B getBehavior();

	/**
	 * The property specifying the Node's behavior.
	 */
	ObjectProperty<B> behaviorProperty();

	void setBehavior(B behavior);
}
