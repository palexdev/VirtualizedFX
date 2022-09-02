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

package io.github.palexdev.virtualizedfx.beans;

/**
 * Useful bean to work with virtualized control's bounds.
 * This allows to keep both the real sizes of the control and the
 * virtual ones.
 */
public class VirtualBounds {
	//================================================================================
	// Static Properties
	//================================================================================
	public static VirtualBounds EMPTY = new VirtualBounds();

	//================================================================================
	// Properties
	//================================================================================
	private final double width;
	private final double height;
	private final double virtualWidth;
	private final double virtualHeight;

	//================================================================================
	// Constructors
	//================================================================================
	protected VirtualBounds() {
		this.width = 0;
		this.height = 0;
		this.virtualWidth = 0;
		this.virtualHeight = 0;
	}

	public VirtualBounds(double width, double height, double virtualWidth, double virtualHeight) {
		this.width = width;
		this.height = height;
		this.virtualWidth = virtualWidth;
		this.virtualHeight = virtualHeight;
	}

	public static VirtualBounds of(double width, double height, double virtualWidth, double virtualHeight) {
		return new VirtualBounds(width, height, virtualWidth, virtualHeight);
	}

	//================================================================================
	// Methods
	//================================================================================
	public boolean isEmpty() {
		return this == EMPTY ||
				(width == 0 && virtualWidth == 0 &&
						height == 0 && virtualHeight == 0);
	}

	@Override
	public String toString() {
		return "VirtualBounds{" +
				"width=" + width +
				", height=" + height +
				", virtualWidth=" + virtualWidth +
				", virtualHeight=" + virtualHeight +
				'}';
	}

	//================================================================================
	// Getters
	//================================================================================

	/**
	 * @return the real width
	 */
	public double getWidth() {
		return width;
	}

	/**
	 * @return the real height
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * @return the virtual width
	 */
	public double getVirtualWidth() {
		return virtualWidth;
	}

	/**
	 * @return the virtual height
	 */
	public double getVirtualHeight() {
		return virtualHeight;
	}
}
