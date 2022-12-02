/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package interactive.flow;

import interactive.cells.flow.AlternativeCell;
import interactive.cells.flow.DetailedCell;
import interactive.others.Constraint;
import interactive.others.Utils;
import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import io.github.palexdev.virtualizedfx.unused.simple.SimpleVirtualFlow;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.layout.Region;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static interactive.others.DialogUtils.*;

public enum FlowTestActions {
	UPDATE_AT_5((p, o, n) -> {
		Runnable oAction = () -> o.getItems().set(5, RandomUtils.random.nextInt(0, 9999));
		Runnable nAction = () -> n.getItems().set(5, RandomUtils.random.nextInt(0, 9999));
		run(p, oAction, nAction);
	}),
	UPDATE_AT_25((p, o, n) -> {
		Runnable oAction = () -> o.getItems().set(25, RandomUtils.random.nextInt(0, 9999));
		Runnable nAction = () -> n.getItems().set(25, RandomUtils.random.nextInt(0, 9999));
		run(p, oAction, nAction);
	}),
	ADD_AT_0((p, o, n) -> {
		Runnable oAction = () -> o.getItems().add(0, RandomUtils.random.nextInt(0, 9999));
		Runnable nAction = () -> n.getItems().add(0, RandomUtils.random.nextInt(0, 9999));
		run(p, oAction, nAction);
	}),
	ADD_AT_END((p, o, n) -> {
		Runnable oAction = () -> o.getItems().add(o.getItems().size(), RandomUtils.random.nextInt(0, 9999));
		Runnable nAction = () -> n.getItems().add(n.getItems().size(), RandomUtils.random.nextInt(0, 9999));
		run(p, oAction, nAction);
	}),
	ADD_AT((p, o, n) -> {
		int index = getIntFromUser(p.getRoot(), "Add at...", "Index to add at", Constraint.of("Invalid!", i -> i >= 0 && i <= o.getItems().size() && i <= n.getItems().size()));
		Runnable oAction = () -> o.getItems().add(index, RandomUtils.random.nextInt(0, 9999));
		Runnable nAction = () -> n.getItems().add(index, RandomUtils.random.nextInt(0, 9999));
		run(p, oAction, nAction);
	}),
	ADD_MULTIPLE_AT_3((p, o, n) -> {
		List<Integer> integers = List.of(
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999)
		);
		Runnable oAction = () -> o.getItems().addAll(3, integers);
		Runnable nAction = () -> n.getItems().addAll(3, integers);
		run(p, oAction, nAction);
	}),
	DELETE_AT_3((p, o, n) -> {
		Runnable oAction = () -> o.getItems().remove(3);
		Runnable nAction = () -> n.getItems().remove(3);
		run(p, oAction, nAction);
	}),
	DELETE_SPARSE((p, o, n) -> {
		Runnable oAction = () -> o.getItems().removeAll(
				Utils.listGetAll(o.getItems(), 2, 5, 6, 8, 25, 53)
		);
		Runnable nAction = () -> n.getItems().removeAll(
				Utils.listGetAll(n.getItems(), 2, 5, 6, 8, 25, 53)
		);
		run(p, oAction, nAction);
	}),
	DELETE_FIRST((p, o, n) -> {
		Runnable oAction = () -> o.getItems().remove(0);
		Runnable nAction = () -> n.getItems().remove(0);
		run(p, oAction, nAction);
	}),
	DELETE_LAST((p, o, n) -> {
		Runnable oAction = () -> o.getItems().remove(o.getItems().size() - 1);
		Runnable nAction = () -> n.getItems().remove(n.getItems().size() - 1);
		run(p, oAction, nAction);
	}),
	DELETE_ALL_VISIBLE((p, o, n) -> {
		Runnable oAction = () -> {
			Integer[] integers = o.getCells().keySet().stream()
					.map(i -> o.getItems().get(i))
					.toArray(Integer[]::new);
			o.getItems().removeAll(integers);
		};
		Runnable nAction = () -> {
			Integer[] integers = n.getIndexedCells().keySet().stream()
					.map(i -> n.getItems().get(i))
					.toArray(Integer[]::new);
			n.getItems().removeAll(integers);
		};
		run(p, oAction, nAction);
	}),
	REPLACE_ALL((p, o, n) -> {
		List<Integer> integers = IntStream.rangeClosed(0, 50)
				.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
				.toList();
		Runnable oAction = () -> o.getItems().setAll(integers);
		Runnable nAction = () -> n.getItems().setAll(integers);
		run(p, oAction, nAction);
	}),
	REVERSE_SORT((p, o, n) -> {
		Runnable oAction = () -> o.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2));
		Runnable nAction = () -> n.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2));
		run(p, oAction, nAction);
	}),
	CLEAR_LIST((p, o, n) -> {
		Runnable oAction = () -> o.getItems().clear();
		Runnable nAction = () -> n.getItems().clear();
		run(p, oAction, nAction);
	}),
	CHANGE_VIEWPORT_SIZE_TO((p, o, n) -> {
		Runnable oAction = () -> {
			String fText = (o.getOrientation() == Orientation.VERTICAL) ?
					"Current height: " + o.getHeight() :
					"Current width: " + o.getWidth();
			double value = getSizeFromUser(p.getRoot(), "Change Viewport Size To...", fText, Constraint.of("Invalid!", i -> i > 0));
			if (value == -1.0) return;

			if (o.getOrientation() == Orientation.VERTICAL) {
				o.setPrefHeight(value);
				o.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
			} else {
				o.setPrefWidth(value);
				o.setMaxSize(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE);
			}
		};
		Runnable nAction = () -> {
			VirtualScrollPane vsp = (VirtualScrollPane) n.getParent().getParent();
			String fText = (n.getOrientation() == Orientation.VERTICAL) ?
					"Current height: " + vsp.getHeight() :
					"Current width: " + vsp.getWidth();
			double value = getSizeFromUser(p.getRoot(), "Change Viewport Size To...", fText, Constraint.of("Invalid!", i -> i > 0));
			if (value == -1.0) return;

			if (n.getOrientation() == Orientation.VERTICAL) {
				vsp.setPrefHeight(value);
				vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
			} else {
				vsp.setPrefWidth(value);
				vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
			}
		};
		run(p, oAction, nAction);
	}),
	RESET_VIEWPORT_SIZE((p, o, n) -> {
		Runnable oAction = () -> {
			o.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			o.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		};
		Runnable nAction = () -> {
			VirtualScrollPane vsp = (VirtualScrollPane) n.getParent().getParent();
			vsp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		};
		run(p, oAction, nAction);
	}),
	CHANGE_CELLS_SIZE_TO((p, o, n) -> {
		Runnable oAction = () -> {
			String details = """
					The chosen action: "Change cells size to" is not supported by the old implementation
					since the cells' fixed size had to be hard-coded in the cells source code
					""";
			showDialog(p.getRoot(), DialogType.ERROR, "Unsupported Operation...", details);
		};
		Runnable nAction = () -> {
			double value = getSizeFromUser(p.getRoot(), "Change Cells Size To...", "Current Size: " + n.getCellSize(), Constraint.of("Invalid!", i -> i > 0));
			if (value == -1.0) return;
			n.setCellSize(value);
		};
		run(p, oAction, nAction);
	}),
	REPLACE_LIST((p, o, n) -> {
		Runnable oAction = () -> {
			ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
					.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
					.collect(FXCollectors.toList());
			o.setItems(integers);
		};
		Runnable nAction = () -> {
			ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
					.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
					.collect(FXCollectors.toList());
			n.setItems(integers);
		};
		run(p, oAction, nAction);
	}),
	SWITCH_CELLS_FACTORY((p, o, n) -> {
		Runnable oAction = () -> {
			Class<? extends Cell<?>> lastFactory = p.getLastCell(0);
			Function<Integer, Cell<Integer>> factory;
			if (lastFactory == DetailedCell.class) {
				factory = AlternativeCell::new;
				p.setLastCell(0, AlternativeCell.class);
			} else {
				factory = DetailedCell::new;
				p.setLastCell(0, DetailedCell.class);
			}
			o.setCellFactory(factory);
		};
		Runnable nAction = () -> {
			Class<? extends Cell<?>> lastFactory = p.getLastCell(1);
			Function<Integer, Cell<Integer>> factory;
			if (lastFactory == DetailedCell.class) {
				factory = AlternativeCell::new;
				p.setLastCell(1, AlternativeCell.class);
			} else {
				factory = DetailedCell::new;
				p.setLastCell(1, DetailedCell.class);
			}
			n.setCellFactory(factory);
		};
		run(p, oAction, nAction);
	}),
	SWITCH_ORIENTATION((p, o, n) -> {
		Runnable oAction = () -> {
			Orientation newOr;
			if (o.getOrientation() == Orientation.VERTICAL) {
				newOr = Orientation.HORIZONTAL;
				o.setOrientation(newOr);
			} else {
				newOr = Orientation.VERTICAL;
				o.setOrientation(newOr);
			}
			String details = """
					This test works partially for the old implementation as the cells size
					cannot be changed due to the fact it was hard-coded in the cells source code
					""";
			showDialog(p.getRoot(), DialogType.WARNING, "Warning...", details);
		};
		Runnable nAction = () -> {
			Orientation newOr;
			if (n.getOrientation() == Orientation.VERTICAL) {
				newOr = Orientation.HORIZONTAL;
				n.setOrientation(newOr);
				n.setCellSize(100);
			} else {
				newOr = Orientation.VERTICAL;
				n.setOrientation(newOr);
				n.setCellSize(64);
			}
		};
		run(p, oAction, nAction);
	}),
	TEST_VARIABLE_BREADTH((p, o, n) -> {
		Runnable oAction = () -> {
			String details = """
					This feature was not available in the old implementation as all
					cells were automatically adjusted to fit the viewport.
					""";
			showDialog(p.getRoot(), DialogType.WARNING, "Warning...", details);
		};
		Runnable nAction = () -> {
			if (!n.isFitToBreadth()) {
				n.setFitToBreadth(true);
				return;
			}

			Orientation or = n.getOrientation();
			Function<Integer, Cell<Integer>> factory;
			if (or == Orientation.VERTICAL) {
				factory = i -> new DetailedCell(i) {
					{
						setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
					}

					@Override
					protected double computePrefWidth(double height) {
						double def = n.getWidth();
						int delta = RandomUtils.random.nextInt(0, 100);
						boolean mul = RandomUtils.random.nextBoolean();
						return def + (mul ? delta : -delta);
					}
				};
			} else {
				factory = i -> new DetailedCell(i) {
					{
						setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
					}

					@Override
					protected double computePrefHeight(double width) {
						double def = n.getHeight();
						int delta = RandomUtils.random.nextInt(0, 100);
						boolean mul = RandomUtils.random.nextBoolean();
						return def + (mul ? delta : -delta);
					}
				};
			}
			n.setCellFactory(factory);
			n.setFitToBreadth(false);
		};
		run(p, oAction, nAction);
	}),
	SCROLL_BY_64PX((p, o, n) -> {
		Runnable oAction = () -> o.scrollBy(64.0);
		Runnable nAction = () -> n.scrollBy(64.0);
		run(p, oAction, nAction);
	}),
	SCROLL_TO_64PX((p, o, n) -> {
		Runnable oAction = () -> o.scrollToPixel(64.0);
		Runnable nAction = () -> n.scrollToPixel(64.0);
		run(p, oAction, nAction);
	}),
	SCROLL_TO_INDEX_20((p, o, n) -> {
		Runnable oAction = () -> o.scrollTo(20);
		Runnable nAction = () -> n.scrollToIndex(20);
		run(p, oAction, nAction);
	}),
	SCROLL_TO_FIRST((p, o, n) -> {
		Runnable oAction = o::scrollToFirst;
		Runnable nAction = n::scrollToFirst;
		run(p, oAction, nAction);
	}),
	SCROLL_TO_LAST((p, o, n) -> {
		Runnable oAction = o::scrollToLast;
		Runnable nAction = n::scrollToLast;
		run(p, oAction, nAction);
	});

	private final Action action;

	FlowTestActions(Action action) {
		this.action = action;
	}

	public Action getAction() {
		return action;
	}

	public void run(FlowTestParameters p, SimpleVirtualFlow<Integer, Cell<Integer>> o, VirtualFlow<Integer, Cell<Integer>> n) {
		action.accept(p, o, n);
	}

	static void run(FlowTestParameters p, Runnable o, Runnable n) {
		switch (p.getMode()) {
			case NEW_IMP -> n.run();
			case OLD_IMP -> o.run();
			case BOTH -> {
				o.run();
				n.run();
			}
		}
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
	}

	@FunctionalInterface
	interface Action {
		void accept(FlowTestParameters p, SimpleVirtualFlow<Integer, Cell<Integer>> o, VirtualFlow<Integer, Cell<Integer>> n);
	}
}
