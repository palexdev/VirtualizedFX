package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.beans.range.NumberRange;
import io.github.palexdev.mfxcore.base.properties.PositionProperty;
import io.github.palexdev.mfxcore.base.properties.range.IntegerRangeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import java.util.Objects;
import java.util.Optional;

/**
 * This interface is a utility API for {@link VirtualizedList} which helps to avoid if checks that depend on the container's
 * orientation, {@link VirtualizedList#orientationProperty()}. There are two concrete implementations: {@link VerticalHelper}
 * and {@link HorizontalHelper}
 */
public interface VirtualizedListHelper<T, C extends Cell<T>> {

	/**
	 * @return the index of the first visible item
	 */
	int firstVisible();

	/**
	 * @return the index of the last visible item
	 */
	int lastVisible();

	/**
	 * @return the number of cells visible in the viewport. Not necessarily the same as {@link #totalNum()}
	 */
	int visibleNum();

	/**
	 * @return the total number of cells in the viewport which doesn't include only the number of visibles cells but also
	 * the number of buffer cells
	 */
	int totalNum();

	/**
	 * @return the range of items present in the list. This also takes into account buffer items, see {@link #visibleNum()}
	 * and {@link #totalNum()}
	 */
	IntegerRange range();

	/**
	 * @return the maximum amount of pixels the container can scroll on the vertical direction
	 */
	double maxVScroll();

	/**
	 * @return the maximum amount of pixels the container can scroll on the horizontal direction
	 */
	double maxHScroll();

	/**
	 * Specifies the virtual length of the viewport (total width/height).
	 */
	ReadOnlyDoubleProperty estimatedLengthProperty();

	/**
	 * Specifies the maximum breadth, opposed to the container's orientation.
	 * So: VERTICAL -> max width, HORIZONTAL -> max height.
	 * <p></p>
	 * This is dynamic, since the breadth of each node is computed only once it is laid out. This means that the absolute
	 * maximum breadth is only found when all items have been displayed at least once.
	 */
	ReadOnlyDoubleProperty maxBreadthProperty();

	/**
	 * Cells are actually contained in a separate pane called 'viewport'. The scroll is applied on this pane.
	 * <p>
	 * This property specifies the translation of the viewport, the calculation depends on the implementation.
	 */
	ReadOnlyObjectProperty<Position> viewportPositionProperty();

	/**
	 * Computes the breadth of the given node.
	 */
	double computeBreadth(Node node);

	/**
	 * Lays out the given node. The index parameter is necessary to identify the position of a cell compared to the others
	 * (comes before or after).
	 */
	void layout(int index, Node node);

	/**
	 * Scrolls in the viewport by the given number of pixels.
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
	 * @return the {@link VirtualizedList} instance associated to this helper
	 */
	VirtualizedList<T, C> getList();

	/**
	 * Forces the {@link VirtualizedList#vPosProperty()} and {@link VirtualizedList#hPosProperty()} to be invalidated.
	 * This is simply done by calling the respective setters with their current respective values. Those two properties
	 * will automatically call {@link #maxVScroll()} and {@link #maxHScroll()} to ensure the values are correct. This is
	 * automatically invoked by the {@link VirtualizedListManager} when needed.
	 */
	default void invalidatePos() {
		VirtualizedList<T, C> list = getList();
		list.setVPos(list.getVPos());
		list.setHPos(list.getHPos());
	}

	/**
	 * Converts the given index to a cell. Uses {@link #itemToCell(Object)}.
	 */
	default C indexToCell(int index) {
		T item = indexToItem(index);
		return itemToCell(item);
	}

	/**
	 * Converts the given item to a cell. The result is either on of the cells cached in {@link VirtualizedListCache} that
	 * is updated with the given item, or a totally new one created by the {@link VirtualizedList#cellFactoryProperty()}.
	 */
	default C itemToCell(T item) {
		VirtualizedListCache<T, C> cache = getList().getCache();
		Optional<C> opt = cache.tryTake();
		opt.ifPresent(c -> c.updateItem(item));
		return opt.orElseGet(() -> getList().getCellFactory().apply(item));
	}

