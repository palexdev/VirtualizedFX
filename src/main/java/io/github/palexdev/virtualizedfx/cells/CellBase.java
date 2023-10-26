package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.controls.Control;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

import java.util.function.Supplier;

/**
 * The basic and typical implementation of a cell in JavaFX is a cell with just two properties: one to keep track
 * of the cell's index and the other to keep track of the displayed item.
 * <p>
 * This abstract class is a good starting point to implement concrete, usable cells.
 * <p>
 * Unusually, this enforces the structuring of cells as specified by the {@code MVC} pattern. In fact, this extends
 * {@link Control}, expects behaviors of type {@link CellBaseBehavior} and doesn't come with a default skin.
 * The default style class is 'cell-base'. Needless to say, also implements {@link Cell}.
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
 */
public abstract class CellBase<T> extends Control<CellBaseBehavior<T>> implements Cell<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final IntegerProperty index = new SimpleIntegerProperty();
	private final ObjectProperty<T> item = new SimpleObjectProperty<>();

	//================================================================================
	// Constructors
	//================================================================================
	public CellBase() {
		initialize();
	}

	public CellBase(T item) {
		this();
		updateItem(item);
	}

	//================================================================================
	// Methods
	//================================================================================
	protected void initialize() {
		getStyleClass().add("cell-base");
		setDefaultBehaviorProvider();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
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

	@Override
	public Supplier<CellBaseBehavior<T>> defaultBehaviorProvider() {
		return () -> new CellBaseBehavior<>(this);
	}

	//================================================================================
	// Getters/Setters
	//================================================================================

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
}
