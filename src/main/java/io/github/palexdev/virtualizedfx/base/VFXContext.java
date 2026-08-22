/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.base;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import io.github.palexdev.virtualizedfx.properties.CellFactory;

/// Carries a virtualized container's instance and a registry of services. The registry starts empty and stays that
/// way until you put something in it.
///
/// The point is to spare you from threading your own dependencies (a selection model, a formatter, a theme service,
/// whatever the container's parts happen to need) down by hand. Register them once here, and anything the container
/// builds or owns can ask for them.
///
/// Every container exposes its context through [VFXContainer#context()], and from there it reaches the rest by two
/// routes. It is **pushed** onto whatever a [CellFactory] builds, which receives it at creation time through
/// [VFXCell#onCreated(VFXContext)]. Everything else **pulls** it from the container it belongs to.
///
/// Services are keyed by [Class] and registered with [#set(Class, Object)], or with [#setLocked(Class, Object)] when
/// the entry must not be replaced or removed afterwards; both throw on an already-locked key. Retrieval is either
/// [#get(Class)], which returns `null` when there is nothing registered, or [#require(Class)], which throws instead.
///
/// **Beware, the registry holds services weakly.** Nothing here keeps a service alive, so whoever registers one must
/// hold on to it as well; a service that is only reachable from this context can be collected at any time. When that
/// happens the entry is dropped silently on the next lookup, its lock along with it, and [#get(Class)] starts
/// answering `null`. That is deliberate, the context lives as long as the container does and must not turn into a leak,
/// but it does mean a missing service is not necessarily one that was never registered.
public class VFXContext<T> {
    //================================================================================
    // Properties
    //================================================================================
    private final VFXContainer<T> container;
    private final ServicesMap services = new ServicesMap();
    private final Set<Class<?>> locked = new HashSet<>();

    //================================================================================
    // Constructors
    //================================================================================
    public VFXContext(VFXContainer<T> container) {
        this.container = container;
    }

    //================================================================================
    // Methods
    //================================================================================
    /// Registers a service under the given class key, replacing any previous one.
    ///
    /// @throws IllegalStateException if the key was registered with [#setLocked(Class, Object)]
    public void set(Class<?> klass, Object service) {
        if (isLocked(klass)) {
            throw new IllegalStateException("Service " + klass.getName() + " is already registered and locked!");
        }
        services.put(klass, service);
    }

    /// Same as [#set(Class, Object)], but the key is then locked: further [#set(Class, Object)] and
    /// [#reset(Class)] calls on it throw. Use it for services the container itself depends on.
    ///
    /// Note that the lock does not outlive the service: if the service is garbage collected, both the entry and the
    /// lock are dropped, see the class docs.
    public void setLocked(Class<?> klass, Object service) {
        set(klass, service);
        locked.add(klass);
    }

    /// Unregisters the service for the given class key.
    ///
    /// @throws IllegalStateException if the key was registered with [#setLocked(Class, Object)]
    public void reset(Class<?> klass) {
        if (isLocked(klass)) {
            throw new IllegalStateException("Service " + klass.getName() + " is locked and cannot be unregistered!");
        }
        services.remove(klass);
    }

    /// @return whether an entry for the given class key exists.
    ///
    /// **Beware, this only checks for the entry, not for the service.** An entry whose service has been collected is
    /// still an entry until something looks it up, so this can answer `true` where [#get(Class)] answers `null`.
    /// When you actually need the service, ask for it.
    public boolean doesHave(Class<?> klass) {
        return services.containsKey(klass);
    }

    /// @return whether the given class key was registered with [#setLocked(Class, Object)] and is therefore
    /// protected from [#set(Class, Object)] and [#reset(Class)]
    public boolean isLocked(Class<?> klass) {
        return locked.contains(klass);
    }

    /// @return the virtualized container this context belongs to
    public VFXContainer<T> getContainer() {
        return container;
    }

    /// Retrieves the service registered under the given class key, or `null` if there is none, if it was not of the
    /// expected type, or if it has been garbage collected. In that last case the stale entry and its lock are dropped
    /// here, as a side effect of the lookup.
    ///
    /// @see #require(Class)
    public <S> S get(Class<S> klass) {
        return services.get(klass);
    }

    /// Like [#get(Class)], for services a caller cannot do without.
    ///
    /// @throws IllegalStateException if no live service is registered under the given class key
    public <S> S require(Class<S> klass) {
        return Optional.ofNullable(get(klass))
            .orElseThrow(() -> new IllegalStateException("Required service " + klass.getName() + " is not registered!"));
    }

    /// @return the registry, wrapped so that it cannot be modified.
    ///
    /// **Beware, the values are the [WeakReference]s, not the services themselves**, and some of them may already be
    /// cleared. This is a debugging window onto the registry, [#get(Class)] is how you actually read a service.
    public Map<Class<?>, Object> services() {
        return Collections.unmodifiableMap(services);
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// The registry itself: a [HashMap] of [WeakReference]s, so that registering a service here never keeps it
    /// alive. Overloads (not overrides) `put` and `get` to wrap and unwrap the references, and to evict an entry
    /// whose referent is gone.
    class ServicesMap extends HashMap<Class<?>, WeakReference<Object>> {
        public void put(Class<?> klass, Object service) {
            super.put(klass, new WeakReference<>(service));
        }

        public <S> S get(Class<S> klass) {
            S service = Optional.ofNullable(super.get(klass))
                .map(Reference::get)
                .filter(klass::isInstance)
                .map(klass::cast)
                .orElse(null);
            if (service == null) {
                remove(klass);
                locked.remove(klass);
            }
            return service;
        }
    }
}
