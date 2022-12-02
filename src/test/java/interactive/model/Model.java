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

package interactive.model;

import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import javafx.collections.ObservableList;

import java.util.stream.IntStream;

public class Model {
	public static final ObservableList<Integer> integers;
	public static final ObservableList<String> strings;
	public static final ObservableList<User> users;

	static {
		integers = IntStream.rangeClosed(0, 200)
				.boxed()
				.collect(FXCollectors.toList());
		strings = IntStream.rangeClosed(0, 200)
				.mapToObj(i -> "String " + i)
				.collect(FXCollectors.toList());
		users = User.randObsList(200);
	}

	private Model() {
	}
}