	/**
	 * Converts the given index to an item (shortcut for {@code getList().getItems().get(index)}).
	 */
	default T indexToItem(int index) {
		return getList().getItems().get(index);
	}

	/**
	 * Implementing the {@link VirtualizedList#spacingProperty()} has been incredibly easy. It is enough to think at the
	 * spacing as an extension of the {@link VirtualizedList#cellSizeProperty()}. In other words, for the helper to still
	 * produce valid ranges, it is enough to sum the spacing to the cell size when the latter is needed.
	 * This is a shortcut for {@code getList().getCellSize() + getList().getSpacing()}.
	 */
	default double getTotalCellSize() {
		return getList().getCellSize() + getList().getSpacing();
	}

	/**
	 * Automatically called by {@link VirtualizedList} when a helper is not needed anymore (changed).
	 * If the helper uses listeners/bindings that may lead to memory leaks, this is the right place to remove them.
	 */
	default void dispose() {}

	/**
	 * Abstract implementation of {@link VirtualizedListHelper}, contains common members for the two concrete implementations
	 * {@link VerticalHelper} and {@link HorizontalHelper}, such as:
	 * <p> - the range of items to display as a {@link IntegerRangeProperty}
	 * <p> - the estimated length, which doesn't depend on the orientation but only on the number of items in the list
	 * and the total cell size ({@link #getTotalCellSize()}). The computation is: {@code itemsNum * totalCellSize - spacing},
	 * spacing for the last element needs to be removed.
	 * <p> - the maximum breadth, {@link #maxBreadthProperty()}
	 * <p> - the viewport's position, {@link #viewportPositionProperty()} as a {@link PositionProperty}
	 */
	abstract class AbstractHelper<T, C extends Cell<T>> implements VirtualizedListHelper<T, C> {
		protected final VirtualizedList<T, C> list;
		protected final IntegerRangeProperty range = new IntegerRangeProperty() {
			@Override
			public void set(NumberRange<Integer> newValue) {
				NumberRange<Integer> oldValue = get();
				if (Objects.equals(oldValue, newValue)) return;
				super.set(newValue);
			}
		};
		protected final ReadOnlyDoubleWrapper estimatedLength = new ReadOnlyDoubleWrapper();
		protected final ReadOnlyDoubleWrapper maxBreadth = new ReadOnlyDoubleWrapper();
		protected final PositionProperty viewportPosition = new PositionProperty();

