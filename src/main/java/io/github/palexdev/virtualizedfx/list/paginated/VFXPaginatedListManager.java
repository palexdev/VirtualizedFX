package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListManager;
import javafx.scene.Parent;

/**
 * Default behavior implementation for {@link VFXPaginatedList}, extends {@link VFXListManager}.
 * <p>
 * This is necessary to respond to the following property changes introduced by the paginated variant:
 * <p> - cells per page changes, {@link #onCellsPerPageChanged()}
 * <p> - max page changes, {@link #onMaxPageChanged()}
 * <p></p>
 * It also modifies some of the behaviors already defined in {@link VFXListManager} because the paginated variant should
 * respond differently in some cases, and also to optimize performance as much as possible.
 */
public class VFXPaginatedListManager<T, C extends VFXCell<T>> extends VFXListManager<T, C> {

	//================================================================================
	// Constructors
	//================================================================================
	public VFXPaginatedListManager(VFXPaginatedList<T, C> list) {
		super(list);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * A paginated container's size strictly depends on how many cells/rows/items is set to display per page, and this
	 * is enforced by the default skin {@link VFXPaginatedListSkin}, see {@link VFXPaginatedListSkin#getLength()}.
	 * <p>
	 * This core method is called whenever the {@link VFXPaginatedList#cellsPerPageProperty()} changes and ensures that
	 * the layout bounds of the container become invalid ({@link Parent#isNeedsLayout()} becomes 'true'), by calling
	 * {@link Parent#requestLayout()}.
	 * <p>
	 * This way, computations that also rely on the container size become invalid too, thus leading to correct values.
	 */
	protected void onCellsPerPageChanged() {
		VFXList<T, C> list = getNode();
		list.requestLayout();
	}

	/**
	 * This core method ensures that the paginated container is always at a valid page/position when the
	 * {@link VFXPaginatedList#maxPageProperty()} changes.
	 * <p>
	 * The only one case this needs to correct the position is when the current page is greater than the new max page.
	 * (both in terms of indexes ofc).
	 * <p>
	 * <b>Important note:</b> the page change would trigger a position change (so {@link #onPositionChanged()}), but this
	 * method avoids it by setting the {@link #invalidatingPos} flag to 'true' before and immediately re-setting it to false
	 * after. This may sound counterintuitive, but there is a reason.
	 * <p>
	 * You see, the max page property can change on two occasions:
	 * <p> 1) the number of items changes
	 * <p> 2) the cells per page changes
	 * <p>
	 * <b>BUT...</b> in the first case, we have a change in the list which is handled by {@link #onItemsChanged()};
	 * and the second case is handled by {@link #onGeometryChanged()}. This last case is very peculiar, because it will work
	 * only if the container's skin is implemented to adapt to the number of items per page. It's the skin's implementation
	 * to trigger the geometry change, and this should be the intended default behavior.
	 * <p>
	 * If you want to make a skin that doesn't follow this logic, then you probably want to change this method too.
	 */
	protected void onMaxPageChanged() {
		VFXPaginatedList<T, C> list = getNode();
		int page = list.getPage();
		int max = list.getMaxPage();
		if (page > max) {
			invalidatingPos = true;
			list.moveBy(0);
			invalidatingPos = false;
		}
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Overridden to just call {@link VFXList#requestViewportLayout()}.
	 * <p>
	 * For the paginated variant, the cells' size is irrelevant for its state. The default behavior, also dependent on
	 * the skin, is to adapt the container's size to the size of each cell as well as the number of cells per page.
	 */
	@Override
	protected void onCellSizeChanged() {
		VFXList<T, C> list = getNode();
		list.requestViewportLayout();
	}

	/**
	 * As also described in the super method ({@link VFXListManager#onOrientationChanged()}), when the orientation changes
	 * the most reasonable behavior is to reset both the positions to 0.0. For the paginated variant this requires extra
	 * steps because, according to the previous orientation, the vPos or hPos properties are bound. Also, the reset also
	 * requires setting the page to 0.
	 */
	@Override
	protected void onOrientationChanged() {
		VFXPaginatedList<T, C> list = getNode();
		list.vPosProperty().unbind();
		list.hPosProperty().unbind();
		list.setPage(0);
		super.onOrientationChanged();
	}

	/**
	 * Overridden to cast to {@link VFXPaginatedList} since this behavior only allows that type.
	 */
	@Override
	public VFXPaginatedList<T, C> getNode() {
		return (VFXPaginatedList<T, C>) super.getNode();
	}
}
