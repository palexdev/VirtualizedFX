package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.builders.bindings.IntegerBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXPaginated;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListHelper;
import io.github.palexdev.virtualizedfx.list.VFXListManager;
import io.github.palexdev.virtualizedfx.list.VFXListState;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedListHelper.HorizontalHelper;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedListHelper.VerticalHelper;
import io.github.palexdev.virtualizedfx.utils.IndexBiMap.StateMap;
import io.github.palexdev.virtualizedfx.utils.Utils;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Simple and naive implementation of a paginated variant of {@link VFXList}.
 * The default style class is extended to: '.vfx-list.paginated'.
 * <p>
 * Extends {@link VFXList}, implements {@link VFXPaginated}, has its own skin {@link VFXPaginatedListSkin} and behavior
 * {@link VFXPaginatedListSkin}.
 * <p>
 * A: What do you mean by naive? <p>
 * Q: Since this extends {@link VFXList}, it uses its infrastructure as much as possible. After all, the only major
 * difference between the two is that this can't scroll freely because the position depends on the page index.
 * Although there is <b>a lot</b> going on in the background (range computation, viewport translation, spacing, etc...)
 * this solution works surprisingly well. Sure, there are some caveats and special cases to handle, but there is no
 * performance degradation, and this is a huge win.
 * <p>
 * For example, let's suppose the container is at the last page, and there aren't enough items to fill the page. You may
 * think that the {@link VFXListState} would contain only the items/cells that are "contained" by the page
 * (plus the buffer)... wrong. Because of how the system works by default, the number of cells will always be the sum
 * of the cells per page and the buffer (x2 top and bottom buffers). So, for example, if the last page can show only 5 elements
 * out of 10, the state will contain 14 cells anyway. The viewport translation mechanism described here
 * {@link VFXListHelper#viewportPositionProperty()} (check implementations for a more detailed explanation) ensures
 * that what we see is actually those 5 needed cells. Whether this is a waste in terms of performance is debatable.
 * While it's true that the paginated variant could avoid working on those unnecessary extra cells, we should also consider
 * that they act as a cache. In fact, if we go to the previous page, there won't be as many updates as you may expect,
 * because some of the cells from the previous state are already good to go.
 * <p>
 * Because of this, there are a couple of extra methods that give you information strictly on the visible cells.
 * See {@link #getVisibleRange()}, {@link #getVisibleCellsByIndex()} and {@link #getVisibleCellsByItem()}.
 * <p></p>
 * This variant is intended to use implementations of {@link VFXPaginatedListHelper}. Nothing prevents you from setting a
 * {@link #helperFactoryProperty()} that produces helpers of type {@link VFXListHelper}, don't do that!
 * You may end up with invalid states, thus a broken component.
 */
public class VFXPaginatedList<T, C extends VFXCell<T>> extends VFXList<T, C> implements VFXPaginated<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final IntegerProperty page = PropUtils.clampedIntProperty(
		() -> 0,
		this::getMaxPage
	);
	private final ReadOnlyIntegerWrapper maxPage = new ReadOnlyIntegerWrapper();

	//================================================================================
	// Constructors
	//================================================================================
	public VFXPaginatedList() {
		super();
		initialize();
	}

	public VFXPaginatedList(ObservableList<T> items, Function<T, C> cellFactory) {
		super(items, cellFactory);
		initialize();
	}

	public VFXPaginatedList(ObservableList<T> items, Function<T, C> cellFactory, Orientation orientation) {
		super(items, cellFactory, orientation);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		maxPage.bind(IntegerBindingBuilder.build()
			.setMapper(this::computeMaxPage)
			.addSources(sizeProperty(), cellsPerPageProperty())
			.get()
		);
	}

	/**
	 * Computes the range of visible items for the current page. The computation is done by using the
	 * {@link VFXPaginatedListHelper}. The range doesn't include the buffer cells!
	 */
	public IntegerRange getVisibleRange() {
		if (isEmpty()) return Utils.INVALID_RANGE;
		VFXListHelper<T, C> helper = getHelper();
		int first = helper.firstVisible();
		int last = Math.min(first + getCellsPerPage() - 1, size() - 1);
		return IntegerRange.of(first, last);
	}

	/**
	 * By using the {@link IntegerRange} computed by {@link #getVisibleRange()}, filters the {@link StateMap}
	 * (from the current state {@link #stateProperty()}), and returns a map of the visible cells by their index.
	 */
	public Map<Integer, C> getVisibleCellsByIndex() {
		IntegerRange range = getVisibleRange();
		if (Utils.INVALID_RANGE.equals(range)) return Map.of();
		return getCellsByIndexUnmodifiable().entrySet().stream()
			.filter(e -> IntegerRange.inRangeOf(e.getKey(), range))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * By using the {@link IntegerRange} computed by {@link #getVisibleRange()}, filters the {@link StateMap}
	 * (from the current state {@link #stateProperty()}), and returns a map of the visible cells by the displayed item.
	 */
	public Map<T, C> getVisibleCellsByItem() {
		IntegerRange range = getVisibleRange();
		if (Utils.INVALID_RANGE.equals(range)) return Map.of();
		Map<T, C> map = new HashMap<>();
		SequencedMap<Integer, C> byIndex = getCellsByIndexUnmodifiable();
		for (Integer i : range) {
			T t = getItems().get(i);
			map.put(t, byIndex.get(i));
		}
		return map;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-list", "paginated");
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Overridden to use implementations of {@link VFXPaginatedListHelper} instead.
	 */
	@Override
	protected Function<Orientation, VFXListHelper<T, C>> defaultHelperFactory() {
		return o -> (o == Orientation.VERTICAL) ?
			new VerticalHelper<>(this) :
			new HorizontalHelper<>(this);
	}

	@Override
	public Supplier<VFXListManager<T, C>> defaultBehaviorProvider() {
		return () -> new VFXPaginatedListManager<>(this);
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXPaginatedListSkin<>(this);
	}

	/**
	 * Since for the paginated variant the position is bound to the {@link #pageProperty()}, this setter won't do
	 * anything if the current orientation is vertical. You could still use the {@link #vPosProperty()} to set the position,
	 * but that would generate an exception. Unbinding the property would result in invalid states, so, don't do it.
	 */
	@Override
	public void setVPos(double vPos) {
		if (vPosProperty().isBound()) return;
		super.setVPos(vPos);
	}

	/**
	 * Since for the paginated variant the position is bound to the {@link #pageProperty()}, this setter won't do
	 * anything if the current orientation is horizontal. You could still use the {@link #hPosProperty()} to set the position,
	 * but that would generate an exception. Unbinding the property would result in invalid states, so, don't do it.
	 */
	@Override
	public void setHPos(double hPos) {
		if (hPosProperty().isBound()) return;
		super.setHPos(hPos);
	}

	/**
	 * Shortcut for {@code setPage(0)}.
	 */
	@Override
	public void scrollToFirst() {
		setPage(0);
	}

	/**
	 * Shortcut for {@code setPage(getMaxPage())}.
	 */
	@Override
	public void scrollToLast() {
		setPage(getMaxPage());
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableIntegerProperty cellsPerPage = new StyleableIntegerProperty(
		StyleableProperties.CELLS_PER_PAGE,
		this,
		"cellsPerPage",
		10
	);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Can be set in CSS via the property: '-vfx-cells-per-page'.
	 */
	@Override
	public StyleableIntegerProperty cellsPerPageProperty() {
		return cellsPerPage;
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VFXPaginatedList<?, ?>> FACTORY = new StyleablePropertyFactory<>(VFXList.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VFXPaginatedList<?, ?>, Number> CELLS_PER_PAGE =
			FACTORY.createSizeCssMetaData(
				"-vfx-cells-per-page",
				VFXPaginatedList::cellsPerPageProperty,
				10
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				VFXList.getClassCssMetaData(),
				CELLS_PER_PAGE
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

	@Override
	public IntegerProperty pageProperty() {
		return page;
	}

	@Override
	public ReadOnlyIntegerProperty maxPageProperty() {
		return maxPage.getReadOnlyProperty();
	}
}
