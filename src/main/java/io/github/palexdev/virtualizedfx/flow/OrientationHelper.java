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

package io.github.palexdev.virtualizedfx.flow;

import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;

/**
 * The {@code OrientationHelper} is a utility interface with two concrete implementations {@link HorizontalHelper} and
 * {@link VerticalHelper}, both are used by the {@link VirtualFlow} and its subcomponents, depending on its
 * {@link VirtualFlow#orientationProperty()}, to separate common computations/operations that depend on the
 * orientation.
 */
public interface OrientationHelper {

	/**
	 * @return the index of the first visible item
	 */
	int firstVisible();

	/**
	 * @return the index of the last visible item
	 */
	int lastVisible();

	/**
	 * @return the maximum number of cells the viewport can display, this also
	 * includes one cell of overscan/buffer
	 */
	int maxCells();

	/**
	 * @return the maximum amount of pixels the virtual flow can scroll on the vertical direction
	 */
	double maxVScroll();

	/**
	 * @return the maximum amount of pixels the virtual flow can scroll on the horizontal direction
	 */
	double maxHScroll();

	/**
	 * @return the virtual length of the viewport, the {@link VirtualFlow#cellSizeProperty()} multiplied
	 * by the number of items in the list
	 */
	double computeEstimatedLength();

	/**
	 * Holds the result of {@link #computeEstimatedLength()}.
	 */
	ReadOnlyDoubleProperty estimatedLengthProperty();

	/**
	 * Responsible for computing the breadth of the given node.
	 */
	double computeBreadth(Node node);

	/**
	 * Specifies the maximum length, opposed to the virtual flow orientation, among the displayed cells.
	 * So: VERTICAL -> max width, HORIZONTAL -> max height
	 */
	ReadOnlyDoubleProperty maxBreadthProperty();

	/**
	 * This binding holds the horizontal position of the viewport.
	 */
	DoubleBinding xPosBinding();

	/**
	 * This binding holds the vertical position of the viewport.
	 */
	DoubleBinding yPosBinding();

	/**
	 * Invalidates both {@link VirtualFlow#hPosProperty()} and {@link VirtualFlow#vPosProperty()} in case changes
	 * occurred in the viewport and the current position are no longer valid.
	 */
	void invalidatePos();

	/**
	 * Scrolls in the viewport by the given amount of pixels.
	 */
	void scrollBy(double pixels);

	/**
	 * Scrolls in the viewport to the given pixel value.
	 */
	void scrollToPixel(double pixel);

	/**
	 * Scrolls in the viewport to the given item's index.
	 */
	void scrollToIndex(int index);

	/**
	 * Responsible for calling {@link Node#resizeRelocate(double, double, double, double)} with the needed parameters,
	 * this is used to position and resize cells.
	 */
	void layout(Node node, double pos, double breadth);

	/**
	 * Disposes bindings/listeners that are not required anymore, for example
	 * when changing orientation and helper as well, the old is disposed.
	 */
	void dispose();

	/**
	 * Abstract implementation of {@link OrientationHelper}, base class for both {@link HorizontalHelper} and
	 * {@link VerticalHelper} to extract common properties.
	 */
	abstract class AbstractHelper implements OrientationHelper {
		protected final VirtualFlow<?, ?> virtualFlow;
		protected final ViewportManager<?, ?> viewportManager;

		protected final DoubleProperty estimatedLength = new SimpleDoubleProperty();
		protected final DoubleProperty maxBreadth = new SimpleDoubleProperty();