		public AbstractHelper(VirtualizedList<T, C> list) {
			this.list = list;
			estimatedLength.bind(DoubleBindingBuilder.build()
				.setMapper(() -> (list.size() * getTotalCellSize() - list.getSpacing()))
				.addSources(list.sizeProperty(), list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
		}

		@Override
		public int totalNum() {
			int visible = visibleNum();
			return visible == 0 ? 0 : Math.min(visible + list.getBufferSize().val() * 2, list.size());
		}

		@Override
		public IntegerRange range() {
			return (IntegerRange) range.get();
		}

		@Override
		public ReadOnlyDoubleProperty estimatedLengthProperty() {
			return estimatedLength.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyDoubleProperty maxBreadthProperty() {
			return maxBreadth.getReadOnlyProperty();
		}

		@Override
		public ReadOnlyObjectProperty<Position> viewportPositionProperty() {
			return viewportPosition.getReadOnlyProperty();
		}

		@Override
		public VirtualizedList<T, C> getList() {
			return list;
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} for {@link Orientation#VERTICAL}. Here the range of items to
	 * display and the viewport position are defined as follows:
	 * <p> - the range is given by the {@link #firstVisible()} element minus the buffer size ({@link VirtualizedList#bufferSizeProperty()}),
	 * cannot be negative; and the sum between this start index and the total number of needed cells given by {@link #totalNum()}, cannot
	 * exceed the number of items - 1. It may happen the number of indexes given by the range {@code end - start + 1} is lesser
	 * than the total number of cells we need, in such cases, the range start is corrected to be {@code end - needed + 1}.
	 * A typical situation for this is when the list position reaches the max scroll.
	 * The range computation has the following dependencies: the list's height, the estimated length, the buffer size and
	 * the vertical position.
	 * <p> - the viewport position. This computation is at the core of virtual scrolling. The viewport, which contains the cell,
	 * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
	 * The horizontal position is just the current {@link VirtualizedList#hPosProperty()} but negative. The vertical
	 * position is the one virtualized.
	 * First we get the range of items to display and the total cell size given by {@link #getTotalCellSize()}, yes, the
	 * spacing also affects the position. Then we compute the range to the first visible cell, which is given by
	 * {@code IntegerRange.of(range.getMin(), firstVisible())}, in other words we limit the 'complete' range to the
	 * top buffer including the first cell after the buffer. The number of indexes in this newfound range, given by
	 * {@link IntegerRange#diff()} is multiplied by the total cell size, this way we found the number of pixels to the
	 * first visible cell, {@code pixelsToFirst}. We are missing only one last information, how much do we actually see
	 * of the first visible cell? We call this amount {@code visibleAmountFirst} and it's given by {@code vPos % totalCellSize}.
	 * Finally, the viewport's vertical position is given by {@code -(pixelsToFirst + visibleAmountFist}.
	 * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
	 * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the top
	 * or bottom anymore.
	 * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
	 * the cell size and the spacing
	 */
	class VerticalHelper<T, C extends Cell<T>> extends AbstractHelper<T, C> {

		public VerticalHelper(VirtualizedList<T, C> list) {
			super(list);
			range.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					int needed = totalNum();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstVisible() - list.getBufferSize().val());
					int end = Math.min(list.size() - 1, start + totalNum() - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(list.heightProperty(), estimatedLengthProperty())
				.addSources(list.bufferSizeProperty())
				.addSources(list.vPosProperty())
				.get()
			);
			viewportPosition.bind(ObjectBindingBuilder.<Position>build()
				.setMapper(() -> {
					if (list.isEmpty()) return Position.origin();
					IntegerRange range = range();
					if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

					double size = getTotalCellSize();
					IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
					double pixelsToFirst = rangeToFirstVisible.diff() * size;
					double visibleAmountFirst = list.getVPos() % size;

					double x = -NumberUtils.clamp(list.getHPos(), 0.0, maxHScroll());
					double y = -(pixelsToFirst + visibleAmountFirst);
					return Position.of(x, y);
				})
				.addSources(list.hPosProperty(), list.vPosProperty())
				.addSources(list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code vPos / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
				(int) Math.floor(list.getVPos() / getTotalCellSize()),
				0,
				list.size() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code (vPos + listHeight) / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int lastVisible() {
			return NumberUtils.clamp(
				(int) Math.floor((list.getVPos() + list.getHeight()) / getTotalCellSize()),
				0,
				list.size() - 1
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(listHeight / totalCellSize)}.
		 */
		@Override
		public int visibleNum() {
			return (int) Math.ceil(list.getHeight() / getTotalCellSize());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code estimatedLength - listHeight}, cannot be negative
		 */
		@Override
		public double maxVScroll() {
			return Math.max(0, estimatedLength.get() - list.getHeight());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code maxBreadth - listWidth}, cannot be negative
		 */
		@Override
		public double maxHScroll() {
			return Math.max(0, maxBreadth.get() - list.getWidth());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * If {@link VirtualizedList#fitToBreadthProperty()} is true, then the computation will always return the
		 * list's width, otherwise the node width is computed by {@link LayoutUtils#boundWidth(Node)}.
		 * Also, in the latter case, if the found width is greater than the current max breadth, then the property
		 * {@link #maxBreadthProperty()} is updated with the new value.
		 */
		@Override
		public double computeBreadth(Node node) {
			boolean fitToBreadth = list.isFitToBreadth();
			if (fitToBreadth) {
				double fW = list.getWidth();
				maxBreadth.set(fW);
				return fW;
			}
			double nW = LayoutUtils.boundWidth(node);
			if (nW > maxBreadth.get()) maxBreadth.set(nW);
			return nW;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The x position is 0. The y position is the total cell size multiplied bu the given index. The width is
		 * computed by {@link #computeBreadth(Node)}, and the height is given by the {@link VirtualizedList#cellSizeProperty()}.
		 */
		@Override
		public void layout(int index, Node node) {
			double y = getTotalCellSize() * index;
			double w = computeBreadth(node);
			double h = list.getCellSize();
			node.resizeRelocate(0, y, w, h);
		}

		@Override
		public void scrollBy(double pixels) {
			list.setVPos(list.getVPos() + pixels);
		}

		@Override
		public void scrollToPixel(double pixel) {
			list.setVPos(pixel);
		}

		@Override
		public void scrollToIndex(int index) {
			scrollToPixel(getTotalCellSize() * index);
		}
	}

	/**
	 * Concrete implementation of {@link AbstractHelper} for {@link Orientation#HORIZONTAL}. Here the range of items to
	 * display and the viewport position are defined as follows:
	 * <p> - the range is given by the {@link #firstVisible()} element minus the buffer size ({@link VirtualizedList#bufferSizeProperty()}),
	 * cannot be negative; and the sum between this start index and the total number of needed cells given by {@link #totalNum()}, cannot
	 * exceed the number of items - 1. It may happen the number of indexes given by the range {@code end - start + 1} is lesser
	 * than the total number of cells we need, in such cases, the range start is corrected to be {@code end - needed + 1}.
	 * A typical situation for this is when the list position reaches the max scroll.
	 * The range computation has the following dependencies: the list's width, the estimated length, the buffer size and
	 * the horizontal position.
	 * <p> - the viewport position. This computation is at the core of virtual scrolling. The viewport, which contains the cell,
	 * is not supposed to scroll by insane numbers of pixels both for performance reasons and because it is not necessary.
	 * The vertical position is just the current {@link VirtualizedList#vPosProperty()} but negative. The horizontal
	 * position is the one virtualized.
	 * First we get the range of items to display and the total cell size given by {@link #getTotalCellSize()}, yes, the
	 * spacing also affects the position. Then we compute the range to the first visible cell, which is given by
	 * {@code IntegerRange.of(range.getMin(), firstVisible())}, in other words we limit the 'complete' range to the
	 * left buffer including the first cell after the buffer. The number of indexes in this newfound range, given by
	 * {@link IntegerRange#diff()} is multiplied by the total cell size, this way we found the number of pixels to the
	 * first visible cell, {@code pixelsToFirst}. We are missing only one last information, how much do we actually see
	 * of the first visible cell? We call this amount {@code visibleAmountFirst} and it's given by {@code vPos % totalCellSize}.
	 * Finally, the viewport's vertical position is given by {@code -(pixelsToFirst + visibleAmountFist}.
	 * While it's true that the calculations are more complex and 'needy', it's important to note that this approach
	 * allows avoiding 'hacks' to correctly lay out the cells in the viewport. No need for special offsets at the left
	 * or right anymore.
	 * The viewport's position computation has the following dependencies: the horizontal position, the vertical position,
	 * the cell size and the spacing
	 */
	class HorizontalHelper<T, C extends Cell<T>> extends AbstractHelper<T, C> {

		public HorizontalHelper(VirtualizedList<T, C> list) {
			super(list);
			range.bind(ObjectBindingBuilder.<IntegerRange>build()
				.setMapper(() -> {
					int needed = totalNum();
					if (needed == 0) return Utils.INVALID_RANGE;

					int start = Math.max(0, firstVisible() - list.getBufferSize().val());
					int end = Math.min(list.size() - 1, start + totalNum() - 1);
					if (end - start + 1 < needed) start = Math.max(0, end - needed + 1);
					return IntegerRange.of(start, end);
				})
				.addSources(list.widthProperty(), estimatedLengthProperty())
				.addSources(list.bufferSizeProperty())
				.addSources(list.hPosProperty())
				.get()
			);
			viewportPosition.bind(ObjectBindingBuilder.<Position>build()
				.setMapper(() -> {
					if (list.isEmpty()) return Position.origin();
					IntegerRange range = range();
					if (Utils.INVALID_RANGE.equals(range)) return Position.origin();

					double size = getTotalCellSize();
					IntegerRange rangeToFirstVisible = IntegerRange.of(range.getMin(), firstVisible());
					double pixelsToFirst = rangeToFirstVisible.diff() * size;
					double visibleAmountFirst = list.getHPos() % size;

					double x = -NumberUtils.clamp(list.getVPos(), 0.0, maxVScroll());
					double y = -(pixelsToFirst + visibleAmountFirst);
					return Position.of(x, y);
				})
				.addSources(list.hPosProperty(), list.vPosProperty())
				.addSources(list.cellSizeProperty(), list.spacingProperty())
				.get()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code hPos / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int firstVisible() {
			return NumberUtils.clamp(
				(int) Math.floor(list.getHPos() / getTotalCellSize()),
				0,
				list.size()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code (hPos + listWidth) / totalCellSize}, clamped between 0 and itemsNum - 1.
		 */
		@Override
		public int lastVisible() {
			return NumberUtils.clamp(
				(int) Math.floor((list.getHPos() + list.getWidth()) / getTotalCellSize()),
				0,
				list.size()
			);
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code Math.ceil(listWidth / totalCellSize)}.
		 */
		@Override
		public int visibleNum() {
			return (int) Math.ceil(list.getWidth() / getTotalCellSize());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code maxBreadth - listHeight}, cannot be negative
		 */
		@Override
		public double maxVScroll() {
			return Math.max(0, maxBreadth.get() - list.getHeight());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * Given by {@code estimatedLength - listWidth}, cannot be negative
		 */
		@Override
		public double maxHScroll() {
			return Math.max(0, estimatedLength.get() - list.getWidth());
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * If {@link VirtualizedList#fitToBreadthProperty()} is true, then the computation will always return the
		 * list's height, otherwise the node width is computed by {@link LayoutUtils#boundHeight(Node)}.
		 * Also, in the latter case, if the found height is greater than the current max breadth, then the property
		 * {@link #maxBreadthProperty()} is updated with the new value.
		 */
		@Override
		public double computeBreadth(Node node) {
			boolean fitToBreadth = list.isFitToBreadth();
			if (fitToBreadth) {
				double fH = list.getHeight();
				maxBreadth.set(fH);
				return fH;
			}
			double nH = LayoutUtils.boundHeight(node);
			if (nH > maxBreadth.get()) maxBreadth.set(nH);
			return nH;
		}

		/**
		 * {@inheritDoc}
		 * <p></p>
		 * The y position is 0. The x position is the total cell size multiplied bu the given index. The height is
		 * computed by {@link #computeBreadth(Node)}, and the width is given by the {@link VirtualizedList#cellSizeProperty()}.
		 */
		@Override
		public void layout(int index, Node node) {
			double x = getTotalCellSize() * index;
			double w = list.getCellSize();
			double h = computeBreadth(node);
			node.resizeRelocate(x, 0, w, h);
		}

		@Override
		public void scrollBy(double pixels) {
			list.setHPos(list.getHPos() + pixels);
		}

		@Override
		public void scrollToPixel(double pixel) {
			list.setHPos(pixel);
		}

		@Override
		public void scrollToIndex(int index) {
			scrollToPixel(getTotalCellSize() * index);
		}
	}
}
