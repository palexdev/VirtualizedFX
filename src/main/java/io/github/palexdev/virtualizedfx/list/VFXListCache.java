package io.github.palexdev.virtualizedfx.list;

import io.github.palexdev.virtualizedfx.cells.Cell;
import io.github.palexdev.virtualizedfx.utils.CellsQueue;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;

/**
 * Simple cache implementation for the {@link VFXList} container. Cells are stored in a {@link CellsQueue},
 * a special extension of {@link LinkedList}. The cache can be limited in the maximum number of cells to keep by setting its
 * capacity, see {@link VFXList#cacheCapacityProperty()}. Cells won't be added if the capacity is reached and will
 * be disposed immediately instead.
 * <p>
 * Aside from the typical functions of a cache (take/store), you are also allowed to 'populate' it at will, see {@link #populate()}.
 * Since we know how to generate the cache items (cells), because they are created by the {@link VFXList#cellFactoryProperty()},
 * you are allowed to fill the cache whenever you want. This is most useful to 'pre-populate' it, fill the cache before
 * the container is even laid out so that at init cells are already built and just need to be updated.
 * <b>Beware</b>, in order for this to work, the cells you are using must allow {@code null} items!
 */
public class VFXListCache<T, C extends Cell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final VFXList<T, C> list;
	private final CellsQueue<T, C> queue = new CellsQueue<>(0);

	//================================================================================
	// Constructors
	//================================================================================
	public VFXListCache(VFXList<T, C> list) {this.list = list;}

	public VFXListCache(VFXList<T, C> list, int capacity) {
		this.list = list;
		queue.setCapacity(capacity);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Fills the cache to its limit. Can be useful to 'pre-populate' the cache before init time, making cells already
	 * available and just in need of an update ({@link Cell#updateIndex(int)}, {@link Cell#updateItem(Object)}).
	 *
	 * @throws NullPointerException if {@link VFXList#getCellFactory()} returns {@code null}
	 */
	public VFXListCache<T, C> populate() {
		Function<T, C> f = list.getCellFactory();
		if (queue.size() == queue.getCapacity()) return this; // Already at capacity
		if (f == null) throw new NullPointerException("Cannot populate cache as the cell factory is null");

		do {
			C c = f.apply(null);
			queue.add(c);
		} while (queue.size() != queue.getCapacity());
		return this;
	}

	/**
	 * Adds the given cells to the queue. For successfully cached cells, {@link Cell#onCache()} will automatically be invoked.
	 */
	@SafeVarargs
	public final VFXListCache<T, C> cache(C... cells) {
		for (C c : cells) {
			if (queue.add(c)) c.onCache();
		}
		return this;
	}

	/**
	 * Adds the given cells to the queue. For successfully cached cells, {@link Cell#onCache()} will automatically be invoked.
	 */
	public VFXListCache<T, C> cache(Collection<C> cells) {
		for (C c : cells) {
			if (queue.add(c)) c.onCache();
		}
		return this;
	}

	/**
	 * Removes one cell from the cache, specifically from the queue's head, so the oldest cached cell.
	 * Beware this can return a {@code null} value, if it's not, {@link Cell#onDeCache()} is automatically invoked.
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
	public VFXListCache<T, C> remove(C cell) {
		boolean removed = queue.remove(cell);
		if (removed) cell.dispose();
		return this;
	}

	/**
	 * Disposes and removes all the cells from the cache.
	 */
	public VFXListCache<T, C> clear() {
		queue.forEach(Cell::dispose);
		queue.clear();
		return this;
	}

	public int size() {
		return queue.size();
	}

	/**
	 * Sets the cache's capacity. Visibility is restricted because capacity is meant to be set through the
	 * {@link VFXList#cacheCapacityProperty()}.
	 */
	protected VFXListCache<T, C> setCapacity(int capacity) {
		queue.setCapacity(capacity);
		return this;
	}
}
