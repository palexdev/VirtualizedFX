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

package misc;

import io.github.palexdev.virtualizedfx.base.VFXContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestContext {

    @Test
    void testContext() {
        VFXContext<Object> context = new VFXContext<>(null);
        // Prepare
        context.add(ServiceA.class, new ServiceA());
        context.add(ServiceB.class, new ServiceB());
        context.addLocked(ServiceC.class, null);

        assertEquals(3, context.services().size());
        assertTrue(context.isLocked(ServiceC.class));

        ServiceC sc = context.getService(ServiceC.class);
        assertNull(sc);
        assertEquals(2, context.services().size());
        assertFalse(context.isLocked(ServiceC.class));

        ServiceA currA = context.getService(ServiceA.class);
        context.addLocked(ServiceA.class, new ServiceA());
        assertEquals(2, context.services().size());
        assertNotNull(context.getService(ServiceA.class));
        assertNotSame(currA, context.getService(ServiceA.class));

        assertThrows(IllegalStateException.class, () -> context.add(ServiceA.class, new ServiceA()));

        assertThrows(IllegalStateException.class, () -> context.requireService(ServiceC.class));
    }

    static class ServiceA {}

    static class ServiceB {}

    static class ServiceC {}
}
