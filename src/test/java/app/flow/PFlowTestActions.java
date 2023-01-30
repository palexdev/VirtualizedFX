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

package app.flow;

import app.cells.flow.AlternativeCell;
import app.cells.flow.DetailedCell;
import app.others.Constraint;
import app.others.Utils;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static app.others.DialogUtils.getIntFromUser;
import static app.others.DialogUtils.getSizeFromUser;

public enum PFlowTestActions {
	UPDATE_AT_5((p, f) -> run(() -> f.getItems().set(5, RandomUtils.random.nextInt(0, 9999)))),
	UPDATE_AT_25((p, f) -> run(() -> f.getItems().set(25, RandomUtils.random.nextInt(0, 9999)))),
	ADD_AT_0((p, f) -> run(() -> f.getItems().add(0, RandomUtils.random.nextInt(0, 9999)))),
	ADD_AT_END((p, f) -> run(() -> f.getItems().add(f.getItems().size(), RandomUtils.random.nextInt(0, 9999)))),
	ADD_AT((p, f) -> {
		int index = getIntFromUser(p.getRoot(), "Add at...", "Index to add at", Constraint.listIndexConstraint(f.getItems(), true));
		run(() -> f.getItems().add(index, RandomUtils.random.nextInt(0, 9999)));
	}),
	ADD_MULTIPLE_AT_3((p, f) -> {
		List<Integer> integers = List.of(
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999),
				RandomUtils.random.nextInt(0, 9999)
		);
		run(() -> f.getItems().addAll(3, integers));
	}),
	DELETE_AT_3((p, f) -> run(() -> f.getItems().remove(3))),
	DELETE_SPARSE((p, f) -> run(() -> f.getItems().removeAll(
			Utils.listGetAll(f.getItems(), 2, 5, 6, 8, 25, 53)
	))),
	DELETE_FIRST((p, f) -> run(() -> f.getItems().remove(0))),
	DELETE_LAST((p, f) -> run(() -> f.getItems().remove(f.getItems().size() - 1))),
	DELETE_ALL_VISIBLE((p, f) -> run(() -> {
		Integer[] integers = f.getIndexedCells().keySet().stream()
				.map(i -> f.getItems().get(i))
				.toArray(Integer[]::new);
		f.getItems().removeAll(integers);
	})),
	REPLACE_ALL((p, f) -> {
		List<Integer> integers = IntStream.rangeClosed(0, 50)
				.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
				.toList();
		run(() -> f.getItems().setAll(integers));
	}),
	REVERSE_SORT((p, f) -> run(() -> f.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2)))),
	CLEAR_LIST((p, f) -> run(() -> f.getItems().clear())),
	CHANGE_CELLS_PER_PAGE((p, f) -> {
		int cpp = getIntFromUser(
				p.getRoot(),
				"Change Cells Per Page",
				"Cells Number",
				Constraint.of(
						"Invalid number", i -> i >= 0)
		);
		if (cpp == -1) return;
		f.setCellsPerPage(cpp);
	}),
	CHANGE_CELLS_SIZE_TO((p, f) -> run(() -> {
		double value = getSizeFromUser(p.getRoot(), "Change Cells Size To...", "Current Size: " + f.getCellSize(), Constraint.of("Invalid!", i -> i > 0));
		if (value == -1.0) return;
		f.setCellSize(value);
	})),
	REPLACE_LIST((p, f) -> run(() -> {
		ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
				.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
				.collect(FXCollectors.toList());
		f.setItems(integers);
	})),
	SWITCH_CELLS_FACTORY((p, f) -> run(() -> {
		Class<? extends Cell<?>> lastFactory = p.getLastCell(1);
		Function<Integer, Cell<Integer>> factory;
		if (lastFactory == DetailedCell.class) {
			factory = AlternativeCell::new;
			p.setLastCell(1, AlternativeCell.class);
		} else {
			factory = DetailedCell::new;
			p.setLastCell(1, DetailedCell.class);
		}
		f.setCellFactory(factory);
	})),
	SWITCH_ORIENTATION((p, f) -> run(() -> {
		Orientation newOr;
		if (f.getOrientation() == Orientation.VERTICAL) {
			newOr = Orientation.HORIZONTAL;
			f.setOrientation(newOr);
			f.setCellSize(100);
		} else {
			newOr = Orientation.VERTICAL;
			f.setOrientation(newOr);
			f.setCellSize(64);
		}
	})),
	TEST_VARIABLE_BREADTH((p, f) -> run(() -> {
		if (!f.isFitToBreadth()) {
			f.setFitToBreadth(true);
			return;
		}

		Orientation or = f.getOrientation();
		Function<Integer, Cell<Integer>> factory;
		if (or == Orientation.VERTICAL) {
			factory = i -> new DetailedCell(i) {
				{
					setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
				}

				@Override
				protected double computePrefWidth(double height) {
					double def = f.getWidth();
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
					double def = f.getHeight();
					int delta = RandomUtils.random.nextInt(0, 100);
					boolean mul = RandomUtils.random.nextBoolean();
					return def + (mul ? delta : -delta);
				}
			};
		}
		f.setCellFactory(factory);
		f.setFitToBreadth(false);
	})),
	GO_TO_PAGE((p, f) -> run(() -> f.goToPage(getIntFromUser(p.getRoot(), "Go To Page...",
			"Available Pages %d/%d".formatted(1, f.getMaxPage()), Constraint.of("Invalid!", i -> i >= 0 && i <= f.getMaxPage()))))),
	SCROLL_TO_INDEX_20((p, f) -> run(() -> f.scrollToIndex(20))),
	SCROLL_TO_FIRST((p, f) -> run(f::scrollToFirst)),
	SCROLL_TO_LAST((p, f) -> run(f::scrollToLast));


	private final Action action;

	PFlowTestActions(Action action) {
		this.action = action;
	}

	public Action getAction() {
		return action;
	}

	public void run(FlowTestParameters p, PaginatedVirtualFlow<Integer, Cell<Integer>> n) {
		action.accept(p, n);
	}

	static void run(Runnable r) {
		r.run();
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
	}

	@FunctionalInterface
	interface Action {
		void accept(FlowTestParameters p, PaginatedVirtualFlow<Integer, Cell<Integer>> n);
	}
}
