package io.github.palexdev.virtualizedfx.cells;

import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;

import java.beans.EventHandler;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Simple skin implementation to be used with any descendant of {@link VFXCellBase}.
 * <p>
 * This will display the data specified by the {@link VFXCellBase#itemProperty()} as a {@code String} in a {@link Label}.
 * It's the only children of this skin, it is positioned according to the {@link VFXCellBase#alignmentProperty()}.
 * Also, the label's graphic property is bound to {@link VFXCellBase#graphicProperty()}.
 * <p>
 * The label's text will be updated on two occasions:
 * <p> 1) when the {@link VFXCellBase#itemProperty()} is invalidated
 * <p> 2) when an event of type {@link VFXContainerEvent#UPDATE} reaches the cell
 * <p>
 * You can modify {@link #addListeners()} to change such behavior. For example, rather than using an {@link InvalidationListener}
 * you could use a {@link ChangeListener} instead, add your own logic, etc. (useful when you want to optimize update performance).
 * <p>
 * (It's recommended to use {@link #listeners(When[])}, {@link #events(WhenEvent[])} and in general {@link When} constructs.
 * Simply because they make your life easier, also disposal would be automatic this way).
 * <p>
 * Last but not least, the label's text is updated by the {@link #update()} method.
 */
public class VFXLabeledCellSkin<T> extends SkinBase<VFXCellBase<T>, CellBaseBehavior<T>> {
	//================================================================================
	// Properties
	//================================================================================
	protected final Label label;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXLabeledCellSkin(VFXCellBase<T> cell) {
		super(cell);

		// Init label
		label = new Label();
		label.graphicProperty().bind(cell.graphicProperty());

		// Finalize init
		addListeners();
		getChildren().add(label);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds an {@link InvalidationListener} on the {@link VFXCellBase#itemProperty()} to call {@link #update()} when it changes,
	 * and an {@link EventHandler} to support "manual" updates through events of type {@link VFXContainerEvent#UPDATE}.
	 * <p>
	 * (Uses {@link When} and {@link WhenEvent} constructs).
	 *
	 * @see #listeners(When[])
	 * @see #events(WhenEvent[])
	 */
	protected void addListeners() {
		VFXCellBase<T> cell = getSkinnable();
		listeners(
			onInvalidated(cell.itemProperty())
				.then(t -> update())
				.executeNow()
		);
		events(
			intercept(cell, VFXContainerEvent.UPDATE)
				.process(e -> update())
		);
	}

	/**
	 * This is responsible for updating the label's text using the value specified by the {@link VFXCellBase#itemProperty()}.
	 * <p>
	 * If the item is {@code null} sets the text to an empty string, otherwise calls {@code toString()} on it.
	 */
	protected void update() {
		VFXCellBase<T> cell = getSkinnable();
		T item = cell.getItem();
		if (item == null) {
			label.setText("");
			return;
		}
		label.setText(item.toString());
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected void initBehavior(CellBaseBehavior<T> behavior) {
		behavior.init();
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + label.prefWidth(-1) + rightInset;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VFXCellBase<T> cell = getSkinnable();
		Pos pos = cell.getAlignment();
		layoutInArea(label, x, y, w, h, 0, pos.getHpos(), pos.getVpos());
	}
}
