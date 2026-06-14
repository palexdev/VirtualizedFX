/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.utils;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;

/// A peculiar data structure that allows to bidirectionally map two types of indexed data `K` and `V`.
/// It can be seen as a generalization and a specialization of a classic `BiMap` collection at the same time.
///
/// Usually a `BiMap` uses two types of data `K` and `V` to produce mappings of type `[K -> V]`
/// and `[V -> K]`.
///
/// However, for `VirtualizedFX`'s needs this works on three types of data: `Integer`, `K` and `V`.
/// This is especially useful when we want to "convert" a flat data structure like a List or even an array so that every
/// element is mapped by its position in the origin collection. Take this list as an example:
/// ```
/// List<String> strings = List.of(
///     "String 0",
///     "String 1",
///     "String 2",
///     "String 3",
///     "String 0",
///     "String 5",
///     "String 8",
///     "String 2",
///     "String 7",
///     "String 0",
/// )
/// ```
///
/// With a data structure like this you could achieve something like this:
/// ```
/// IndexBiMap<Integer, String> biMap = new IndexBiMap<>();
/// for (int i = 0: i < strings.size(); i++) {
///     biMap.put(i, i, strings.get(i));
/// }
/// // The above example shows how this collection can be used as a classical BiMap too (in case K are integers!)
/// ```
///
/// However, the true nature of this data structure is to achieve something like this:
/// ```
/// List<V> values = ..; // These values are generated from a String in the strings list
/// IndexBiMap<String, V> biMap = new IndexBiMap<>();
/// for (int i = 0; i < strings.size(); i++) {
///     String s = strings.get(i);
///     V val = values.get(i);
///     biMap.put(i, s, val);
/// }
/// // Get a V val from index...
/// V val = biMap.get(0); // returns "String 0"
/// // Get a V val from a K object (will be simplified for easier comprehension, read more below)
/// Integer index = biMap.get("String 3"); // returns 3
/// V val = biMap.get(index); // returns "String 3"
/// ```
///
/// If you carefully analyze the above examples, you may be able to spot an issue with this data structure: **duplicates**.
///
/// Since this is basically a wrapper for two [Map]s, the moment we decide to map `K` objects to `V` values,
/// we introduce this issue, and we must deal with it.
///
/// In collections such as the List (even arrays) there is one to one association between the index/position of an
/// element and the element itself.
///
/// At index 0 we have "String 0", nothing else. Same applies for other indexes.
///
/// This means that we can also see the List as a Map of type `[Integer->V]`, however, the contrary does not apply. In fact,
/// a string "String 1" could appear at different positions in the List. So, how do we deal with this?
///
/// Simple, we store the positions in a collection.
///
/// The `IndexBiMap`, like said above, uses two maps:
///
/// 1) A [TreeMap] for the mappings of type `[Integer -> V]`. A simple [HashMap] could be used too,
/// but I thought that having positions sorted is a nice to have, especially if we want to 'poll' values from the map as we
/// would do from a [Deque] (TreeMap is a [SequencedMap]). This is called the `byIndex` map.
///
/// 2) An [IdentityHashMap] for the mappings of type `[K -> Collection<Integer>]`. To be precise the collection
/// used to store all the positions for a certain `K` object is a [SequencedSet], which again allows us to
/// 'poll' values when needed. Here, such operation, is especially needed because since all the positions in the set point
/// to the same element, it does not matter which one we take, so [SequencedSet#removeFirst()] comes in handy.
/// This is called the `byKey` map.
///
/// 3) These are two to "concrete" mappings, however the second one is automatically resolved by the map like this:
/// `[K -> Integer -> V]` which can be simplified to `[K -> V]`.
///
/// **Q:Why an [IdentityHashMap]?**
///
/// **A:** There are two things to consider. First, let's not forget that the true nature of this data structure is to
/// map a type `K` which may come from our dataset, to certain `V` objects that **depend** on `K`
/// items. For example: in `VirtualizedFX` cells depend on the items `T` and we want to reuse them as much as possible
/// for performance reasons. Second, the above example is not really suited to understand this, because [String] in
/// Java is a particular case. Strings that are equal in value are also equal in reference, same object, because Java caches
/// string literals for performance and memory reasons. Instead, let's consider this example:
/// ```
/// // Let's say this is our model class
/// public record User(int id, String name, String email) {}
/// List<User> users = ...;
/// // In this list, we may have two Users with a different reference (different object) but with the same values (equal)
/// // Each user is used in an object of type V
/// List<V> values = ...;
/// // Let's suppose now, we make some changes to the users list (additions, removals, updates,...)
/// // And now we need to update the V values as well
/// // We want to ignore those for which the User is the same, and update those for which the User at pos i in the list is now different
/// // Here's where IDENTITY is way more important than EQUALITY
/// // Let's suppose that during the update, because we consider equality instead of identity, we accidentally swap the
/// // User objects of two V values. What do you think it may happen?
/// // It may happen that if we update the values of a certain User object (ignore that it is a record), we may not see the
/// // change in the associated V object, because we swapped it. So, instead, we would see the change in an entirely different V object
/// ```
///
/// **Retrievals**
/// This data structure provides to way of retrieving `V` values, either by index [#get(Integer)] or by key
/// [#get(Object)].
///
/// **Additions**
/// This data structure provides a single [#put(Integer, Object, Object)] method, which requires both the key and the
/// value, as well as the value's index.
///
/// **Removals**
/// Removals are critical operations!
///
/// There are three removal methods, although one is a delegate: [#remove(Integer)], [#remove(Integer, boolean)]
/// and [#remove(Object)].
///
/// As also described in the method documentation, when values are removed by index, we may potentially have an invalid
/// data structure afterward. While it's easy to resolve a reverse mapping like this `[K -> Integer -> V]`, it's not
/// as straightforward to do this `[Integer -> K]` because we don't have such direct mapping.
///
/// In other words, when a value is removed from the `byIndex` map, there is not a fast way to also remove it from
/// the `byKey` map. The only way is to iterate over its `Sets` and when the index is found in one of them,
/// remove it and break out of the loop. Other details here: [#remove(Integer, boolean)].
///
/// **Misc**
/// This data structure also allows you to check whether a value is present either by index or by key: [#contains(Integer)],
/// [#contains(Object)].
///
/// There is also a simple check on the two maps sizes for the data structure's validity: [#isValid()].
///
/// **Usage in VirtualizedFX**
/// - See [StateMap]
/// - See [RowsStateMap]
public class IndexBiMap<K, V> {
    //================================================================================
    // Properties
    //================================================================================
    protected final SequencedMap<Integer, V> byIndex = new TreeMap<>();
    protected final Map<K, SequencedSet<Integer>> byKey = new IdentityHashMap<>();