		public AbstractHelper(VirtualFlow<?, ?> virtualFlow, ViewportManager<?, ?> viewportManager) {
			this.virtualFlow = virtualFlow;
			this.viewportManager = viewportManager;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} for managing the virtual flow when its orientation
	 * is HORIZONTAL.
	 * <p></p>
	 * This helper adds the following listeners:
	 * <p> - A listener to the virtual flow's width to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener to the virtual flow's height to re-layout, {@link VirtualFlow#requestViewportLayout()}
	 * <p> - A listener on the virtual flow's hPos property to process the scroll, {@link ViewportManager#onScroll()}
	 */
	class HorizontalHelper extends AbstractHelper {
		private ChangeListener<? super Number> widthListener;
		private InvalidationListener heightListener;
		private InvalidationListener hPosListener;

		public HorizontalHelper(VirtualFlow<?, ?> virtualFlow, ViewportManager<?, ?> viewportManager) {
			super(virtualFlow, viewportManager);

			widthListener = (observable, oldValue, newValue) -> {
				if (newValue.doubleValue() > 0) viewportManager.init();
			};
			heightListener = invalidated -> virtualFlow.requestViewportLayout();
			hPosListener = invalidated -> viewportManager.onScroll();

			virtualFlow.widthProperty().addListener(widthListener);
			virtualFlow.heightProperty().addListener(heightListener);
			virtualFlow.hPosProperty().addListener(hPosListener);

			((DoubleProperty) virtualFlow.estimatedLengthProperty()).bind(estimatedLengthProperty());
			((DoubleProperty) virtualFlow.maxBreadthProperty()).bind(maxBreadthProperty());
		}

		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
					(int) Math.floor(virtualFlow.getHPos() / virtualFlow.getCellSize()),
					0,
					virtualFlow.getItems().size() - 1
			);
		}

		@Override
		public int lastVisible() {
			return NumberUtils.clamp(firstVisible() + maxCells() - 1, 0, virtualFlow.getItems().size() - 1);
		}

		@Override
		public int maxCells() {
			return (int) (Math.ceil(virtualFlow.getWidth() / virtualFlow.getCellSize()) + 1);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde maxBreadth - virtualFlow.getHeight()}
		 */
		@Override
		public double maxVScroll() {
			return maxBreadthProperty().get() - virtualFlow.getHeight();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@code estimatedLength - virtualFlow.getWidth()}
		 */
		@Override
		public double maxHScroll() {
			return estimatedLengthProperty().get() - virtualFlow.getWidth();
		}

		@Override
		public double computeEstimatedLength() {
			double val = virtualFlow.getItems().size() * virtualFlow.getCellSize();
			estimatedLength.set(val);
			return val;
		}

