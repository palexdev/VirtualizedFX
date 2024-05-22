package io.github.palexdev.virtualizedfx.list.paginated;

import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.cells.base.Cell;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.list.VFXListManager;
import io.github.palexdev.virtualizedfx.list.VFXListSkin;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Orientation;
import javafx.scene.control.SkinBase;

/**
 * Default skin implementation for the paginated variant of {@link VFXList}: {@link VFXPaginatedList}.
 * Extends {@link VFXListSkin} and expects behaviors of type {@link VFXPaginatedListManager}.
 * <p>
 * There's not much going on here, just the bare minimum to get the paginated variant work as intended.
 * <p>
 * First of all, the list position is bound to the current page specified by {@link VFXPaginatedList#pageProperty()}.
 * Only the position that is the same as the current orientation is bound. When the orientation changes, the binding is
 * swapped by {@link #swapPositionBinding()}. Also, there are a couple of extra listeners, {@link #addListeners()}.
 * <p></p>
 * As for the layout, the only thing that changes is that the container's size will adapt to the cell size and the number
 * of cells per page, the exact computation is described and done by {@link #getLength()}.
 */
public class VFXPaginatedListSkin<T, C extends Cell<T>> extends VFXListSkin<T, C> {
	//================================================================================
	// Properties
	//================================================================================
	protected DoubleBinding posBinding;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXPaginatedListSkin(VFXPaginatedList<T, C> list) {
		super(list);

		// Init pos binding
		posBinding = DoubleBindingBuilder.build()
			.setMapper(() -> list.getPage() * list.getCellsPerPage() * (list.getCellSize() + list.getSpacing()))
			.addSources(list.pageProperty(), list.cellsPerPageProperty(), list.cellSizeProperty(), list.spacingProperty())
			.get();
		swapPositionBinding();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Responsible for swapping the position's binding when the orientation changes. Since the base skin uses a similar
	 * mechanism for the scroll, this is called in the {@link #swapPositionListener()} method.
	 */
	protected void swapPositionBinding() {
		if (posBinding == null) return;
		VFXList<T, C> list = getSkinnable();
		Orientation o = list.getOrientation();
		if (o == Orientation.VERTICAL) {
			list.hPosProperty().unbind();
			list.vPosProperty().bind(posBinding);
		} else {
			list.vPosProperty().unbind();
			list.hPosProperty().bind(posBinding);
		}
	}

	/**
	 * Computes the length the container should have, according to the following three properties:
	 * <p> - {@link VFXPaginatedList#cellsPerPageProperty()}
	 * <p> - {@link VFXPaginatedList#cellSizeProperty()}
	 * <p> - {@link VFXPaginatedList#spacingProperty()}
	 * <p>
	 * The formula is as follows: {@code (cellsPerPage * (cellSize + spacing)) - spacing}.
	 * <p></p>
	 * The result is enforced by the 'compute min/pref/max width/height' methods defined by {@link SkinBase} and overridden here.
	 * Note that only the methods relative to the current orientation (VERTICAL -> height / HORIZONTAL -> width) will use
	 * the resulting value. Which means that the size in the opposite direction can be changed as preferred.
	 */
	protected final double getLength() {
		VFXPaginatedList<T, C> list = getList();
		return (list.getCellsPerPage() * (list.getCellSize() + list.getSpacing())) - list.getSpacing();
	}

	/**
	 * Convenience method to cast {@link #getSkinnable()} to {@link VFXPaginatedList}.
	 */
	protected VFXPaginatedList<T, C> getList() {
		return (VFXPaginatedList<T, C>) getSkinnable();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * For the paginated variant there are the following additional listeners:
	 * <p> - Listener on {@link VFXPaginatedList#cellsPerPageProperty()}, will invoke {@link VFXPaginatedListManager#onCellsPerPageChanged()}
	 * <p> - Listener on {@link VFXPaginatedList#maxPageProperty()}, will invoke {@link VFXPaginatedListManager#onMaxPageChanged()}
	 */
	@Override
	protected void addListeners() {
		VFXPaginatedList<T, C> list = getList();
		super.addListeners();
		listeners(
			When.onInvalidated(list.cellsPerPageProperty())
				.then(cpp -> getBehavior().onCellsPerPageChanged()),
			When.onInvalidated(list.maxPageProperty())
				.then(mp -> getBehavior().onMaxPageChanged())
		);
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Overridden to also call {@link #swapPositionBinding()}.
	 */
	@Override
	protected void swapPositionListener() {
		super.swapPositionListener();
		swapPositionBinding();
	}

	/**
	 * Overridden to cast the behavior to {@link VFXPaginatedListManager}.
	 * <p>
	 * Since {@link VFXPaginatedList} extends {@link VFXList} nothing prevents the user from using behaviors of type
	 * {@link VFXListManager}, but that would result in exceptions being thrown and invalid states.
	 * Long story short: don't do it!
	 */
	@Override
	protected VFXPaginatedListManager<T, C> getBehavior() {
		return (VFXPaginatedListManager<T, C>) super.getBehavior();
	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.HORIZONTAL) return getLength();
		return super.computeMaxWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		Orientation o = getSkinnable().getOrientation();
		if (o == Orientation.VERTICAL) return getLength();
		return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
	}
}