    //================================================================================
    // Methods
    //================================================================================

    /// Tries to retrieve a value for the given index from the `byIndex` map.
    public V get(Integer index) {
        return byIndex.get(index);
    }

    /// Tries to retrieve a list of values for the given key from the `byKey` map.
    ///
    /// First retrieves a [SequencedSet], then if it is `null` returns an empty list.
    /// If it is empty, then the mapping is not valid anymore, therefore, it's removed from the map, and empty list returned.
    /// Otherwise, the indexes in the `Set` are resolved by calling [#get(Integer)] and the values returned in a list.
    /// We also make sure that the list will not contain any `null` value.
    ///
    /// See [IndexBiMap] to understand why this may return multiple values (hint: duplicates).
    public List<V> get(K key) {
        SequencedSet<Integer> set = byKey.get(key);
        if (set == null) return List.of();
        if (set.isEmpty()) {
            byKey.remove(key);
            return List.of();
        }
        return set.stream()
            .map(this::get)
            .filter(Objects::nonNull)
            .toList();
    }

    /// Adds the appropriate mappings for the given parameters to both the maps by this data structure.
    ///
    /// First the entry `[Integer, V]` is added to the `byIndex` map.
    ///
    /// Then the entry `[K, SequencedSet<Integer>]` is added to the `byKey` map.
    ///
    /// See [IndexBiMap] to understand why the second mapping is like that.
    public void put(Integer index, K key, V val) {
        if (index == null || key == null || val == null)
            throw new NullPointerException("Cannot add entry [Index:%s; Item:%s; VFXCell:%s] in state map".formatted(index, key, val));
        byIndex.put(index, val);
        byKey.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(index);
    }

    /// @return whether an entry for the given index is present in the `byIndex` map
    public boolean contains(Integer index) {
        return byIndex.containsKey(index);
    }

    /// @return whether an entry for the given key is present in the `byKey` map
    public boolean contains(K key) {
        SequencedSet<Integer> set = byKey.get(key);
        return set != null && !set.isEmpty();
    }

