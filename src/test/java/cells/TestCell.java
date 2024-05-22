package cells;

import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.virtualizedfx.cells.VFXLabeledCellSkin;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleCell;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static utils.TestFXUtils.counter;

public class TestCell<T> extends VFXSimpleCell<T> {
	public TestCell(T item) {
		super(item);
	}

	@Override
	public void onDeCache() {
		counter.fCache();
	}

	@Override
	public void onCache() {
		counter.tCache();
	}

	@Override
	public void dispose() {
		counter.disposed();
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXLabeledCellSkin<>(this) {
			{
				setConverter(t -> {
					int index = getIndex();
					return "Index: %d Item: %s".formatted(
						index,
						t != null ? t.toString() : ""
					);
				});
				setStyle("-fx-border-color: red");
				update();
				counter.index();
				counter.item();
			}

			@Override
			protected void addListeners() {
				listeners(
					onInvalidated(indexProperty())
						.then(v -> {
							counter.index();
							update();
						}),
					onInvalidated(itemProperty())
						.then(v -> {
							counter.item();
							update();
						})
				);
				events(
					intercept(TestCell.this, VFXContainerEvent.UPDATE)
						.process(e -> {
							update();
							e.consume();
						})
				);
			}

			@Override
			protected void update() {
				T item = getItem();
				label.setText(getConverter().toString(item));
			}
		};
	}
}