		@Override
		public ReadOnlyDoubleProperty estimatedLengthProperty() {
			return estimatedLength;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The {@code HorizontalHelper} computes the values depending on {@link VirtualFlow#fitToBreadthProperty()}:
		 * <p> - True: {@code virtualFlow.getHeight()}
		 * <p> - False: {@link LayoutUtils#boundHeight(Node)}
		 */
		@Override
		public double computeBreadth(Node node) {
			boolean fitToBreadth = virtualFlow.isFitToBreadth();
			return fitToBreadth ? virtualFlow.getHeight() : LayoutUtils.boundHeight(node);
		}

		@Override
		public ReadOnlyDoubleProperty maxBreadthProperty() {
			return maxBreadth;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In the {@code HorizontalHelper} this is the direction along the estimated length. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualFlow#hPosProperty()}
		 * and {@link VirtualFlow#cellSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell size.
		 * <p>
		 * This is the formula: {@code -virtualFlow.getHPos() % virtualFlow.getCellSize()}.
		 * <p>
		 * Think about this. We have cells of size 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the cell for 4px, but once we scroll again it makes to sense to go to 75px because the first cell won't be
		 * visible anymore, so we go back at 11px. Now the top cell will be visible for 53px. Keep in mind that cells
		 * are always positioned from the end to 0 (exceptions documented here {@link ViewportState#computePositions()}).
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the cells are just repositioned from the end. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding xPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -virtualFlow.getHPos() % virtualFlow.getCellSize(),
					virtualFlow.hPosProperty(), virtualFlow.cellSizeProperty()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In the {@code HorizontalHelper} this is the direction along the max breadth, this depends on
		 * {@link VirtualFlow#vPosProperty()}, {@link VirtualFlow#heightProperty()} and {@link #maxBreadthProperty()}.
		 * <p>
		 * This is computed as: {@code -NumberUtils.clamp(virtualFlow.getVPos(), 0.0, maxBreadth.get() - virtualFlow.getHeight())}
		 */
		@Override
		public DoubleBinding yPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -NumberUtils.clamp(virtualFlow.getVPos(), 0.0, maxBreadth.get() - virtualFlow.getHeight()),
					virtualFlow.vPosProperty(), virtualFlow.heightProperty(), maxBreadth
			);
		}

		@Override
		public void invalidatePos() {
			double length = estimatedLength.get();
			double breadth = maxBreadth.get();
			virtualFlow.setVPos(Math.min(virtualFlow.getVPos(), length));
			virtualFlow.setHPos(Math.min(virtualFlow.getHPos(), breadth));
		}

		@Override
		public void scrollBy(double pixels) {
			double newVal = NumberUtils.clamp(virtualFlow.getHPos() + pixels, 0, maxHScroll());
			virtualFlow.setHPos(newVal);
		}

		@Override
		public void scrollToPixel(double pixel) {
			double clampedVal = NumberUtils.clamp(pixel, 0, maxHScroll());
			virtualFlow.setHPos(clampedVal);
		}

		@Override
		public void scrollToIndex(int index) {
			double val = index * virtualFlow.getCellSize();
			double clampedVal = NumberUtils.clamp(val, 0, maxHScroll());
			virtualFlow.setHPos(clampedVal);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param node    the node to layout
		 * @param pos     the x position
		 * @param breadth the node's height
		 */
		@Override
		public void layout(Node node, double pos, double breadth) {
			double size = virtualFlow.getCellSize();
			node.resizeRelocate(pos, 0, size, breadth);
		}

		@Override
		public void dispose() {
			virtualFlow.widthProperty().removeListener(widthListener);
			virtualFlow.heightProperty().removeListener(heightListener);
			virtualFlow.hPosProperty().removeListener(hPosListener);

			widthListener = null;
			heightListener = null;
			hPosListener = null;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} for managing the virtual flow when its orientation
	 * is VERTICAL.
	 * <p></p>
	 * This helper adds the following listeners:
	 * <p> - A listener to the virtual flow's height to re-initialize the viewport, {@link ViewportManager#init()}
	 * <p> - A listener to the virtual flow's width to re-layout, {@link VirtualFlow#requestViewportLayout()}
	 * <p> - A listener on the virtual flow's vPos property to process the scroll, {@link ViewportManager#onScroll()}
	 */
	class VerticalHelper extends AbstractHelper {
		private ChangeListener<? super Number> heightListener;
		private InvalidationListener widthListener;
		private InvalidationListener vPosListener;

		public VerticalHelper(VirtualFlow<?, ?> virtualFlow, ViewportManager<?, ?> viewportManager) {
			super(virtualFlow, viewportManager);

			heightListener = (observable, oldValue, newValue) -> {
				if (newValue.doubleValue() > 0) viewportManager.init();
			};
			widthListener = invalidated -> virtualFlow.requestViewportLayout();
			vPosListener = invalidated -> viewportManager.onScroll();

			virtualFlow.heightProperty().addListener(heightListener);
			virtualFlow.widthProperty().addListener(widthListener);
			virtualFlow.vPosProperty().addListener(vPosListener);

			((DoubleProperty) virtualFlow.estimatedLengthProperty()).bind(estimatedLengthProperty());
			((DoubleProperty) virtualFlow.maxBreadthProperty()).bind(maxBreadthProperty());
		}

		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
					(int) Math.floor(virtualFlow.getVPos() / virtualFlow.getCellSize()),
					0,
					virtualFlow.getItems().size() - 1
			);
		}

		@Override
		public int lastVisible() {
			return NumberUtils.clamp(firstVisible() + maxCells() - 1, 0, virtualFlow.getItems().size() - 1);
		}

		@Override
		public int maxCells() {
			return (int) (Math.ceil(virtualFlow.getHeight() / virtualFlow.getCellSize()) + 1);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@code estimatedLength - virtualFlow.getHeight()}
		 */
		@Override
		public double maxVScroll() {
			return estimatedLengthProperty().get() - virtualFlow.getHeight();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The value is given by: {@cde maxBreadth - virtualFlow.getWidth()}
		 */
		@Override
		public double maxHScroll() {
			return maxBreadthProperty().get() - virtualFlow.getWidth();
		}

		@Override
		public double computeEstimatedLength() {
			double val = virtualFlow.getItems().size() * virtualFlow.getCellSize();
			estimatedLength.set(val);
			return val;
		}

		@Override
		public ReadOnlyDoubleProperty estimatedLengthProperty() {
			return estimatedLength;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The {@code VerticalHelper} computes the values depending on {@link VirtualFlow#fitToBreadthProperty()}:
		 * <p> - True: {@code virtualFlow.getWidth()}
		 * <p> - False: {@link LayoutUtils#boundWidth(Node)}
		 */
		@Override
		public double computeBreadth(Node node) {
			boolean fitToBreadth = virtualFlow.isFitToBreadth();
			return fitToBreadth ? virtualFlow.getWidth() : LayoutUtils.boundWidth(node);
		}

		@Override
		public ReadOnlyDoubleProperty maxBreadthProperty() {
			return maxBreadth;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In the {@code VerticalHelper} this is the direction along the max breadth, this depends on
		 * {@link VirtualFlow#hPosProperty()}, {@link VirtualFlow#widthProperty()} and {@link #maxBreadthProperty()}
		 * <p>
		 * This is computed as: {@code -NumberUtils.clamp(virtualFlow.getHPos(), 0.0, maxBreadth.get() - virtualFlow.getWidth())}
		 */
		@Override
		public DoubleBinding xPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -NumberUtils.clamp(virtualFlow.getHPos(), 0.0, maxBreadth.get() - virtualFlow.getWidth()),
					virtualFlow.hPosProperty(), virtualFlow.widthProperty(), maxBreadth
			);
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * In the {@code VerticalHelper} this is the direction along the estimated length. However, the implementation
		 * makes it so that the position of the viewport is virtual. This binding which depends on both {@link VirtualFlow#vPosProperty()}
		 * and {@link VirtualFlow#cellSizeProperty()} will always return a value that is greater or equal to 0 and lesser
		 * than the cell size.
		 * <p>
		 * This is the formula: {@code -virtualFlow.getVPos() % virtualFlow.getCellSize()}.
		 * <p>
		 * Think about this. We have cells of size 64. and we scroll 15px on each gesture. When we reach 60px, we can still
		 * see the cell for 4px, but once we scroll again it makes to sense to go to 75px because the first cell won't be
		 * visible anymore, so we go back at 11px. Now the top cell will be visible for 53px. Keep in mind that cells
		 * are always positioned from the end to 0 (exceptions documented here {@link ViewportState#computePositions()}).
		 * <p>
		 * Long story short, scrolling is just an illusion, the viewport just scroll by a little to give this illusion and
		 * when needed the cells are just repositioned from the end. This is important because the estimated length
		 * could, in theory, reach very high values, so we don't want the viewport to scroll by thousands of pixels.
		 */
		@Override
		public DoubleBinding yPosBinding() {
			return Bindings.createDoubleBinding(
					() -> -virtualFlow.getVPos() % virtualFlow.getCellSize(),
					virtualFlow.vPosProperty(), virtualFlow.cellSizeProperty()
			);
		}

		@Override
		public void invalidatePos() {
			double length = estimatedLength.get();
			double breadth = maxBreadth.get();
			virtualFlow.setVPos(Math.min(virtualFlow.getVPos(), length));
			virtualFlow.setHPos(Math.min(virtualFlow.getHPos(), breadth));
		}

		@Override
		public void scrollBy(double pixels) {
			double newVal = NumberUtils.clamp(virtualFlow.getVPos() + pixels, 0, maxVScroll());
			virtualFlow.setVPos(newVal);
		}

		@Override
		public void scrollToPixel(double pixel) {
			double clampedVal = NumberUtils.clamp(pixel, 0, maxVScroll());
			virtualFlow.setVPos(clampedVal);
		}

		@Override
		public void scrollToIndex(int index) {
			double val = index * virtualFlow.getCellSize();
			double clampedVal = NumberUtils.clamp(val, 0, maxVScroll());
			virtualFlow.setVPos(clampedVal);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param node    the node to layout
		 * @param pos     the y position
		 * @param breadth the node's width
		 */
		@Override
		public void layout(Node node, double pos, double breadth) {
			double size = virtualFlow.getCellSize();
			node.resizeRelocate(0, pos, breadth, size);
		}

		@Override
		public void dispose() {
			virtualFlow.heightProperty().removeListener(heightListener);
			virtualFlow.widthProperty().removeListener(widthListener);
			virtualFlow.vPosProperty().removeListener(vPosListener);

			heightListener = null;
			widthListener = null;
			vPosListener = null;
		}
	}
}
