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

package interactive.controller;

import interactive.controller.ComparisonTestController.AlternativeCell;
import interactive.controller.ComparisonTestController.DetailedCell;
import interactive.model.Model;
import io.github.palexdev.materialfx.controls.MFXFilterComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.materialfx.utils.NodeUtils;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.builders.nodes.IconWrapperBuilder;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.mfxresources.font.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static interactive.controller.DialogUtils.getIntFromUser;
import static interactive.controller.DialogUtils.getSizeFromUser;

public class PaginationTestController implements Initializable {
	private static Pane root;
	private static final Map<Object, Class<? extends Cell<Integer>>> lastFactoryMap = new HashMap<>();

	private final Stage stage;
	private final PaginatedVirtualFlow<Integer, Cell<Integer>> flow;

	private double xOffset;
	private double yOffset;
	private boolean canDrag = false;

	@FXML private StackPane rootPane;
	@FXML private HBox windowHeader;
	@FXML private Label headerLabel;
	@FXML private MFXFilterComboBox<Action> actionsBox;
	@FXML private MFXIconWrapper runIcon;
	@FXML private BorderPane contentPane;
	@FXML private HBox footer;

	public PaginationTestController(Stage stage) {
		this.stage = stage;

		flow = new PaginatedVirtualFlow<>(
				FXCollections.observableArrayList(Model.integers),
				DetailedCell::new
		);
		flow.setCellsPerPage(7);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Init Static Variables
		root = rootPane;
		lastFactoryMap.put(flow, DetailedCell.class);

		// Header
		headerLabel.textProperty().bind(stage.titleProperty());
		windowHeader.setOnMousePressed(event -> {
			if (event.getPickResult().getIntersectedNode().getClass() == MFXFontIcon.class) {
				canDrag = false;
				return;
			}
			xOffset = stage.getX() - event.getScreenX();
			yOffset = stage.getY() - event.getScreenY();
			canDrag = true;
		});
		windowHeader.setOnMouseDragged(event -> {
			if (canDrag) {
				stage.setX(event.getScreenX() + xOffset);
				stage.setY(event.getScreenY() + yOffset);
			}
		});

		// Actions Pane
		actionsBox.setItems(FXCollections.observableArrayList(Action.values()));
		actionsBox.selectFirst();

		runIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);


