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

package io.github.palexdev.virtualizedfx.table;

import io.github.palexdev.virtualizedfx.cell.TableCell;

import java.util.*;

/**
 * A simple cache implementation to help {@link VirtualTable} with horizontal scrolling
 * and some other operations with rows.
 * <p></p>
 * Cached cells are stored in a Map which maps a certain {@link TableColumn} to a Set of {@link TableCell}s
 * (this to ensure uniqueness).
 * <p></p>
 * {@link VirtualTable} also allows to disable this with {@link VirtualTable#enableColumnsCacheProperty()}.
 */
// TODO maybe this could be improved by dividing the clear and dispose operations of TableState and TableRow
public class TableCache<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final VirtualTable<T> table;
	private final Map<TableColumn<T, ? extends TableCell<T>>, Set<TableCell<T>>> cache = new HashMap<>();

	//================================================================================
	// Constructors
	//================================================================================
	public TableCache(VirtualTable<T> table) {
		this.table = table;
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Retrieves the Set for the given column from the map (or creates a new one) and adds to it all the
	 * given cells.
	 *
	 * @param column the {@link TableColumn} for which cache the cells
	 * @param cells  the cells that will be added to the Set (to be cached)
	 */
	@SafeVarargs
	public final TableCache<T> cache(TableColumn<T, ? extends TableCell<T>> column, TableCell<T>... cells) {
		if (cells.length == 0) return this;
		Set<TableCell<T>> cached = cache.computeIfAbsent(column, c -> new HashSet<>());
		Collections.addAll(cached, cells);
		return this;
	}

	/**
	 * Retrieves the Set for the given column from the map (or creates a new one) and adds to it all the
	 * given cells.
	 *
	 * @param column the {@link TableColumn} for which cache the cells
	 * @param cells  the cells that will be added to the Set (to be cached)
	 */
	public TableCache<T> cache(TableColumn<T, ? extends TableCell<T>> column, Collection<TableCell<T>> cells) {
		if (cells.isEmpty()) return this;
		Set<TableCell<T>> cached = cache.computeIfAbsent(column, c -> new HashSet<>());
		cached.addAll(cells);
		return this;
	}

	/**
	 * Attempts at retrieving a {@link TableCell} from the cache map for the given {@link TableColumn}.
	 * <p>
	 * Since this uses Sets to store the cells, to de-cache the first available cell an iterator must be used.
	 * <p>
	 * If no cells are available this will return null.
	 *
	 * @param column the {@link TableColumn} for which try to retrieve a cached cell
	 * @return the previously cached cell or null if none were available
	 */
	public TableCell<T> take(TableColumn<T, ? extends TableCell<T>> column) {
		return Optional.ofNullable(cache.get(column))
				.map(s -> {
					if (s.isEmpty()) return null;
					Iterator<TableCell<T>> it = s.iterator();
					TableCell<T> cell = it.next();
					it.remove();
					return cell;
				})
				.orElse(null);
	}

	/**
	 * Attempts at retrieving a {@link TableCell} from the cache map for the given {@link TableColumn}.
	 * <p>
	 * Since this uses Sets to store the cells, to de-cache the first available cell an iterator must be used.
	 * <p>
	 * Null-safe variant of {@link #take(TableColumn)}, this returns an {@link Optional} rather than the cell itself,
	 * meaning that if no cell was available this returns an empty Optional.
	 *
	 * @param column the {@link TableColumn} for which try to retrieve a cached cell
	 * @return an {@link Optional} that may or may not contain a previously cached cell
	 */
	public Optional<TableCell<T>> tryTake(TableColumn<T, ? extends TableCell<T>> column) {
		return Optional.ofNullable(cache.get(column))
				.map(s -> {
					if (s.isEmpty()) return null;
					Iterator<TableCell<T>> it = s.iterator();
					TableCell<T> cell = it.next();
					it.remove();
					return cell;
				});
	}

	/**
	 * If there is a Set of cells for the given {@link TableColumn} then removes the given {@link TableCell}
	 * from it. If the Set then becomes empty then the mapping is also removed from the cache map.
	 * <p></p>
	 * This is useful when disposing a cell, as this ensures that the cell is not cached as it is supposedly not
	 * valid anymore.
	 */
	public TableCache<T> remove(TableColumn<T, ? extends TableCell<T>> column, TableCell<T> cell) {
		Optional.ofNullable(cache.get(column))
				.ifPresent(cells -> {
					cells.remove(cell);
					if (cells.isEmpty()) cache.remove(column);
				});
		return this;
	}

	/**
	 * Disposes all the cached cells with {@link TableCell#dispose()} and then clears the cache.
	 */
	public TableCache<T> clear() {
		cache.values().stream()
				.flatMap(Collection::stream)
				.forEach(TableCell::dispose);
		cache.clear();
		return this;
	}

	/**
	 * Disposes all the cached cells for the given {@link TableColumn} with {@link TableCell} and then
	 * clears the mapping for that column.
	 */
	public TableCache<T> clear(TableColumn<T, ? extends TableCell<T>> column) {
		Set<TableCell<T>> removed = cache.remove(column);
		if (removed != null) {
			removed.forEach(TableCell::dispose);
		}
		return this;
	}

	//================================================================================
	// Getters
	//================================================================================

	/**
	 * @return the table's instance this cache refers to
	 */
	public VirtualTable<T> getTable() {
		return table;
	}

	/**
	 * @return the cache as an unmodifiable map
	 */
	public Map<TableColumn<T, ? extends TableCell<T>>, Set<TableCell<T>>> getCacheUnmodifiable() {
		return Collections.unmodifiableMap(cache);
	}
}
