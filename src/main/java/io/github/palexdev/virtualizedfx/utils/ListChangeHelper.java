package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.virtualizedfx.beans.NumberRange;
import javafx.collections.ListChangeListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ListChangeHelper {
    public static <T> Change processChange(ListChangeListener.Change<? extends T> change, NumberRange<Integer> indexes) {
        Set<ChangeBean> added = new HashSet<>();
        Set<ChangeBean> removed = new HashSet<>();
        Set<ReplaceBean> replaced = new HashSet<>();

        int removeFrom = -1;
        int removeTo = -1;
        Set<Integer> removedAccumulator = new HashSet<>();

        int removedSize = 0;
        while (change.next()) {
            if (change.wasReplaced()) {
                NumberRange<Integer> range = NumberRange.of(change.getFrom(), change.getTo() - 1);
                Set<Integer> changed = NumberRange.expandRangeToSet(range).stream().filter(i -> NumberRange.inRangeOf(i, indexes)).collect(Collectors.toSet());

                removeFrom = change.getTo();
                removeTo = NumberUtils.clamp(change.getRemovedSize() - 1, 0, indexes.getMax());
                removedAccumulator.addAll(NumberRange.expandRangeToSet(NumberRange.of(removeFrom, removeTo)));

                removedAccumulator.removeAll(changed);
                replaced.add(new ReplaceBean(range, changed, removedAccumulator));
                continue;
            }
            if (change.wasAdded()) {
                NumberRange<Integer> range = NumberRange.of(change.getFrom(), change.getTo() - 1);
                added.add(new ChangeBean(range, NumberRange.expandRangeToSet(range)));
                continue;
            }
            if (change.wasRemoved()) {
                NumberRange<Integer> range = NumberRange.of(change.getTo() + removedSize, NumberUtils.clamp(change.getTo() + change.getRemovedSize() - 1 + removedSize, 0, indexes.getMax()));
                if (removeFrom == -1) removeFrom = range.getMin();
                removeTo = range.getMax();
                removedAccumulator.addAll(NumberRange.expandRangeToSet(range));
                removedSize += change.getRemovedSize();
            }
        }
        removed.add(new ChangeBean(NumberRange.of(removeFrom, removeTo), removedAccumulator));
        return new Change(added, removed, replaced);
    }

    public static class Change {
        private boolean wasReplacement;
        private final Set<ChangeBean> added;
        private final Set<ChangeBean> removed;
        private final Set<ReplaceBean> replaced;

        private Change(Set<ChangeBean> added, Set<ChangeBean> removed, Set<ReplaceBean> replaced) {
            this.added = added;
            this.removed = removed;
            this.replaced = replaced;
        }

        public void processAddition(TriConsumer<Integer, Integer, Set<Integer>> action) {
            if (added.isEmpty()) return;
            for (ChangeBean changeBean : added) {
                if (changeBean.changed.isEmpty()) continue;
                action.accept(changeBean.gerFrom(), changeBean.getTo(), changeBean.changed);
            }
        }

        public void processRemoval(TriConsumer<Integer, Integer, Set<Integer>> action) {
            if (removed.isEmpty() || wasReplacement) return;
            for (ChangeBean changeBean : removed) {
                if (changeBean.changed.isEmpty()) continue;
                action.accept(changeBean.gerFrom(), changeBean.getTo(), changeBean.changed);
            }
        }

        public void processReplacement(BiConsumer<Set<Integer>, Set<Integer>> action) {
            if (replaced.isEmpty()) return;
            for (ReplaceBean replaceBean : replaced) {
                if (replaceBean.isEmpty()) continue;
                action.accept(replaceBean.changed, replaceBean.removed);
            }
            wasReplacement = true;
        }

        public Set<ChangeBean> getAdded() {
            return added;
        }

        public Set<ChangeBean> getRemoved() {
            return removed;
        }

        public Set<ReplaceBean> getReplaced() {
            return replaced;
        }
    }

    public static class ChangeBean {
        private final NumberRange<Integer> range;
        private final Set<Integer> changed;

        public ChangeBean(NumberRange<Integer> range, Set<Integer> added) {
            this.range = range;
            this.changed = added;
        }

        public int gerFrom() {
            return range.getMin();
        }

        public int getTo() {
            return range.getMax();
        }
    }

    public static class ReplaceBean {
        private final NumberRange<Integer> range;
        private final Set<Integer> changed;
        private final Set<Integer> removed;

        public ReplaceBean(NumberRange<Integer> range, Set<Integer> changed, Set<Integer> removed) {
            this.range = range;
            this.changed = changed;
            this.removed = removed;
        }

        public int gerFrom() {
            return range.getMin();
        }

        public int getTo() {
            return range.getMax();
        }

        public boolean isEmpty() {
            return changed.isEmpty() && removed.isEmpty();
        }
    }
}
