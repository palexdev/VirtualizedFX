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

/**
 * Enumeration to set the buffer size of virtualized containers. To avoid the user abusing the 'dynamic' buffer system, by
 * setting unreasonably high numbers or 0, an enumeration is used instead.
 * <p>
 * The size of the buffer is given by the constant's ordinal + 1, {@link #val()}.
 */
public enum BufferSize {
    SMALL,
    MEDIUM,
    BIG,
    ;

    /**
     * @return the constant's {@link #ordinal()} + 1
     */
    public int val() {
        return ordinal() + 1;
    }

    /**
     * @return the standard, recommended buffer size, which is {@link #MEDIUM} (2)
     */
    public static BufferSize standard() {
        return MEDIUM;
    }
}
