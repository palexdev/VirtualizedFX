package io.github.palexdev.virtualizedfx.table.defaults;

import io.github.palexdev.mfxcore.base.bindings.MFXBindings;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.TextUtils;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.controls.BoundLabel;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import javafx.beans.InvalidationListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Default skin implementation for {@link DefaultTableColumn}.
 * <p></p>
 * The layout is quite easy, it is composed by an {@link HBox} which simplifies the positioning of the text and the icon,
 * and a {@link BoundLabel}. The icon is placed according to the {@link DefaultTableColumn#iconAlignmentProperty()}.
 * <p></p>
 * This skin implementation also has an extra node called "overlay" (the style class for CSS is also "overlay") which
 * can be useful to highlight the column in certain occasions (like column selection or mouse hover).
 * <p>
 * One thing to keep in mind though is that if you define a background color for the overlay make sure that it is
 * opaque otherwise it will end up covering the rows cells.
 * <p></p>
 * A little side note:
 * <p>
 * In the source code of this class is also available an incomplete but really promising example of Drag-and-Drop
 * feature to move columns around. I ended up not implementing it to not overcomplicate things, still I don't want to
 * remove that code since it really is a promising implementation.
 */
public class DefaultTableColumnSkin<T, C extends TableCell<T>> extends SkinBase<DefaultTableColumn<T, C>> {
	//================================================================================
	// Properties
	//================================================================================
	private final HBox box;
	private final BoundLabel label;
	private final Region overlay;

	//================================================================================
	// Constructors
	//================================================================================
	public DefaultTableColumnSkin(DefaultTableColumn<T, C> column) {
		super(column);

		// Init text node
		label = new BoundLabel(column);
		label.graphicProperty().unbind();
		label.setGraphic(null);
		label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		HBox.setHgrow(label, Priority.ALWAYS);

		// Init container and icon
		box = new HBox(label);
		box.setAlignment(Pos.CENTER);
		box.spacingProperty().bind(label.graphicTextGapProperty());
		initIcon();

		// Init overlay
		overlay = new Region();
		overlay.getStyleClass().add("overlay");
		overlay.setManaged(false);

		// Finalize initialization
		getChildren().addAll(box, overlay);
		addListeners();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void addListeners() {
		DefaultTableColumn<T, C> column = getSkinnable();
		column.graphicProperty().addListener((observable, oldValue, newValue) -> {
			if (oldValue != null) box.getChildren().remove(oldValue);
			initIcon();
		});
		column.iconAlignmentProperty().addListener(invalidated -> initIcon());

		column.cellFactoryProperty().addListener(invalidated -> {
			VirtualTable<T> table = column.getTable();
			table.onColumnChangedFactory(column);
		});

		/*
		 * JavaFX caches the size computations, requestLayout() resets the cache
		 */
		VirtualTable<T> table = column.getTable();
		InvalidationListener layoutListener = invalidated -> column.requestLayout();
		table.heightProperty().addListener(layoutListener);
		table.columnsLayoutModeProperty().addListener(layoutListener);
	}

	private void initIcon() {
		DefaultTableColumn<T, C> column = getSkinnable();
		Node icon = column.getGraphic();
		if (icon == null) return;

		box.getChildren().remove(icon); // Make sure it's not already in box
		HPos iconAlignment = column.getIconAlignment();
		if (iconAlignment == HPos.LEFT) {
			box.getChildren().add(0, icon);
		} else {
			box.getChildren().add(icon);
		}
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		DefaultTableColumn<T, C> column = getSkinnable();
		VirtualTable<T> table = column.getTable();
		if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) {
			return Math.max(LayoutUtils.boundWidth(box), table.getColumnSize().getWidth());
		}
		return super.computeMinWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		DefaultTableColumn<T, C> column = getSkinnable();
		VirtualTable<T> table = column.getTable();
		Node icon = column.getGraphic();
		double gap = column.getGraphicTextGap();
		if (table.getColumnsLayoutMode() == ColumnsLayoutMode.VARIABLE) {
			return leftInset + TextUtils.computeLabelWidth(label) + (icon != null ? icon.prefWidth(-1) + gap : 0) + rightInset;
		}
		return super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		DefaultTableColumn<T, C> column = getSkinnable();
		super.layoutChildren(x, y, w, h);

		VirtualTable<T> table = column.getTable();
		double oW = snappedRightInset() + w + snappedLeftInset();
		double oH = (table != null) ? table.getHeight() : 0.0;
		double oY = (column.isOverlayOnHeader()) ? 0.0 : h;
		overlay.resizeRelocate(0.0, oY, oW, oH);
	}

	@Override
	public void dispose() {
		DefaultTableColumn<T, C> column = getSkinnable();
		MFXBindings.instance().dispose(column.inViewportProperty());
		super.dispose();
	}

		 /*
		// Promising, experimental implementation of Drag-and-Drop feature
		column.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
			dragStart = e.getX();
		});
		column.addEventHandler(MouseEvent.DRAG_DETECTED, e -> {
			System.out.println("Column is being dragged");
			SnapshotWrapper wrapper = new SnapshotWrapper(column);
			Node graphic = wrapper.getGraphic();
			popup.getContent().setAll(graphic);
			popup.show(
					column.getScene().getWindow(),
					e.getScreenX() - popup.getWidth() / 2,
					column.localToScreen(column.getLayoutBounds()).getMinY());
		});
		column.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			if (!popup.isShowing()) return;
			System.out.println(e.getEventType());
			popup.setAnchorX(e.getScreenX() - popup.getWidth() / 2);
			popup.setAnchorY(column.localToScreen(column.getLayoutBounds()).getMinY());


		});
		column.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
			System.out.println("Drag Dropped");

			VirtualTable<T> table = column.getTable();
			TableState<T> state = table.getState();
			List<Column<T, ? extends TableCell<T>>> columns = table.getColumns().subList(
					state.getColumnsRange().getMin(),
					state.getColumnsRange().getMax() + 1
			);

			Parent parent = column.getParent();
			Point2D local = parent.screenToLocal(e.getScreenX(), e.getScreenY());
			System.out.println("Mouse at: " + local);

			double closestValueTo = NumberUtils.closestValueTo(
					local.getX(),
					columns.stream().map(c -> c.getRegion().getBoundsInParent().getMaxX()).collect(Collectors.toList())
			);
			System.out.println("Closest is: " + closestValueTo);
			// This seems to work pretty well

			popup.hide();
		});
		*/
}
