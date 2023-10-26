package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.behavior.BehaviorBase;

/**
 * Base, empty behavior for cells of type {@link CellBase}, extends {@link BehaviorBase}.
 *
 * @param <T> the type of item displayed by the cell
 */
public class CellBaseBehavior<T> extends BehaviorBase<CellBase<T>> {

	//================================================================================
	// Constructors
	//================================================================================
	public CellBaseBehavior(CellBase<T> cell) {
		super(cell);
	}
}
