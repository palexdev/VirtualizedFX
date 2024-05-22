package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.list.VFXList;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;

/**
 * Simple cache implementation for virtualized containers that produce cells of type {@link  VFXCell}.
 * Cells are stored in a {@link CellsQueue}, a special extension of {@link LinkedList}.
 * The cache can be limited in the maximum number of cells to keep by setting its capacity, see {@link VFXList#cacheCapacityProperty()}.
 * Cells won't be added if the capacity is reached and will be disposed immediately instead.
 * <p>
 * Aside from the typical functions of a cache (take/store), you are also allowed to 'populate' it at will, see {@link #populate()}.
 * <p>
 * <b>Note</b> however that to generate cells, the cache must know the function to produce them. The function must be
 * the same used by the container, therefore, it's set by the container itself.
 * <p>
 * This is mostly useful to 'pre-populate' it, filling the cache before the container is even laid out,
 * so that at init cells are already built and just need to be updated.
 * <p>
 * <b>Beware</b>, in order for this to work, the cells you are using must allow {@code null} items!
 * <p></p>
 * <b>Dev Notes</b>
 * <p>
 * I often thought about optimizing the cache by using a {@code Map} instead of a {@code Queue} to store the cached cells.
 * However, doing so poses a variety of issues that make such an improvement not possible.
 * <p></p>
 * <b>Pros</b>
 * <p>
 * The enhancement would make the cache more efficient in theory because when a cell is de-cached it could be extracted by
 * the needed item. Which means that if a cell was previously built for that item, the only update needed would be the index.
 * The current implementation, polls the first available cell in the cache and updates both the item and the index.
 * <p></p>
 * <b>Cons</b>
 * <p>
 * <p> - The {@link #populate()} method could not work because the cells are produced with {@code null} items. So, these
 * cells would require a separate collection. This would also make controlling the cache capacity a bit harder, as well as
 * add and poll operations.
 * <p> - The mapping [Item -> VFXCell] raises the problem of updating the cache when the items list changes.
 * If an item is removed from the list, and a mapping is present in the cache, then it becomes invalid. Which means that
 * the cell associated with that item would need to be disposed or moved to the other collection.
 * <p> - T items come from "outside". In this new version of {@code VirtualizedFX}, to simplify the handling of changes in
 * the items' list, mappings of type [Item -> VFXCell] are used in the state. But to avoid memory leaks, old maps are always
 * cleaned, so that there is not any reference to old items. So, again, we would have to manage this aspect too for the cache.
 * <p></p>
 * Ultimately, the number of cons and the implementation complexity they pose make me think that the refactor would not
 * be worth the effort. Especially considering that I have no empiric proof that it would be more efficient, JMH tests
 * would be required, which is even more work and thus adds up to the cons.
 */
public class VFXCellsCache<T, C extends VFXCell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private Function<T, C> cellFactory;
	private final CellsQueue<T, C> queue = new CellsQueue<>(0);

	//================================================================================
	// Constructors
	//================================================================================
	public VFXCellsCache(Function<T, C> cellFactory) {this.cellFactory = cellFactory;}

	public VFXCellsCache(Function<T, C> cellFactory, int capacity) {
		this.cellFactory = cellFactory;
		queue.setCapacity(capacity);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Fills the cache to its limit. Can be useful to 'pre-populate' the cache before init time, making cells already
	 * available and just in need of an update ({@link VFXCell#updateIndex(int)}, {@link VFXCell#updateItem(Object)}).
	 *
	 * @throws NullPointerException if {@link #getCellFactory()} returns {@code null}
	 */
	public VFXCellsCache<T, C> populate() {
		if (queue.size() == queue.getCapacity()) return this; // Already at capacity
		if (cellFactory == null) throw new NullPointerException("Cannot populate cache as the cell factory is null");

		do {
			C c = cellFactory.apply(null);
			queue.add(c);
		} while (queue.size() != queue.getCapacity());
		return this;
	}

	/**
	 * Adds the given cells to the queue. For successfully cached cells, {@link VFXCell#onCache()} will automatically be invoked.
	 */
	@SafeVarargs
	public final VFXCellsCache<T, C> cache(C... cells) {
		for (C c : cells) {
			if (queue.add(c)) c.onCache();
		}
		return this;
	}

	/**
	 * Adds the given cells to the queue. For successfully cached cells, {@link VFXCell#onCache()} will automatically be invoked.
	 */
	public VFXCellsCache<T, C> cache(Collection<C> cells) {
		for (C c : cells) {
			if (queue.add(c)) c.onCache();
		}
		return this;
	}

	/**
	 * Removes one cell from the cache, specifically from the queue's head, so the oldest cached cell.
	 * Beware this can return a {@code null} value, if it's not, {@link VFXCell#onDeCache()} is automatically invoked.
	 */
	public C take() {
		C c = queue.poll();
		if (c != null) c.onDeCache();
		return c;
	}

	/**
	 * Wraps the result of {@link #take()} in an {@link Optional} instance.
	 */
	public Optional<C> tryTake() {
		return Optional.ofNullable(take());
	}

	/**
	 * Removed the specified cell from the cache's queue. The removed cell is also disposed.
	 */
	public VFXCellsCache<T, C> remove(C cell) {
		boolean removed = queue.remove(cell);
		if (removed) cell.dispose();
		return this;
	}

	/**
	 * Disposes and removes all the cells from the cache.
	 */
	public VFXCellsCache<T, C> clear() {
		queue.forEach(VFXCell::dispose);
		queue.clear();
		return this;
	}

	/**
	 * @return the number of cached cells
	 */
	public int size() {
		return queue.size();
	}

	/**
	 * Sets the cache's capacity.
	 */
	public VFXCellsCache<T, C> setCapacity(int capacity) {
		queue.setCapacity(capacity);
		return this;
	}

	public Function<T, C> getCellFactory() {
		return cellFactory;
	}

	public void setCellFactory(Function<T, C> cellFactory) {
		this.cellFactory = cellFactory;
	}
}
