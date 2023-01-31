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

package app.cells.flow;

public class GlobalCellParameters {
	private static double animationDuration;
	private static boolean animEnabled;

	private GlobalCellParameters() {
	}

	public static double getAnimationDuration() {
		return animationDuration;
	}

	public static void setAnimationDuration(double animationDuration) {
		GlobalCellParameters.animationDuration = animationDuration;
	}

	public static boolean isAnimEnabled() {
		return animEnabled;
	}

	public static void setAnimEnabled(boolean animEnabled) {
		GlobalCellParameters.animEnabled = animEnabled;
	}
}
