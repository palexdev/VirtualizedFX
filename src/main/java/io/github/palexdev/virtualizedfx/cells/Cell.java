package io.github.palexdev.virtualizedfx.cells;

import javafx.scene.Node;

/**
 * Public, base API for all cells used by any virtualized container. All cells need these three main capabilities:
 * <p> - A way to convert themselves to a {@link Node}, can be implemented in various ways see {@link #toNode()}
 * <p> - A way to keep track of its index
 * <p> - A way to keep track of the displayed item
 * <p></p>
 * Aside from these core functionalities, the API also offers other hooks:
 * <p> - Virtualized containers that make use of a cache (to avoid creating new cells every time one is needed) should
 * make use of {@link #onCache()} and {@link #onDeCache()}. Implementations can track cache/de-cache operations by overriding
 * these methods
 * <p> - When cells are not needed anymore, {@link #dispose()} should be called (automatically handled by the framework
 * don't worry). Here implementations can specify operations to do before the cell is GCd.
 *
 * @param <T> the type of item to display
 */
public interface Cell<T> {

	/**
	 * Converts the cell to a {@link Node}.
	 * <p>
	 * Implementations can follow the following examples:
	 * <pre>
	 * {@code
	 * // Example 1
	 * public class SimpleCell<T> extends Label implements Cell<T> {
	 *     ...
	 *     ...
	 *
	 *     @Override
	 *     public Node toNode() {
	 *         return this;
	 *     }
	 * }
	 *
	 * // Example 2
	 * public class SimpleCell<T> implements Cell<T> {
	 *     private final Label label = ...;
	 *     ...
	 *     ...
	 *
	 *     @Override
	 *     public Node toNode() {
	 *         return label;
	 *     }
	 * }
	 * </pre>
	 */
	Node toNode();

	/**
	 * Automatically called by the framework when the cell needs to update its index.
	 * <p>
	 * Note though, that there is no 100% guarantee the new given index is different from the current one. If you have some
	 * operations happen when the index changes, for performance reasons, I recommend you to first ensure the new index really
	 * is different.
	 * <p>
	 * See {@link CellBase} and read how this is handled.
	 */
	void updateIndex(int index);

	/**
	 * Automatically called by the framework when the cell needs to update its item.
	 * <p>
	 * Note though, that there is no 100% guarantee the new given item is different from the current one. If you have some
	 * operations happen when the item changes, for performance reasons, I recommend you to first ensure the new item really
	 * is different.
	 * <p>
	 * See {@link CellBase} and read how this is handled.
	 */
	void updateItem(T item);

	/**
	 * Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
	 * should call this when adding the cell to the cache.
	 * <p>
	 * Users can intercept/hook to this operation by overriding this method.
	 */
	default void onCache() {}

	/**
	 * Virtualized containers that make use of a cache to store unneeded cells that may be required again in a second time
	 * should call this when removing the cell from the cache.
	 * <p>
	 * Users can intercept/hook to this operation by overriding this method.
	 */
	default void onDeCache() {}

	/**
	 * Automatically called by the framework when the cell is not needed anymore. The use can override this to perform
	 * some operations before the cell is GCd.
	 */
	default void dispose() {}
}
