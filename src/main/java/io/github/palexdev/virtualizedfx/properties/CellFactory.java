package io.github.palexdev.virtualizedfx.properties;

import java.util.Optional;
import java.util.function.Function;

import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXContext;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/// A wrapper for cell creation functions used by virtualized containers.
///
/// Encapsulates a cell generation function ([#create(Object)]) along with a reference to the [VFXContext] of
/// the container that uses the factory.
///
/// This allows each created cell to access the container's context upon creation. In other words, this wrapper is an easy
/// way to provide all cells the container's instance as well as additional services without radical API changes.
///
/// **Note 1:** nothing prevents you from creating cells using the function directly, however, keep in mind that to
/// automatically make use of the new [VFXCell#onCreated(VFXContext)] hook, you must use [#create(Object)]
/// instead.
///
/// **Note 2:** for convenience this extends [Property] and delegates the implemented methods to the wrapped
/// cell factory property.
public class CellFactory<T, C extends VFXCell<T>> implements Property<Function<T, C>> {
    //================================================================================
    // Properties
    //================================================================================
    private final VFXContext<T> context;
    private final FunctionProperty<T, C> factory = new FunctionProperty<>() {
        @Override
        protected void invalidated() {
            onInvalidated(get());
        }
    };

    //================================================================================
    // Constructors
    //================================================================================
    public CellFactory(VFXContext<T> context) {
        this.context = context;
    }

    //================================================================================
    // Methods
    //================================================================================

    /// Creates a new cell for the given item, using the current cell factory function.
    ///
    /// If the factory function is set, it generates a cell, calls [VFXCell#onCreated(VFXContext)]
    /// on it, and then returns it. Returns `null` if no factory function is defined.
    public C create(T item) {
        return Optional.ofNullable(getValue())
            .map(f -> f.apply(item))
            .map(c -> {
                c.onCreated(context());
                return c;
            })
            .orElse(null);
    }

    /// Hook method called when the cell factory function is invalidated. Subclasses may override this method
    /// to handle changes to the factory function.
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

    /// @return the virtualized container's [VFXContext] instance.
    public VFXContext<T> context() {
        return context;
    }

    /// @return the container that owns this cell factory and inherently the cells created by it.
    public VFXContainer<T> getOwner() {
        return context().getContainer();
    }

    /// Convenience method to check whether the cell generating function is not `null`.
    public boolean canCreate() {
        return getValue() != null;
    }
}
