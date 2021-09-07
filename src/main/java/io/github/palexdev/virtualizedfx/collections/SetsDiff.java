package io.github.palexdev.virtualizedfx.collections;

import java.util.Set;
import java.util.stream.Collectors;

public class SetsDiff<T> {
    private Set<T> added;
    private Set<T> removed;

    public static <T> SetsDiff<T> difference(Set<T> first, Set<T> second) {
        SetsDiff<T> diff = new SetsDiff<>();
        diff.added = second.parallelStream().filter(t -> !first.contains(t)).collect(Collectors.toSet());
        diff.removed = first.parallelStream().filter(t -> !second.contains(t)).collect(Collectors.toSet());
        return diff;
    }

    public static SetsDiff<Integer> intDifference(Set<Integer> first, Set<Integer> second) {
        SetsDiff<Integer> diff = new SetsDiff<>();
        diff.added = second.parallelStream().filter(i -> !first.contains(i)).collect(Collectors.toSet());
        diff.removed = first.parallelStream().filter(i -> i >= 0 && !second.contains(i)).collect(Collectors.toSet());
        return diff;
    }

    public Set<T> getAdded() {
        return added;
    }

    public Set<T> getRemoved() {
        return removed;
    }
}
