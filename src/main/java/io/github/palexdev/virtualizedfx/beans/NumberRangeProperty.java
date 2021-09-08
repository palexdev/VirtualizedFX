package io.github.palexdev.virtualizedfx.beans;

import javafx.beans.property.SimpleObjectProperty;

/**
 * Extension of {@link SimpleObjectProperty} for {@link NumberRange}s.
 */
public class NumberRangeProperty<T extends Number> extends SimpleObjectProperty<NumberRange<T>> {

    /**
     * Convenience method to get the range's upper bound.
     * Null if the range is null.
     */
    public T getMin() {
        return get() == null ? null : get().getMin();
    }

    /**
     * Convenience method to get the range's lower bound.
     * Null if the range is null.
     */
    public T getMax() {
        return get() == null ? null : get().getMin();
    }

    /**
     * Convenience method to set a range with both min and max equal.
     */
    public void setRange(T value) {
        set(NumberRange.of(value));
    }

    /**
     * Convenience method to set a range with the given min and max values.
     */
    public void setRange(T min, T max) {
        set(NumberRange.of(min, max));
    }

    /**
     * Overridden to check equality between ranges and return in case ranges are the same.
     */
    @Override
    public void set(NumberRange<T> newValue) {
        NumberRange<T> oldValue = get();
        if (newValue.equals(oldValue)) return;
        super.set(newValue);
    }
}