		// Content
		VirtualScrollPane vsp = VirtualScrollPane.wrap(flow);
		Runnable speedAction = () -> {
			VirtualScrollPane.setVSpeed(vsp, 15, flow.getCellSize() * 1.5, 15);
			VirtualScrollPane.setHSpeed(vsp, 15, flow.getCellSize() * 1.5, 15);
		};
		When.onInvalidated(flow.cellSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();
		contentPane.setCenter(vsp);

		// Footer
		// TODO spinner models are absolute garbage
		MFXIconWrapper goFirst = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-step-backward").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.scrollToFirst()).getNode();
		NodeUtils.makeRegionCircular(goFirst);

		MFXIconWrapper goBack = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-caret-left").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.setCurrentPage(Math.max(0, flow.getCurrentPage() - 1))).getNode();
		NodeUtils.makeRegionCircular(goBack);

		MFXIconWrapper goForward = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-caret-right").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.setCurrentPage(flow.getCurrentPage() + 1)).getNode();
		NodeUtils.makeRegionCircular(goForward);

		MFXIconWrapper goLast = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().setDescription("mfx-step-forward").setSize(16).get())
				.setSize(32)
				.defaultRippleGeneratorBehavior()
				.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> flow.scrollToLast()).getNode();
		NodeUtils.makeRegionCircular(goLast);

		MFXTextField label = MFXTextField.asLabel();
		label.setFloatMode(FloatMode.DISABLED);
		label.setAlignment(Pos.CENTER);
		label.setLeadingIcon(new HBox(5, goFirst, goBack));
		label.setTrailingIcon(new HBox(5, goForward, goLast));
		label.textProperty().bind(StringBindingBuilder.build()
				.setMapper(() -> flow.getCurrentPage() + "/" + flow.getMaxPage())
				.addSources(flow.currentPageProperty(), flow.maxPageProperty())
				.get()
		);

		footer.getChildren().addAll(label);
	}

	@FXML
	void runAction() {
		Optional.ofNullable(actionsBox.getSelectedItem())
				.ifPresent(action -> action.run(flow));
	}

	@FXML
	void close() {
		System.exit(0);
	}

	@FXML
	void minimize() {
		stage.setIconified(!stage.isIconified());
	}

	@FXML
	void maximize() {
		stage.setMaximized(!stage.isMaximized());
	}

	//================================================================================
	// Inner Classes
	//================================================================================
	@FunctionalInterface
	private interface TestAction<T> {
		void accept(PaginatedVirtualFlow<T, Cell<T>> flow);

		default Consumer<PaginatedVirtualFlow<T, Cell<T>>> andThen(TestAction<T> action) {
			Objects.requireNonNull(action);

			return (flow) -> {
				accept(flow);
				action.accept(flow);
			};
		}
	}

	private enum Action {
		UPDATE_AT_5(flow -> execute(() -> flow.getItems().set(5, RandomUtils.random.nextInt(0, 9999)))),
		UPDATE_AT_25(flow -> execute(() -> flow.getItems().set(25, RandomUtils.random.nextInt(0, 9999)))),
		ADD_AT_0(flow -> execute(() -> flow.getItems().add(0, RandomUtils.random.nextInt(0, 9999)))),
		ADD_AT_3(flow -> execute(() -> flow.getItems().add(3, RandomUtils.random.nextInt(0, 9999)))),
		ADD_AT_END(flow -> execute(() -> flow.getItems().add(flow.getItems().size(), RandomUtils.random.nextInt(0, 9999)))),
		ADD_MULTIPLE_AT_3(flow -> execute(() -> {
			List<Integer> integers = List.of(
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999)
			);
			flow.getItems().addAll(3, integers);
		})),
		ADD_AT(flow -> {
			int index = getIntFromUser(root, "Add at...", "Index to add at");
			execute(() -> flow.getItems().add(index, RandomUtils.random.nextInt(0, 9999)));
		}),
		DELETE_AT_3(flow -> execute(() -> flow.getItems().remove(3))),
		DELETE_SPARSE(flow -> execute(() -> flow.getItems().removeAll(
				flow.getItems().get(2), flow.getItems().get(5),
				flow.getItems().get(6), flow.getItems().get(8),
				flow.getItems().get(25), flow.getItems().get(53)
		))),
		DELETE_FIRST(flow -> execute(() -> flow.getItems().remove(0))),
		DELETE_LAST(flow -> execute(() -> flow.getItems().remove(flow.getItems().size() - 1))),
		DELETE_ALL_VISIBLE(flow -> execute(() -> {
			Integer[] integers = flow.getIndexedCells().keySet().stream()
					.map(i -> flow.getItems().get(i))
					.toArray(Integer[]::new);
			flow.getItems().removeAll(integers);
		})),
		REPLACE_ALL(flow -> execute(() -> {
			List<Integer> integers = IntStream.rangeClosed(0, 50)
					.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
					.toList();
			flow.getItems().setAll(integers);
		})),
		REVERSE_SORT(flow -> execute(() -> flow.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2)))),
		CLEAR_LIST(flow -> execute(() -> flow.getItems().clear())),
		CHANGE_VIEWPORT_SIZE_TO(flow -> execute(() -> {
			VirtualScrollPane vsp = (VirtualScrollPane) flow.getParent().getParent();
			String fText = (flow.getOrientation() == Orientation.VERTICAL) ?
					"Current height: " + vsp.getHeight() :
					"Current width: " + vsp.getWidth();
			double value = getSizeFromUser(root, "Change Viewport Size To...", fText);
			if (value == -1.0) return;

			if (flow.getOrientation() == Orientation.VERTICAL) {
				vsp.setPrefHeight(value);
				vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
			} else {
				vsp.setPrefWidth(value);
				vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
			}
		})),
		RESET_VIEWPORT_SIZE(flow -> execute(() -> {
			VirtualScrollPane vsp = (VirtualScrollPane) flow.getParent().getParent();
			vsp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		})),
		CHANGE_CELLS_SIZE_TO(flow -> execute(() -> {
			double value = getSizeFromUser(root, "Change Cells Size To...", "Current Size: " + flow.getCellSize());
			if (value == -1.0) return;
			flow.setCellSize(value);
		})),
		CHANGE_CELLS_PER_PAGE(flow -> execute(() -> {
			int value = getIntFromUser(root, "Change Cells Per Page To...", "Current Number: " + flow.getCellsPerPage());
			if (value == -1) return;
			flow.setCellsPerPage(value);
		})),
		REPLACE_LIST(flow -> execute(() -> {
			ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
					.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
					.collect(FXCollectors.toList());
			flow.setItems(integers);
		})),
		SWITCH_CELLS_FACTORY(flow -> execute(() -> {
			Class<? extends Cell<Integer>> lastFactory = lastFactoryMap.get(flow);
			Function<Integer, Cell<Integer>> factory;
			if (lastFactory == DetailedCell.class) {
				factory = AlternativeCell::new;
				lastFactoryMap.put(flow, AlternativeCell.class);
			} else {
				factory = DetailedCell::new;
				lastFactoryMap.put(flow, DetailedCell.class);
			}
			flow.setCellFactory(factory);
		})),
		SWITCH_ORIENTATION(flow -> execute(() -> {
			Orientation newOr;
			if (flow.getOrientation() == Orientation.VERTICAL) {
				newOr = Orientation.HORIZONTAL;
				flow.setOrientation(newOr);
				flow.setCellSize(100);
			} else {
				newOr = Orientation.VERTICAL;
				flow.setOrientation(newOr);
				flow.setCellSize(64);
			}
		})),
		TEST_VARIABLE_BREADTH(flow -> execute(() -> {
			if (!flow.isFitToBreadth()) {
				flow.setFitToBreadth(true);
				return;
			}

			Orientation o = flow.getOrientation();
			Function<Integer, Cell<Integer>> factory;
			if (o == Orientation.VERTICAL) {
				factory = i -> new DetailedCell(i) {
					{
						setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
					}

					@Override
					protected double computePrefWidth(double height) {
						double def = flow.getWidth();
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
						double def = flow.getHeight();
						int delta = RandomUtils.random.nextInt(0, 100);
						boolean mul = RandomUtils.random.nextBoolean();
						return def + (mul ? delta : -delta);
					}
				};
			}
			flow.setCellFactory(factory);
			flow.setFitToBreadth(false);
		})),
		GO_TO_PAGE(flow -> execute(() -> flow.goToPage(getIntFromUser(root, "Go To Page...",
				"Available Pages %d/%d".formatted(1, flow.getMaxPage()))))),
		SCROLL_TO_INDEX_20(flow -> execute(() -> flow.scrollToIndex(20))),
		SCROLL_TO_FIRST(flow -> execute(flow::scrollToFirst)),
		SCROLL_TO_LAST(flow -> execute(flow::scrollToLast));

		private final TestAction<Integer> action;

		Action(TestAction<Integer> action) {
			this.action = action;
		}

		public TestAction<Integer> getAction() {
			return action;
		}

		public void run(PaginatedVirtualFlow<Integer, Cell<Integer>> flow) {
			action.accept(flow);
		}

		private static void execute(Runnable action) {
			action.run();
		}

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
		}
	}
}