    /// Tries to remove a value from the `byIndex` map by the given index.
    ///
    /// When such operation occurs, the two wrapped maps become desynchronized; therefore, the bimap becomes invalid.
    /// The issue is that there is no fast way to also remove the value from the `byKey` map, because the mappings
    /// are `[K, SequencedSet<Integer>]`, so the only way is to iterate over the other map.
    ///
    /// So, by design choice, this data structure prioritizes speed rather than reliability.
    ///
    /// The `validate` parameter allows you to keep the data structure valid by performing the aforementioned iteration.
    /// For every [SequencedSet] in the `byKey` map we attempt at removing the `index` parameter.
    /// If the removal was successful, it means that we found the correct mapping thus we break out of the loop.
    /// Also, if the `Set` becomes empty after the removal, the mapping is removed from the `byKey` map.
    ///
    /// **Q: How can you be sure that the Set containing the given index is the right mapping?**
    ///
    /// **A:** there is a little issue with this data structure. The moment we decide to have mappings from a key
    /// `K` to an index `Integer`, we **must** take into account **duplicates**.
    /// For example, let's consider a list. We can confidently assert that at every index corresponds one and only one item.
    /// However, we cannot make the reverse claim: that at every item corresponds one and only one index.
    /// It's possible for the same item to appear multiple times at different positions within the list.
    ///
    /// So, by the first assertion, if for a key `K` the `Set` of indexes contains the given index, then it
    /// is for sure the right mapping.
    public V remove(Integer index, boolean validate) {
        V val = byIndex.remove(index);
        if (val == null) return null;
        if (validate) {
            Iterator<SequencedSet<Integer>> it = byKey.values().iterator();
            while (it.hasNext()) {
                SequencedSet<Integer> next = it.next();
                if (next.remove(index)) {
                    if (next.isEmpty()) it.remove();
                    break;
                }
            }
        }
        return val;
    }

    /// Delegates the removal to [#remove(Integer, boolean)].
    ///
    /// By default, this will not validate the data structure, meaning that after a removal done by this method, the bimap will
    /// become **invalid!**
    ///
    /// See [#remove(Integer, boolean)] for what _invalid_ means.
    public V remove(Integer index) {
        return remove(index, false);
    }

    /// Before the actual value can be removed, the mapping [K,SequencedSet<Integer>] must be resolved.
    ///
    /// First a `Set` of indexes is retrieved (not removed!) from the `byKey` map by the given key.
    /// If the `Set` is null or empty, `null` is returned. In the latter case, the mapping is also removed.
    /// <br >
    ///
    /// Otherwise, one of the indexes is removed from it by using [SequencedSet#removeFirst()], and then we can
    /// remove and return the value by the retrieved index from the `byIndex` map.
    /// <br >
    ///
    /// This method is the reason we use a [SequencedSet] to store the indexes. We can benefit from the fast operations
    /// of a `Set` while being able to poll the head of the collection just like [Deque#poll()].
    /// <br >
    ///
    /// <b>Note</b>
    /// The retrieved Set contains all the positions for the given `K` object. Which means that, no matter which
    /// index we remove from the Set, it will point to the same `K` instance anyway. See [IdentityHashMap].
    public V remove(K key) {
        SequencedSet<Integer> set = byKey.get(key);
        if (set == null) return null;
        Integer index = switch (set.size()) {
            case 0 -> {
                byKey.remove(key);
                yield null;
            }
            case 1 -> {
                byKey.remove(key);
                yield set.removeFirst();
            }
            default -> set.removeFirst();
        };
        return byIndex.remove(index);
    }

    /// @return the size of this data structure. Since it is expected for both the maps to have the same size,
    /// this delegates to the [Map#size()] method of the `byIndex` map.
    public int size() {
        return byIndex.size();
    }

    /// @return whether the data structure is empty. Since it is expected for both the maps to have the same size,
    /// this delegates to the [Map#isEmpty()] method of the `byKey` map.
    public boolean isEmpty() {
        return byIndex.isEmpty();
    }

    /// The size of the `byKey` map cannot be retrieved by simply calling [Map#size()] because of duplicates.
    /// For this reason, first we need to flatten the values into another collection, and then we can check the size of
    /// that collection.
    ///
    /// @return whether the two maps have the same size
    /// @see #remove(Integer, boolean)
    /// @see #byKeysFlattened()
    public boolean isValid() {
        return size() == byKeysFlattened().size();
    }

    /// Clears both the maps.
    public void clear() {
        byIndex.clear();
        byKey.clear();
    }

