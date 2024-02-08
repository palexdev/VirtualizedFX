package io.github.palexdev.virtualizedfx.grid;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.GridUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.virtualizedfx.cells.Cell;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.SequencedMap;

import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

public class VFXGridSkin<T, C extends Cell<T>> extends SkinBase<VFXGrid<T, C>, VFXGridManager<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	protected final Pane viewport;
	protected final Rectangle clip;
	protected double DEFAULT_SIZE = 100.0;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXGridSkin(VFXGrid<T, C> grid) {
		super(grid);

		// Init viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				VFXGridSkin.this.layout();
			}
		};
		viewport.getStyleClass().add("viewport");

		// Init clip
		clip = new Rectangle();
		clip.widthProperty().bind(grid.widthProperty());
		clip.heightProperty().bind(grid.heightProperty());
		clip.arcWidthProperty().bind(grid.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(grid.clipBorderRadiusProperty());
		grid.setClip(clip);

		// End initialization
		addListeners();
		getChildren().add(viewport);
	}

	//================================================================================
	// Methods
	//================================================================================
	protected void addListeners() {
		VFXGrid<T, C> grid = getSkinnable();
		listeners(
			// Core changes
			onInvalidated(grid.stateProperty())
				.then(s -> {
					if (s == VFXGridState.EMPTY) {
						viewport.getChildren().clear();
					} else if (s.haveCellsChanged()) {
						viewport.getChildren().setAll(s.getNodes());
						grid.requestViewportLayout();
					}
				}),
			onInvalidated(grid.needsViewportLayoutProperty())
				.condition(v -> v)
				.then(v -> layout()),
			onChanged(grid.helperProperty())
				.then((o, n) -> {
					viewport.translateXProperty().bind(n.viewportPositionProperty().map(Position::getX));
					viewport.translateYProperty().bind(n.viewportPositionProperty().map(Position::getY));
				})
				.executeNow(),

			// Geometry changes
			onInvalidated(grid.widthProperty())
				.then(w -> getBehavior().onGeometryChanged()),
			onInvalidated(grid.heightProperty())
				.then(h -> getBehavior().onGeometryChanged()),
			onInvalidated(grid.bufferSizeProperty())
				.then(b -> getBehavior().onGeometryChanged()),

			// Position changes
			onInvalidated(grid.vPosProperty())
				.then(h -> getBehavior().onPositionChanged(Orientation.VERTICAL)),
			onInvalidated(grid.hPosProperty())
				.then(v -> getBehavior().onPositionChanged(Orientation.HORIZONTAL)),

			// Others
			onInvalidated(grid.columnsNumProperty())
				.then(n -> getBehavior().onColumnsNumChanged()),
			onInvalidated(grid.cellFactoryProperty())
				.then(f -> getBehavior().onCellFactoryChanged()),
			onInvalidated(grid.cellSizeProperty())
				.then(s -> getBehavior().onCellSizeChanged()),
			onInvalidated(grid.hSpacingProperty())
				.then(s -> getBehavior().onSpacingChanged()),
			onInvalidated(grid.vSpacingProperty())
				.then(s -> getBehavior().onSpacingChanged()),
			onInvalidated(grid.itemsProperty())
				.then(it -> getBehavior().onItemsChanged())
		);
	}

	protected void layout() {
		VFXGrid<T, C> grid = getSkinnable();
		if (!grid.isNeedsViewportLayout()) return;

		VFXGridHelper<T, C> helper = grid.getHelper();
		VFXGridState<T, C> state = grid.getState();
		int nColumns = helper.maxColumns();
		if (state != VFXGridState.EMPTY) {
			SequencedMap<Integer, C> cells = state.getCellsByIndex();
			int i = 0, j = 0;
			outer_loop:
			for (Integer rIdx : state.getRowsRange()) {
				for (Integer cIdx : state.getColumnsRange()) {
					int linear = GridUtils.subToInd(nColumns, rIdx, cIdx);
					if (linear >= grid.size()) break outer_loop;
					helper.layout(i, j, cells.get(linear).toNode());
					j++;
				}
				i++;
				j = 0;
			}
			onLayoutCompleted(true);
			return;
		}
		onLayoutCompleted(false);
	}

	/**
	 * This method is <b>crucial</b> because it resets the {@link VFXGrid#needsViewportLayoutProperty()} to false.
	 * If you override this method or the {@link #layout()}, remember to call this!
	 *
	 * @param done this parameter can be useful to overriders as it gives information on whether the {@link #layout()}
	 *             was executed correctly
	 */
	protected void onLayoutCompleted(boolean done) {
		VFXGrid<T, C> grid = getSkinnable();
		grid.setNeedsViewportLayout(false);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected void initBehavior(VFXGridManager<T, C> behavior) {

	}

	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + DEFAULT_SIZE + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return topInset + DEFAULT_SIZE + bottomInset;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VFXGrid<T, C> grid = getSkinnable();
		VFXGridHelper<T, C> helper = grid.getHelper();
		IntegerRange cRange = helper.columnsRange();
		IntegerRange rRange = helper.rowsRange();
		double vw = ((cRange.diff() + 1) * helper.getTotalCellSize().getWidth()) - grid.getHSpacing();
		double vh = ((rRange.diff() + 1) * helper.getTotalCellSize().getHeight()) - grid.getVSpacing();

		viewport.resize(vw, vh);
		Position pos = LayoutUtils.computePosition(
			grid, viewport,
			0, 0, grid.getWidth(), grid.getHeight(), 0, Insets.EMPTY,
			grid.getAlignment().getHpos(), grid.getAlignment().getVpos(),
			false, false
		);
		viewport.relocate(
			Math.max(0, pos.getX()),
			Math.max(0, pos.getY())
		);
	}
}
