package io.github.palexdev.virtualizedfx.collections;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utils class to compute the difference between to Sets.
 * <p></p>
 * The resulting SetsDiff instance will contain two Sets for added and removed items.
 *
 * @param <T> the type of objects contained in the Sets
 */
public class SetsDiff<T> {
    //================================================================================
    // Properties
    //================================================================================
    private Set<T> added;
    private Set<T> removed;

    //================================================================================
    // Constructors
    //================================================================================
    private SetsDiff() {}

    //================================================================================
    // Static Methods
    //================================================================================
    /**
     * Computes the difference between the given sets.
     * <p>
     * The resulting "added" Set will contain all the items of the second Set that are NOT contained in the first.<p>
     * The resulting "removed" Set will contain all the items of the first Set that are NOT contained in the second.<p>
     *
     * @return an instance of {@link SetsDiff}
     */
    public static <T> SetsDiff<T> difference(Set<T> first, Set<T> second) {
        SetsDiff<T> diff = new SetsDiff<>();
        diff.added = second.parallelStream().filter(t -> !first.contains(t)).collect(Collectors.toSet());
        diff.removed = first.parallelStream().filter(t -> !second.contains(t)).collect(Collectors.toSet());
        return diff;
    }

    /**
     * Computes the difference between the given sets of indexes.
     * <p>
     * The resulting "added" Set will contain all the items of the second Set that are NOT contained in the first.<p>
     * The resulting "removed" Set will contain all the items of the first Set that are NOT contained in the second and that are greater or equal to 0.<p>
     *
     * @return an instance of {@link SetsDiff}
     */
    public static SetsDiff<Integer> indexDifference(Set<Integer> first, Set<Integer> second) {
        SetsDiff<Integer> diff = new SetsDiff<>();
        diff.added = second.parallelStream().filter(i -> !first.contains(i)).collect(Collectors.toSet());
        diff.removed = first.parallelStream().filter(i -> i >= 0 && !second.contains(i)).collect(Collectors.toSet());
        return diff;
    }

    //================================================================================
    // Getters
    //================================================================================

    /**
     * @return the Set containing all the added items
     */
    public Set<T> getAdded() {
        return added;
    }

    /**
     * @return the Set containing all the removed items
     */
    public Set<T> getRemoved() {
        return removed;
    }
}
