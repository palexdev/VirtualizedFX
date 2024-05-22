package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.controls.BoundLabel;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.TextMeasurementCache;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;

import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Default skin implementation for {@link VFXDefaultTableColumn}, extends {@link SkinBase} and uses behaviors of type
 * {@link VFXTableColumnBehavior}.
 * <p>
 * The layout is simple, there are at max three nodes.
 * <p> - a {@link BoundLabel} to show the column's text
 * <p> - a generic {@link Node} specified by the column's {@link VFXTableColumn#graphicProperty()}. It is highly recommended
 * to use this "slot" just for icons
 * <p> - a {@link Region} called 'overlay' which can be used to indicate selection, hovering or other states for the column.
 * This region can be selected in CSS by the selector '.overlay'.
 * <p>
 * See also: {@link VFXDefaultTableColumn#enableOverlayProperty()}, {@link VFXDefaultTableColumn#overlayOnHeaderProperty()}.
 * <p></p>
 * There are three ways to arrange the text and the 'icon' as specified by the {@link VFXDefaultTableColumn#iconAlignmentProperty()}.
 * For {@link HPos#LEFT} and {@link HPos#RIGHT}, the 'icon' is going to be placed to the left and right respectively of the
 * label. For {@link HPos#CENTER} only the 'icon' will be visible at the center of the area, the label will be hidden.
 */
public class VFXDefaultTableColumnSkin<T, C extends VFXTableCell<T>> extends SkinBase<VFXTableColumn<T, C>, VFXTableColumnBehavior<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	private final BoundLabel label;
	private final Region overlay;
	private final TextMeasurementCache tmc;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXDefaultTableColumnSkin(VFXDefaultTableColumn<T, C> column) {
		super(column);

		// Init text node
		label = new BoundLabel(column);
		label.graphicProperty().unbind();
		label.setGraphic(null);
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		tmc = new TextMeasurementCache(column);

		// Init overlay
		overlay = new Region();
		overlay.getStyleClass().add("overlay");
		overlay.setManaged(false);

		// Finalize initialization
		addListeners();
		getChildren().addAll(label, overlay);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds listeners to the following component's properties.
	 * <p> - Listener on the {@link VFXTableColumn#graphicProperty()} to call {@link #handleIcon(Node, Node)} when it changes,
	 * this is also called at init time by using {@link When#executeNow()}
	 * <p> - Listener on the {@link VFXDefaultTableColumn#iconAlignmentProperty()} to update the layout when it changes
	 * <p> - Listener on the {@link VFXTableColumn#gestureResizableProperty()} to enable or disable the feature by calling
	 * {@link VFXTableColumnBehavior#onResizableChanged()}; this is also called at init time by using {@link When#executeNow()}
	 */
	private void addListeners() {
		VFXDefaultTableColumn<T, C> column = getColumn();
		listeners(
			onChanged(column.graphicProperty())
				.then(this::handleIcon)
				.executeNow(),
			onInvalidated(column.iconAlignmentProperty())
				.then(a -> column.requestLayout()),
			onInvalidated(column.gestureResizableProperty())
				.then(r -> getBehavior().onResizableChanged())
		);
	}

	/**
	 * Simply removed the old icon from the children list and adds the new one (after {@code null} checks ofc).
	 */
	protected void handleIcon(Node oldIcon, Node newIcon) {
		if (oldIcon != null) getChildren().remove(oldIcon);
		if (newIcon != null) getChildren().addFirst(newIcon);
	}

	/**
	 * Convenience method to cast {@link #getSkinnable()} to {@link VFXDefaultTableColumn}.
	 */
	protected VFXDefaultTableColumn<T, C> getColumn() {
		return (VFXDefaultTableColumn<T, C>) getSkinnable();
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected void initBehavior(VFXTableColumnBehavior<T, C> behavior) {
		behavior.init();
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		VFXTableColumn<T, C> column = getSkinnable();
		Node icon = column.getGraphic();
		return leftInset +
			((icon != null) ? LayoutUtils.boundWidth(icon) + column.getGraphicTextGap() : 0.0) +
			tmc.getSnappedWidth() +
			rightInset;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VFXDefaultTableColumn<T, C> column = getColumn();
		Node icon = column.getGraphic();
		HPos ia = column.getIconAlignment();
		double gap = column.getGraphicTextGap();

		double iw = 0;
		if (icon != null) {
			switch (ia) {
				case LEFT -> layoutInArea(icon, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
				case CENTER -> layoutInArea(icon, x, y, w, h, 0, HPos.CENTER, VPos.CENTER);
				case RIGHT -> layoutInArea(icon, x, y, w, h, 0, HPos.RIGHT, VPos.CENTER);
			}
			iw = icon.getLayoutBounds().getWidth();
		}

		switch (ia) {
			case LEFT -> {
				layoutInArea(label, x + gap + iw + snappedLeftInset(), y, w - iw - gap, h, 0, HPos.LEFT, VPos.CENTER);
				label.setVisible(true);
			}
			case CENTER -> label.setVisible(false);
			case RIGHT -> {
				layoutInArea(label, x, y, w, h, 0, HPos.LEFT, VPos.CENTER);
				label.setVisible(true);
			}
		}

		// Overlay layout
		VFXTable<T> table = column.getTable();
		double oW = snappedRightInset() + w + snappedLeftInset();
		double oH = (table != null) ? table.getHeight() : 0.0;
		double oY = (column.isOverlayOnHeader()) ? 0.0 : h;
		overlay.resizeRelocate(0.0, oY, oW, oH);
	}
}
