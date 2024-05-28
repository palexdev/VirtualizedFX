package io.github.palexdev.virtualizedfx.base;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.grid.VFXGrid;
import io.github.palexdev.virtualizedfx.list.VFXList;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import javafx.geometry.Orientation;

/**
 * Interface to quickly wrap a content in a {@link VFXScrollPane} and make it scrollable. How the wrapping is done
 * depends on the implementing class.
 * <p></p>
 * Since this is mostly intended for virtualized components (but not limited to!!!), there are also a bunch of util methods
 * specifically for {@link VFXContainer}.
 */
public interface VFXScrollable {
	/**
	 * This multiplier is applied to the unit increment of a {@link VFXScrollPane} to determine the track increment.
	 * <p>
	 * So, if I have a unit increment of 30px, pressing on the track would result in a (30px * mul)px scroll.
	 */
	double TRACK_MULTIPLIER = 5.0;

	/**
	 * Wraps this in a {@link VFXScrollPane} to enable scrolling.
	 */
	VFXScrollPane makeScrollable();

	/**
	 * Sets the unit increment and track increment for both scroll bars of the given {@link VFXScrollPane}, optionally
	 * binds the properties if the {@code bind} parameter is {@code true}.
	 * <p>
	 * The unit increment is set to sizes specified by {@link VFXGrid#cellSizeProperty()} (width/height according to the scroll bar orientation).
	 * <p>
	 * The track increment is set to be the unit increment multiplied by {@link #TRACK_MULTIPLIER} (you can change it since
	 * it's a public variable).
	 */
	static void setSpeed(VFXScrollPane vsp, VFXGrid<?, ?> grid, boolean bind) {
		if (bind) {
			vsp.hUnitIncrementProperty().bind(grid.cellSizeProperty().map(Size::getWidth));
			vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
			vsp.vUnitIncrementProperty().bind(grid.cellSizeProperty().map(Size::getHeight));
			vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
			return;
		}
		vsp.setHUnitIncrement(grid.getCellSize().getWidth());
		vsp.setHTrackIncrement(grid.getCellSize().getWidth() * 1.5);
		vsp.setVUnitIncrement(grid.getCellSize().getHeight());
		vsp.setVTrackIncrement(grid.getCellSize().getHeight() * 1.5);
	}

	/**
	 * Sets the unit increment and track increment for both scroll bars of the given {@link VFXScrollPane}, optionally
	 * binds the properties if the {@code bind} parameter is {@code true}.
	 * <p>
	 * The unit increment is set to according to the {@link VFXList#orientationProperty()} and is the value specified by
	 * {@link VFXList#cellSizeProperty()}. The increment for the opposite orientation is set to a default of 30px.
	 * <p>
	 * The track increment is set to be the unit increment multiplied by {@link #TRACK_MULTIPLIER} (you can change it since
	 * it's a public variable).
	 */
	static void setSpeed(VFXScrollPane vsp, VFXList<?, ?> list, boolean bind) {
		if (bind) {
			vsp.hUnitIncrementProperty().bind(DoubleBindingBuilder.build()
				.setMapper(() -> (list.getOrientation() == Orientation.HORIZONTAL) ? list.getCellSize() : 30.0)
				.addSources(list.orientationProperty(), list.cellSizeProperty())
				.get()
			);
			vsp.hTrackIncrementProperty().bind(vsp.hUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
			vsp.vUnitIncrementProperty().bind(DoubleBindingBuilder.build()
				.setMapper(() -> (list.getOrientation() == Orientation.VERTICAL) ? list.getCellSize() : 30.0)
				.addSources(list.orientationProperty(), list.cellSizeProperty())
				.get()
			);
			vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
			return;
		}
		Orientation o = list.getOrientation();
		if (o == Orientation.VERTICAL) {
			vsp.setVUnitIncrement(list.getCellSize());
			vsp.setVTrackIncrement(list.getCellSize() * TRACK_MULTIPLIER);
			vsp.setHUnitIncrement(30.0);
			vsp.setHTrackIncrement(45.0);
		} else {
			vsp.setHUnitIncrement(list.getCellSize());
			vsp.setHTrackIncrement(list.getCellSize() * TRACK_MULTIPLIER);
			vsp.setVUnitIncrement(30.0);
			vsp.setVTrackIncrement(45.0);
		}
	}

	/**
	 * Sets the unit increment and track increment for both scroll bars of the given {@link VFXScrollPane}, optionally
	 * binds the properties if the {@code bind} parameter is {@code true}.
	 * <p>
	 * The horizontal unit increment is set to half the width specified by {@link VFXTable#columnsSizeProperty()}, while
	 * the track increment is the whole size.
	 * <p>
	 * The vertical unit increment is set to {@link VFXTable#rowsHeightProperty()}, while the track increment is the same
	 * value multiplied by {@link #TRACK_MULTIPLIER}.
	 */
	static void setSpeed(VFXScrollPane vsp, VFXTable<?> table, boolean bind) {
		if (bind) {
			vsp.hUnitIncrementProperty().bind(table.columnsSizeProperty().map(s -> s.getWidth() * 0.5));
			vsp.hTrackIncrementProperty().bind(table.columnsSizeProperty().map(Size::getWidth));
			vsp.vUnitIncrementProperty().bind(table.rowsHeightProperty());
			vsp.vTrackIncrementProperty().bind(vsp.vUnitIncrementProperty().multiply(TRACK_MULTIPLIER));
			return;
		}
		vsp.setHUnitIncrement(table.getColumnsSize().getWidth() * 0.5);
		vsp.setHTrackIncrement(table.getColumnsSize().getWidth());
		vsp.setVUnitIncrement(table.getRowsHeight());
		vsp.setVTrackIncrement(table.getRowsHeight() * TRACK_MULTIPLIER);
	}
}
