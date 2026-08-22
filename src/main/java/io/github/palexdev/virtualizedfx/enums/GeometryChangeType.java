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

package io.github.palexdev.virtualizedfx.enums;

/// Tells a virtualized container's manager what kind of geometry change it is reacting to.
///
/// A geometry change generally means recomputing the ranges and producing a new state, but the two dimensions do not
/// invalidate the same things, so knowing which one moved lets a manager skip work that cannot have been affected, or
/// do extra work that only that dimension requires.
public enum GeometryChangeType {
    /// The container's width changed.
    WIDTH,
    /// The container's height changed.
    HEIGHT,
    /// Neither dimension changed, but a geometry pass is needed anyway. A buffer size change is the typical case: it
    /// alters how many cells the viewport needs without altering its size.
    OTHER,
    ;
}
