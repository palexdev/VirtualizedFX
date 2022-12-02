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

package interactive.others;

import java.util.List;
import java.util.function.Function;

public record Constraint<T>(String message, Function<T, Boolean> validator) {

	public static <T> Constraint<T> of(String message, Function<T, Boolean> validator) {
		return new Constraint<>(message, validator);
	}

	public static Constraint<Integer> listIndexConstraint(List<?> list, boolean isAdd) {
		return new Constraint<>(
				"Invalid Index",
				i -> i >= 0 && i < list.size() + ((isAdd) ? 1 : 0)
		);
	}

	public boolean isValid(T val) {
		return validator.apply(val);
	}
}
