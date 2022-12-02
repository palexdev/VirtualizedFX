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

import io.github.palexdev.mfxcore.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class Utils {

	private Utils() {
	}

	public static Integer[][] randMatrix(int rows, int columns) {
		Integer[][] matrix = new Integer[rows][columns];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				matrix[i][j] = RandomUtils.random.nextInt(100);
			}
		}
		return matrix;
	}

	// TODO replace everywhere
	public static <T> List<T> listGetAll(List<T> in, Integer... indexes) {
		List<T> l = new ArrayList<>();
		for (int index : indexes) {
			try {
				l.add(in.get(index));
			} catch (Exception ignored) {
			}
		}
		return l;
	}
}
