package io.github.palexdev.virtualizedfx.beans;

import javafx.beans.property.SimpleObjectProperty;

public class NumberRangeProperty<T extends Number> extends SimpleObjectProperty<NumberRange<T>> {

    @Override
    public void set(NumberRange<T> newValue) {
        NumberRange<T> oldValue = get();
        if (newValue.equals(oldValue)) return;
        super.set(newValue);
    }

    public void setRange(T value) {
        set(NumberRange.of(value));
    }

    public void setRange(T min, T max) {
        set(NumberRange.of(min, max));
    }
}
