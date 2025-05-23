package io.github.palexdev.virtualizedfx.properties;

import java.util.Optional;
import java.util.function.Function;

import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * A wrapper for cell creation functions used by virtualized containers.
 * <p>
 * Encapsulates a cell generation function ({@link #create(Object)}) along with a reference to the associated {@link VFXContainer},
 * allowing each created cell to access the container instance upon creation. In other words, this wrapper is an easy
 * way to provide all cells the container they belong to without radical API changes.
 * <p>
 * Basically provides a hook that invokes {@link VFXCell#onCreated(VFXContainer)} on each newly generated cell,
 * passing the container instance to the cell. This is particularly useful when cells need contextual information from
 * the container, such as configuration, state, selection handling.
 * <p> </p>
 * <b>Note 1:</b> nothing prevents you from creating cells using the function directly, however, keep in mind that to
 * automatically make use of the new {@link VFXCell#onCreated(VFXContainer)} hook, you must use {@link #create(Object)}
 * instead.
 * <p> </p>
 * <b>Note 2:</b> for convenience this extends {@link Property} and delegates the implemented methods to the wrapped
 * cell factory property.
 */
public class CellFactory<T, C extends VFXCell<T>> implements Property<Function<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    private final VFXContainer<T> owner;
    private final FunctionProperty<T, C> factory = new FunctionProperty<>() {
        @Override
        protected void invalidated() {
            onInvalidated(get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public CellFactory(VFXContainer<T> owner) {
        this.owner = owner;
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Creates a new cell for the given item, using the current cell factory function.
     * <p>
     * If the factory function is set, it generates a cell, calls {@link VFXCell#onCreated(VFXContainer)}
     * on it, and then returns it. Returns {@code null} if no factory function is defined.
     */
    public C create(T item) {
        return Optional.ofNullable(getValue())
            .map(f -> f.apply(item))
            .map(c -> {
                c.onCreated(getOwner());
                return c;
            })
            .orElse(null);
    }

    /**
     * Hook method called when the cell factory function is invalidated. Subclasses may override this method
     * to handle changes to the factory function.
     */
    protected void onInvalidated(Function<T, C> newFactory) {}

    //================================================================================
    // Overridden Methods
    //================================================================================

    @Override
    public void bind(ObservableValue<? extends Function<T, C>> observable) {
        factory.bind(observable);
    }

    @Override
    public void unbind() {
        factory.unbind();
    }

    @Override
    public boolean isBound() {
        return factory.isBound();
    }

    @Override
    public void bindBidirectional(Property<Function<T, C>> other) {
        factory.bindBidirectional(other);
    }

    @Override
    public void unbindBidirectional(Property<Function<T, C>> other) {
        factory.unbindBidirectional(other);
    }

    @Override
    public Object getBean() {
        return factory.getBean();
    }

    @Override
    public String getName() {
        return factory.getName();
    }

    @Override
    public void addListener(ChangeListener<? super Function<T, C>> listener) {
        factory.addListener(listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Function<T, C>> listener) {
        factory.removeListener(listener);
    }

    @Override
    public Function<T, C> getValue() {
        return factory.getValue();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        factory.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        factory.removeListener(listener);
    }

    @Override
    public void setValue(Function<T, C> value) {
        factory.setValue(value);
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the container that owns this cell factory and inherently the cells created by it.
     */
    public VFXContainer<T> getOwner() {
        return owner;
    }

    /**
     * Convenience method to check whether the cell generating function is not {@code null}.
     */
    public boolean canCreate() {
        return getValue() != null;
    }
}
