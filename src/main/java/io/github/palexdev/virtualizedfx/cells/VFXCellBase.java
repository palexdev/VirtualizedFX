/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.cells;

import java.util.List;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.MFXStyleable;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.cells.base.VFXCell;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Pos;
import javafx.scene.Node;

/**
 * The basic and typical implementation of a cell in JavaFX is a cell with just two properties: one to keep track
 * of the cell's index and the other to keep track of the displayed item.
 * <p>
 * This abstract class is a good starting point to implement concrete, usable cells.
 * <p>
 * Unusually, this enforces the structuring of cells as specified by the {@code MVC} pattern. In fact, this extends
 * {@link Control}, expects behaviors of type {@link CellBaseBehavior} and doesn't come with a default skin.
 * <p>
 * The idea is to make the skin implementation responsible for how data is represented (a String, a Node, processing, etc.).
 * A downside of such approach is that for some reason, users are a bit reluctant in making or customizing skins, however,
 * I can assure you it's no big deal at all. Also, never forget that {@code VirtualizedFX} containers <b>do not</b>
 * enforce the usage of {@link VFXCellBase} or any of its implementations, if you are more comfortable using a simpler
 * cell system you are free do it.
 * <p>
 * The default style class is 'cell-base'.
 * <p></p>
 * <b>A word on performance</b>
 * As also stated in the javadocs of {@link #updateIndex(int)} and {@link #updateItem(Object)}, to simplify the internal
 * management of cells, there is no 100% guarantee that updates are called with a different index/item from what the cell
 * currently has. For this reason, here's how you should implement the 'rendering' operations: <p>
 * You probably want to execute some operations when the index or item change by listening to the respective properties,
 * {@link #indexProperty()} and {@link #itemProperty()}. This means that your operations will run only and only if the
 * property fires an invalidation/change event. In this base class the {@link #updateIndex(int)} and {@link #updateItem(Object)}
 * methods are implemented naively, because we work on generic items, we don't know the model, which means that they
 * update the respective properties without any check.
 * <p> - For the {@link #indexProperty()} it's tricky: the JVM caches Integers
 * between -128 and 127, which means that the '==' operator only works in that range; for larger datasets, you may want to
 * override the {@link #updateIndex(int)} method to actually check for equality.
 * <p> For the {@link #itemProperty()} it's the same concept. If you know the model, you may want to perform some equality
 * check in the {@link #updateItem(Object)} method to avoid useless updates. For example, if in your dataset there are two
 * {@code Person} objects with the same attributes but different references you may want to update the property (so that the
 * reference is correct) but not perform any operation that strictly depends on the attributes (if a label displays the attributes,
 * there's no need to re-compute the text)
 *
 * @see #alignmentProperty()
 * @see VFXCell
 */
public abstract class VFXCellBase<T> extends Control<CellBaseBehavior<T>> implements VFXCell<T>, MFXStyleable {
    //================================================================================
    // Properties
    //================================================================================
    private VFXContainer<T> container;
    private final IntegerProperty index = new SimpleIntegerProperty(-1);
    private final ObjectProperty<T> item = new SimpleObjectProperty<>();
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<>();

    //================================================================================
    // Constructors
    //================================================================================
    public VFXCellBase(T item) {
        initialize();
        updateItem(item);
    }

    //================================================================================
    // Methods
    //================================================================================
    private void initialize() {
        defaultStyleClasses(this);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public List<String> defaultStyleClasses() {
        return List.of("cell-base");
    }

    @Override
    public Supplier<CellBaseBehavior<T>> defaultBehaviorProvider() {
        return () -> new CellBaseBehavior<>(this);
    }

    @Override
    public Node toNode() {
        return this;
    }

    /**
     * {@inheritDoc}
     * <p></p>
     * Updates the {@link #itemProperty()}.
     */
    @Override
    public void updateItem(T item) {
        setItem(item);
    }

    /**
     * {@inheritDoc}
     * <p></p>
     * Updates the {@link #indexProperty()}.
     */
    @Override
    public void updateIndex(int index) {
        setIndex(index);
    }

    /**
     * Stores the {@link VFXContainer} instance that owns this cell.
     * <p>
     * The implementation disallows subsequent calls.
     * In other words, once the instance is set (and not null), it will not be replaced.
     */
    @Override
    public void onCreated(VFXContainer<T> container) {
        if (this.container == null) {
            this.container = container;
        }
    }

    @Override
    public void dispose() {
        container = null;
    }

    //================================================================================
    // Styleable Properties
    //================================================================================
    private final StyleableObjectProperty<Pos> alignment = new StyleableObjectProperty<>(
        StyleableProperties.ALIGNMENT,
        this,
        "alignment",
        Pos.CENTER_LEFT
    ) {
        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    public Pos getAlignment() {
        return alignment.get();
    }

    /**
     * Specifies the alignment of the displayed data. How this is used depends on the skin implementation.
     * <p>
     * This is settable via CSS with the "-fx-alignment" property.
     */
    public StyleableObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    public void setAlignment(Pos alignment) {
        this.alignment.set(alignment);
    }

    //================================================================================
    // CssMetaData
    //================================================================================
    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXCellBase<?>> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXCellBase<?>, Pos> ALIGNMENT =
            FACTORY.createEnumCssMetaData(
                Pos.class,
                "-fx-alignment",
                VFXCellBase::alignmentProperty,
                Pos.CENTER_LEFT
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                Control.getClassCssMetaData(),
                ALIGNMENT
            );
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }

    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }

    //================================================================================
    // Getters/Setters
    //================================================================================

    /**
     * @return the {@link VFXContainer} instance that owns this cell
     */
    public VFXContainer<T> getContainer() {
        return container;
    }

    public int getIndex() {
        return index.get();
    }

    /**
     * Specifies the cell's index.
     */
    public IntegerProperty indexProperty() {
        return index;
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public T getItem() {
        return item.get();
    }

    /**
     * Specifies the cell's item.
     */
    public ObjectProperty<T> itemProperty() {
        return item;
    }

    public void setItem(T item) {
        this.item.set(item);
    }

    public Node getGraphic() {
        return graphic.get();
    }

    /**
     * Allows adding a {@code Node} to the cell. To be precise, how this property is used depends on the skin implementation.
     */
    public ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    public void setGraphic(Node graphic) {
        this.graphic.set(graphic);
    }
}