    /// Starting from the two mappings `[Integer, V]``[K, SequencedSet<Integer>]` this method wants to resolve
    /// them to a single mapping of type `[K, V]`. The issue, however, once again is duplicates.
    ///
    /// Because the `byKey` map is designed to take duplicates into account, we would have to resolve the mappings
    /// as follows `[K, Collection<V>]`. However, by design, I decided that it's better to have a flat collection.
    ///
    /// For this reason, the method iterates on each entry of the `byKey` map, and for each index in the [SequencedSet]
    /// creates an [Entry] of type `[K, V]`, by resolving the index to a value using [#get(Integer)],
    /// and then adds it to a [List].
    ///
    /// Because of the nested for loops, this may be a costly operation, use only if necessary!
    public List<Entry<K, V>> resolve() {
        List<Entry<K, V>> resolved = new ArrayList<>();
        for (Entry<K, SequencedSet<Integer>> e : byKey.entrySet()) {
            SequencedSet<Integer> set = e.getValue();
            set.forEach(i -> resolved.add(Map.entry(e.getKey(), get(i))));
        }
        return resolved;
    }

    /// @return the map used to store the values by their index [Integer,V], a copy!
    public SequencedMap<Integer, V> getByIndex() {
        return byIndex;
    }

    /// @return the map used to store the indexes by key [K,Integer], a copy!
    public Map<K, SequencedSet<Integer>> getByKey() {
        return byKey;
    }

    /// Flattens the values of the `byKey` map (which uses mappings of type `[k, SequencedSet<Integer>]` to
    /// a single [Set]. Uses [Stream#flatMap(Function)].
    protected Set<Integer> byKeysFlattened() {
        return byKey.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    //================================================================================
    // Internal Classes
    //================================================================================

    /// Extension of [IndexBiMap] that introduces polling methods: [#pollFirst()], [#pollLast()].
    ///
    /// Also base class to map the items of a `VirtualizedFX` container to [VFXCell] instances.
    public static class StateMapBase<K, T, C extends VFXCell<T>> extends IndexBiMap<K, C> {
        /// Removes the first entry from the `byIndex` map by using [SequencedMap#pollFirstEntry()].
        ///
        /// Note that this won't remove the "corresponding" entry from the `byKey` map, leaving this in an invalid state.
        ///
        /// Ideally, this should be used when "transitioning" to a new bimap.
        public Entry<Integer, C> pollFirst() {
            return byIndex.pollFirstEntry();
        }

        /// Removes the last entry from the `byIndex` map by using [SequencedMap#pollLastEntry()].
        ///
        /// Note that this won't remove the "corresponding" entry from the `byKey` map, leaving this in an invalid state.
        ///
        /// Ideally, this should be used when "transitioning" to a new bimap.
        public Entry<Integer, C> pollLast() {
            return byIndex.pollLastEntry();
        }
    }

    /// Extension of [StateMapBase] which uses mappings of type: `[Integer -> VFXCell]`, `[T -> Collection<Integer>]`
    /// and `[T -> Integer -> VFXCell]`.
    ///
    /// Used by some `VirtualizedFX`'s containers to store the state of the viewport, allowing high usability of
    /// cells, which translates to high performance.
    public static class StateMap<T, C extends VFXCell<T>> extends StateMapBase<T, T, C> {}

    /// Extension of [StateMapBase] which uses mappings of type: `[Integer -> VFXCell]`, `[Column -> Collection<Integer>]`
    /// and `[Column -> Integer -> VFXCell]`.
    ///
    /// Used by the table rows to store their state, allowing high re-usability of cells, which translates to high performance.
    ///
    /// **Q: Why by cells are reverse-mapped by columns?**
    ///
    /// **A:**`VirtualizedFX` is a complex library, not because the code is hard, rather development is. The
    /// core thing to always keep in mind is **performance**. The goal of [IndexBiMap] and its extensions is to
    /// help containers to transition to a new state when changes occur in the dataset.
    /// In this specific case, this helps to deal with changes in the table's list of columns.
    ///
    /// By having mappings, we can quickly separate the cells that need to be updated from the ones that are ready to go.
    @SuppressWarnings("rawtypes")
    public static class RowsStateMap<T, C extends VFXCell<T>> extends StateMapBase<VFXTableColumn<T, ?>, T, C> {
        public static final RowsStateMap EMPTY = new RowsStateMap<>() {
            @Override
            public void put(Integer index, VFXTableColumn<Object, ?> key, VFXCell<Object> val) {}
        };

        /// Basically a shortcut for `byIndex.get(byKey.get(column).getFirst())`, `null` check included.
        ///
        /// @return the cell mapped to the given column or `null` if the indexes [Set] associated to the column
        /// is empty
        public C getSingle(VFXTableColumn<T, ?> column) {
            SequencedSet<Integer> set = byKey.get(column);
            if (set == null) return null;
            return byIndex.get(set.getFirst());
        }
    }
}
