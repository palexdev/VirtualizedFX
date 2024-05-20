package io.github.palexdev.virtualizedfx.enums;

import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper.VariableTableHelper;

/**
 * Enumerator to specify the layout modes for columns in {@link VFXTable}.
 */
public enum ColumnsLayoutMode {

	/**
	 * In this mode, all columns will have the same width specified by {@link VFXTable#columnsSizeProperty()}.
	 */
	FIXED,

	/**
	 * In this mode, columns are allowed to have different widths. This enables features like:
	 * columns auto-sizing ({@link VariableTableHelper#autosizeColumn(VFXTableColumn)}), or resizing at runtime
	 * through gestures.
	 * <p>
	 * A downside of such mode is that basically, virtualization along the x-axis is disabled. Which means that all columns
	 * will be added to the viewport. Internal optimizations should make this issue less impactful on performance.
	 */
	VARIABLE,
	;

	public static ColumnsLayoutMode next(ColumnsLayoutMode mode) {
		return EnumUtils.next(ColumnsLayoutMode.class, mode);
	}
}
