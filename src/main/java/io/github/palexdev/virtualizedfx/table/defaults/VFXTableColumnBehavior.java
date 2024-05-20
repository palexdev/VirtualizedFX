package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.behavior.BehaviorBase;
import io.github.palexdev.mfxcore.enums.Zone;
import io.github.palexdev.mfxcore.utils.resize.RegionDragResizer;
import io.github.palexdev.virtualizedfx.cells.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.scene.input.MouseEvent;

import static io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn.DRAGGED;

/**
 * This is the default behavior implementation for {@link VFXTableColumn}. This basic behavior instantiates a
 * {@link RegionDragResizer} which allows you to resize the column with the mouse cursor at runtime.
 * <p>
 * For the resizer to work, a series of conditions must be met:
 * <p> 1) the feature must be enabled by the {@link VFXTableColumn#gestureResizableProperty()}
 * <p> 2) the table's instance must not be {@code null}
 * <p> 3) the table's layout mode must be set to {@link ColumnsLayoutMode#VARIABLE}.
 */
public class VFXTableColumnBehavior<T, C extends TableCell<T>> extends BehaviorBase<VFXTableColumn<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected RegionDragResizer resizer;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXTableColumnBehavior(VFXTableColumn<T, C> column) {
		super(column);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * This method is responsible for enabling/disabling the {@link RegionDragResizer} by using {@link RegionDragResizer#makeResizable()}
	 * or {@link RegionDragResizer#uninstall()}.
	 * <p>
	 * Beware, this is automatically called by the default skin when needed. Neither the resizer nor this class check whether
	 * it is already enabled, which means that additional calls may add the same handlers multiple times, causing potential
	 * memory leaks!
	 */
	protected void onResizableChanged() {
		VFXTableColumn<T, C> column = getNode();
		boolean resizable = column.isGestureResizable();
		if (!resizable && resizer != null) {
			resizer.uninstall();
			return;
		}
		resizer.makeResizable();
	}

	/**
	 * The {@link RegionDragResizer} used here is an inline custom extension which uses this method to determine whether
	 * the column can be resized or not.
	 */
	protected boolean canResize() {
		VFXTableColumn<T, C> column = getNode();
		VFXTable<T> table = column.getTable();
		return table != null && table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE;
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public void init() {
		VFXTableColumn<T, C> column = getNode();
		resizer = new RegionDragResizer(column) {
			@Override
			protected void handleDragged(MouseEvent event) {
				if (canResize()) {
					super.handleDragged(event);
					column.pseudoClassStateChanged(DRAGGED, true);
				}
				if (column.getTable() == null || column.getTable().getColumnsLayoutMode() == ColumnsLayoutMode.FIXED)
					return;
			}

			@Override
			protected void handleMoved(MouseEvent event) {
				if (canResize()) super.handleMoved(event);
				if (column.getTable() == null || column.getTable().getColumnsLayoutMode() == ColumnsLayoutMode.FIXED)
					return;
			}

			@Override
			protected void handlePressed(MouseEvent event) {
				if (canResize()) super.handlePressed(event);
				if (column.getTable() == null || column.getTable().getColumnsLayoutMode() == ColumnsLayoutMode.FIXED)
					return;
			}

			@Override
			protected void handleReleased(MouseEvent event) {
				super.handleReleased(event);
				column.pseudoClassStateChanged(DRAGGED, false);
			}
		};
		resizer.setMinWidthFunction(r -> column.getTable().getColumnsSize().getWidth());
		resizer.setAllowedZones(Zone.CENTER_RIGHT);
		resizer.setResizeHandler((node, x, y, w, h) -> column.resize(w));
		if (column.isGestureResizable()) resizer.makeResizable();
	}

	/**
	 * {@inheritDoc}
	 * <p></p>
	 * Also disposed the {@link RegionDragResizer}.
	 */
	@Override
	public void dispose() {
		resizer.dispose();
		resizer = null;
		super.dispose();
	}
}
