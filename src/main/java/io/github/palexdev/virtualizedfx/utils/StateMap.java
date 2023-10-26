package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.virtualizedfx.cells.Cell;

import java.util.*;

/**
 * A wrapper class for two {@link Map} collections, used to store the cells of a virtualized container's state.
 * <p></p>
 * A very special kind of data structure that takes advantage of a crucial aspect in virtualized containers and cells:
 * <p>
 * There is a one to one correspondence between a cell and its index, but also between its index and its item.
 * In other words: at a specific moment in time, a cell at index {@code i} will display an item {@code t} at the same
 * index {@code i}. This is the double correspondence: Index -> Cell | Item -> Index, combined leads to this Index -> Cell <- Item
 * <p></p>
 * This important property allows us to use two maps to store and retrieve the cells:
 * <p> 1) The first map is a {@link TreeMap} that stores the cells by their index [Index -> Cell]
 * <p> 2) The second map is a {@link HashMap} that stores the indexes by item [Item -> Index]
 * <p>
 * Retrieval by index is straightforward. Retrieval by item is a little bit trickier but should affect less the memory.
 * Given an item, first we retrieve the index at which that item is in the list, then we do a retrieval by index. Sure,
 * it's two retrievals instead of one, but we are talking about maps, they are really fast anyway.
 * <p></p>
 * <b>Q:</b> Ok but, where is the benefit in doing this?
 * <b>A:</b> It's super effective for changes affecting the dataset. You see, when additions or removals happen in the
 * dataset, retrieving the cells by index is not feasible anymore. Consider this example:
 * <pre>
 * {@code
 * // Starting list
 * List<String> data = List.of("A", "B", "C", "D", "E", "F"); // yeah yeah I know, List.of is unmodifiable, assume it is for the sake of this example
 * // These are the [Index:Item] pairs: [0:A] [1:B] [2:C] [3:D] [4:E] [5:F]
 * // Let's add some items at index 1
 * data.addAll(1, "Z", "X") // yeah yeah, I know, addAll() only accepts a Collection object, goddammit Java collections can suck at times
 * // Here's how the pairs have changed: [0:A] [1:Z] [2:X] [3:B] [4:C] [5:D] [6:E] [7:F]
 * // For the following explanation, let's assume the range of items we want is [0, 4]
 * // As you can see, if we try to retrieve the cells by index (given that we have them still stored by the old pairs)
 * // we will have to update the cells which display items B and C, but wait, these cells are present in the range and they
 * // already have the correct item!! This leads to useless updates that take CPU power to run, meh!
 * // To avoid these updates, we can try retrieving the cells by item:
 * for (int i = 0; i < 5; i++) {
 *     T item = data.get(i);
 *     C cell;
 *     Integer index = byItemMap.remove(item);
 *     if (index != null) { // maybe found
 *         cell = byIndexMap.remove(index);
 *     }
 *     if (cell != null) { // definitely found
 *         cell.updateIndex(i)
 *     }
 * }
 * // Let's see if the loop is right with the above example
 * // At i = 0; Item = "A"; Index = 0; Cell = [0:A] found! Update index even if unnecessary, still better than updateItem (or find a way to not trigger it)
 * // At i = 2; Item = "X"; Index = null; Cell = not found -> will probably need to reuse or create a cell
 * // At i = 4; Item = "C"; Index = 2; Cell = [2:C] found! Update index so that the pair becomes [4:C], update item avoided ;)
 * </pre>
 * <p>
 * See? It's really convenient to avoid unnecessary updates. The risk is potentially introducing a memory leak. We don't know
 * the model, we don't know what kind of item {@code T} the user is displaying. A way to avoid this could be to use a
 * {@link WeakHashMap} for the second mapping but this would still not be very safe as the two maps need to be synchronized,
 * same size at least. There's also another issue, since the hashCode() and equals() may be overridden, it's also the user
 * responsibility for this data structure to work properly.
 */
public class StateMap<T, C extends Cell<T>> {
	//================================================================================
	// Properties
	//================================================================================
	private final SequencedMap<Integer, C> byIndex = new TreeMap<>();
	private final Map<T, Integer> byItem = new HashMap<>();

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Tries retrieving a cell by the given index.
	 */
	public C getCell(Integer index) {
		return byIndex.get(index);
	}

	/**
	 * Tries retrieving a cell by the given item. First an index is retrieved from the [Item -> Integer] map, then
	 * delegates to {@link #getCell(Integer)}. If the cell is null, the [Item:Index] pair is also removed from the map.
	 */
	public C getCell(T item) {
		Integer index = byItem.get(item);
		C c = getCell(index);
		if (c == null) byItem.remove(item);
		return c;
	}

	/**
	 * Adds a cell to both the maps used by this data structure. First by putting [Index -> Cell] and then by putting
	 * [Item -> Index].
	 */
	public void put(Integer index, T item, C cell) {
		assert index != null && item != null && cell != null;
		byIndex.put(index, cell);
		byItem.put(item, index);
	}

	/**
	 * Checks if the map [Index -> Cell] contains the given index.
	 */
	public boolean contains(Integer index) {
		return byIndex.containsKey(index);
	}

	/**
	 * Checks if the map [Item -> Index] contains the given item.
	 */
	public boolean contains(T item) {
		return byItem.containsKey(item);
	}

	/**
	 * Removes a cell from the map [Integer -> Cell] by the given index.
	 */
	public C remove(Integer index) {
		return byIndex.remove(index);
	}

	/**
	 * Removes an index from the map [Item -> Index] and then if the index is not null, removes a cell from the map
	 * [Integer -> Cell] by the found index.
	 */
	public C remove(T item) {
		Integer index = byItem.remove(item);
		return index == null ? null : byIndex.remove(index);
	}

	/**
	 * @return the size of this data structure. Since it is expected for both the used maps to have the same size, this delegates
	 * to the {@link Map#size()} method of the [Integer -> Cell] map.
	 */
	public int size() {
		return byIndex.size();
	}

	/**
	 * @return whether the data structure is empty. Since it is expected for both the used maps to have the same size, this delegates
	 * to the {@link Map#isEmpty()} method of the [Integer -> Cell] map.
	 */
	public boolean isEmpty() {
		return byIndex.isEmpty();
	}

	/**
	 * Clears both the used maps.
	 */
	public void clear() {
		byIndex.clear();
		byItem.clear();
	}

	/**
	 * Resolves the double correspondence [Integer -> Cell <- Item] returning a map that directly gives [Item -> Cell].
	 * Relatively costly operation, use only if necessary.
	 */
	public Map<T, C> resolve() {
		Map<T, C> map = new HashMap<>();
		byItem.forEach((t, i) -> map.put(t, getCell(i)));
		return map;
	}

	/**
	 * @return the map used to store the cells by their index [Integer -> Cell]
	 */
	public SequencedMap<Integer, C> getByIndex() {
		return byIndex;
	}

	/**
	 * @return the map used to store the indexes by item [Item -> Integer]
	 */
	public Map<T, Integer> getByItem() {
		return byItem;
	}
}
