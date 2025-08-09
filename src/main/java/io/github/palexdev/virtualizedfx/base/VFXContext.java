/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
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
    public void addLocked(Class<?> klass, Object service) {
        add(klass, service);
        locked.add(klass);
    }

    public void add(Class<?> klass, Object service) {
        if (locked.contains(klass)) {
            throw new IllegalStateException("Service " + klass.getName() + " is already registered and locked!");
        }
        services.put(klass, service);
    }

    public boolean hasService(Class<?> klass) {
        return services.containsKey(klass);
    }

    public boolean isLocked(Class<?> klass) {
        return locked.contains(klass);
    }

    public VFXContainer<T> getContainer() {
        return container;
    }

    public <S> S getService(Class<S> klass) {
        return services.get(klass);
    }

    public <S> S requireService(Class<S> klass) {
        return Optional.ofNullable(getService(klass))
            .orElseThrow(() -> new IllegalStateException("Required service " + klass.getName() + " is not registered!"));
    }

    public Map<Class<?>, Object> services() {
        return Collections.unmodifiableMap(services);
    }

    //================================================================================
    // Inner Classes
    //================================================================================
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
