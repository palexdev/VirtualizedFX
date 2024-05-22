package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.behavior.BehaviorBase;

/**
 * Base, empty behavior for cells of type {@link VFXCellBase}, extends {@link BehaviorBase}.
 *
 * @param <T> the type of item displayed by the cell
 */
public class CellBaseBehavior<T> extends BehaviorBase<VFXCellBase<T>> {

	//================================================================================
	// Constructors
	//================================================================================
	public CellBaseBehavior(VFXCellBase<T> cell) {
		super(cell);
	}
}
