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

package src.cells;

import java.util.function.Supplier;

import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;

import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static src.utils.TestFXUtils.counter;

public class TestCell<T> extends VFXSimpleCell<T> {
    public TestCell(T item) {
        super(item);
    }

    @Override
    public void onDeCache() {
        counter.fCache();
    }

    @Override
    public void onCache() {
        counter.tCache();
    }

    @Override
    public void dispose() {
        counter.disposed();
        super.dispose();
    }

    @Override
    public Supplier<SkinBase<?, ?>> defaultSkinProvider() {
        return () -> new VFXLabeledCellSkin<>(this) {
            {
                setConverter(t -> {
                    int index = getIndex();
                    return "Index: %d Item: %s".formatted(
                        index,
                        t != null ? t.toString() : ""
                    );
                });
                setStyle("-fx-border-color: red");
                update();
                counter.index();
                counter.item();
            }

            @Override
            protected void addListeners() {
                listeners(
                    onInvalidated(indexProperty())
                        .then(v -> {
                            counter.index();
                            update();
                        }),
                    onInvalidated(itemProperty())
                        .then(v -> {
                            counter.item();
                            update();
                        })
                );
            }

            @Override
            protected void update() {
                T item = getItem();
                label.setText(getConverter().toString(item));
            }
        };
    }
}
