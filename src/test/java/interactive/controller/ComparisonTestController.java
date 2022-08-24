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

import io.github.palexdev.materialfx.controls.*;
import io.github.palexdev.materialfx.dialogs.MFXDialogs;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.enums.DialogType;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.mfxcore.animations.Animations.SequentialBuilder;
import io.github.palexdev.mfxcore.animations.ConsumerTransition;
import io.github.palexdev.mfxcore.animations.Interpolators;
import io.github.palexdev.mfxcore.base.TriConsumer;
import io.github.palexdev.mfxcore.base.beans.PositionBean;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.builders.nodes.IconWrapperBuilder;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.RegionUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxresources.font.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import io.github.palexdev.virtualizedfx.unused.simple.SimpleVirtualFlow;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.IntStream;

import static interactive.model.Model.integers;

@SuppressWarnings("deprecation")
public class ComparisonTestController implements Initializable {
	private static Pane root;
	private static final Map<Object, Class<? extends Cell<Integer>>> lastFactoryMap = new HashMap<>();

	private final Stage stage;
	private final SimpleVirtualFlow<Integer, Cell<Integer>> lFlow;
	private final VirtualFlow<Integer, Cell<Integer>> rFlow;
	private final VirtualScrollPane vsp;
	private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.NEW_IMP);

	private double xOffset;
	private double yOffset;
	private boolean canDrag = false;
	private static double animationDuration = 500.0;

	@FXML
	private StackPane rootPane;

	@FXML
	private HBox windowHeader;

	@FXML
	private Label headerLabel;

	@FXML
	private Label selectionLabel;

	@FXML
	private StackPane leftPane;

	@FXML
	private StackPane rightPane;

	@FXML
	private MFXComboBox<Action> actionsBox;

	@FXML
	private MFXIconWrapper runIcon;

	@FXML
	private MFXIconWrapper switchIcon;

	@FXML
	private MFXCheckbox ssCheck;

	@FXML
	private MFXCheckbox modeCheck;

	@FXML
	private MFXTextField durationField;

	public ComparisonTestController(Stage stage) {
		this.stage = stage;

		lFlow = new SimpleVirtualFlow<>(
				FXCollections.observableArrayList(integers),
				DetailedCell::new,
				Orientation.VERTICAL
		);
		rFlow = new VirtualFlow<>(
				FXCollections.observableArrayList(integers),
				DetailedCell::new
		);
		rFlow.setCellSize(64);

		vsp = VirtualScrollPane.wrap(rFlow);
		Runnable speedAction = () -> {
			VirtualScrollPane.setVSpeed(vsp, 15, rFlow.getCellSize() * 1.5, 15);
			VirtualScrollPane.setHSpeed(vsp, 15, rFlow.getCellSize() * 1.5, 15);
		};
		When.onInvalidated(rFlow.cellSizeProperty())
				.then(i -> speedAction.run())
				.executeNow()
				.listen();
		//vsp.setLayoutMode(LayoutMode.COMPACT);
		//vsp.setAutoHideBars(true);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Init Static Variables
		root = rootPane;
		lastFactoryMap.put(lFlow, DetailedCell.class);
		lastFactoryMap.put(rFlow, DetailedCell.class);

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
		selectionLabel.textProperty().bind(StringBindingBuilder.build()
				.setMapper(() -> {
					Mode mode = this.mode.get();
					return "Action Target: " + mode;
				})
				.addSources(mode)
				.get()
		);
		actionsBox.setItems(FXCollections.observableArrayList(Action.values()));
		actionsBox.selectFirst();

		runIcon.defaultRippleGeneratorBehavior();
		switchIcon.defaultRippleGeneratorBehavior();
		RegionUtils.makeRegionCircular(runIcon);
		RegionUtils.makeRegionCircular(switchIcon);

		// Settings
		ssCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				vsp.setSmoothScroll(true);
				vsp.setTrackSmoothScroll(true);
				lFlow.features().enableSmoothScrolling(1);
			} else {
				vsp.setSmoothScroll(false);
				vsp.setTrackSmoothScroll(false);
				lFlow.features().disableSmoothScrolling();
			}
		});
		MFXTooltip tooltip = MFXTooltip.of(ssCheck,
				"""
						Notice how in the new implementation
						the smooth scrolling is much more reliable thanks to the new MFXScrollbar.
						Check the documentation and source code to know more about it.
						"""
		).install();
		MFXTooltip.fixPosition(tooltip, true);
		modeCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				mode.set(Mode.BOTH);
			} else {
				mode.set(Mode.NEW_IMP);
			}
		});

		durationField.delegateSetTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("[0-9]*") ? change : null));
		durationField.textProperty().addListener(invalidated -> animationDuration = Double.parseDouble(durationField.getText()));

		// Misc
		leftPane.getChildren().setAll(lFlow);
		rightPane.getChildren().setAll(vsp);
	}

	@FXML
	void runAction() {
		Optional.ofNullable(actionsBox.getSelectedItem())
				.ifPresent(action -> action.run(mode.get(), lFlow, rFlow));
	}

	@FXML
	void switchTarget() {
		if (modeCheck.isSelected()) {
			return;
		}
		mode.set(mode.get() == Mode.OLD_IMP ? Mode.NEW_IMP : Mode.OLD_IMP);
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
	private enum Mode {
		OLD_IMP, NEW_IMP, BOTH;

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
		}
	}

	@FunctionalInterface
	private interface TestAction<T> {
		void accept(Mode mode, SimpleVirtualFlow<T, Cell<T>> oldImp, VirtualFlow<T, Cell<T>> newImp);

		default TriConsumer<Mode, SimpleVirtualFlow<T, Cell<T>>, VirtualFlow<T, Cell<T>>> andThen(TestAction<T> action) {
			Objects.requireNonNull(action);

			return (mode, oldImp, newImp) -> {
				accept(mode, oldImp, newImp);
				action.accept(mode, oldImp, newImp);
			};
		}
	}

	@SuppressWarnings("unchecked")
	private enum Action {
		UPDATE_AT_5((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().set(5, RandomUtils.random.nextInt(0, 9999));
			Runnable nAction = () -> newImp.getItems().set(5, RandomUtils.random.nextInt(0, 9999));
			execute(mode, oAction, nAction);
		}),
		UPDATE_AT_25((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().set(25, RandomUtils.random.nextInt(0, 9999));
			Runnable nAction = () -> newImp.getItems().set(25, RandomUtils.random.nextInt(0, 9999));
			execute(mode, oAction, nAction);
		}),
		ADD_AT_0((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().add(0, RandomUtils.random.nextInt(0, 9999));
			Runnable nAction = () -> newImp.getItems().add(0, RandomUtils.random.nextInt(0, 9999));
			execute(mode, oAction, nAction);
		}),
		ADD_MULTIPLE_AT_3((mode, oldImp, newImp) -> {
			List<Integer> integers = List.of(
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999),
					RandomUtils.random.nextInt(0, 9999)
			);
			Runnable oAction = () -> oldImp.getItems().addAll(3, integers);
			Runnable nAction = () -> newImp.getItems().addAll(3, integers);
			execute(mode, oAction, nAction);
		}),
		DELETE_AT_3((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().remove(3);
			Runnable nAction = () -> newImp.getItems().remove(3);
			execute(mode, oAction, nAction);
		}),
		DELETE_SPARSE((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().removeAll(
					oldImp.getItems().get(2), oldImp.getItems().get(5),
					oldImp.getItems().get(6), oldImp.getItems().get(8),
					oldImp.getItems().get(25), oldImp.getItems().get(53)
			);
			Runnable nAction = () -> newImp.getItems().removeAll(
					newImp.getItems().get(2), newImp.getItems().get(5),
					newImp.getItems().get(6), newImp.getItems().get(8),
					newImp.getItems().get(25), newImp.getItems().get(53)
			);
			execute(mode, oAction, nAction);
		}),
		DELETE_FIRST((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().remove(0);
			Runnable nAction = () -> newImp.getItems().remove(0);
			execute(mode, oAction, nAction);
		}),
		DELETE_LAST((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().remove(oldImp.getItems().size() - 1);
			Runnable nAction = () -> newImp.getItems().remove(newImp.getItems().size() - 1);
			execute(mode, oAction, nAction);
		}),
		DELETE_ALL_VISIBLE((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				Integer[] integers = oldImp.getCells().keySet().stream()
						.map(i -> oldImp.getItems().get(i))
						.toArray(Integer[]::new);
				oldImp.getItems().removeAll(integers);
			};
			Runnable nAction = () -> {
				Integer[] integers = newImp.getIndexedCells().keySet().stream()
						.map(i -> newImp.getItems().get(i))
						.toArray(Integer[]::new);
				newImp.getItems().removeAll(integers);
			};
			execute(mode, oAction, nAction);
		}),
		REPLACE_ALL((mode, oldImp, newImp) -> {
			List<Integer> integers = IntStream.rangeClosed(0, 50)
					.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
					.toList();
			Runnable oAction = () -> oldImp.getItems().setAll(integers);
			Runnable nAction = () -> newImp.getItems().setAll(integers);
			execute(mode, oAction, nAction);
		}),
		REVERSE_SORT((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2));
			Runnable nAction = () -> newImp.getItems().sort((o1, o2) -> Comparator.<Integer>reverseOrder().compare(o1, o2));
			execute(mode, oAction, nAction);
		}),
		CLEAR_LIST((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.getItems().clear();
			Runnable nAction = () -> newImp.getItems().clear();
			execute(mode, oAction, nAction);
		}),
		CHANGE_VIEWPORT_SIZE_TO((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				String fText = (oldImp.getOrientation() == Orientation.VERTICAL) ?
						"Current height: " + oldImp.getHeight() :
						"Current width: " + oldImp.getWidth();
				double value = getSizeFromUser(root, "Change Viewport Size To...", fText);
				if (value == -1.0) return;

				if (oldImp.getOrientation() == Orientation.VERTICAL) {
					oldImp.setPrefHeight(value);
					oldImp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
				} else {
					oldImp.setPrefWidth(value);
					oldImp.setMaxSize(Region.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE);
				}
			};
			Runnable nAction = () -> {
				VirtualScrollPane vsp = (VirtualScrollPane) newImp.getParent().getParent();
				String fText = (newImp.getOrientation() == Orientation.VERTICAL) ?
						"Current height: " + vsp.getHeight() :
						"Current width: " + vsp.getWidth();
				double value = getSizeFromUser(root, "Change Viewport Size To...", fText);
				if (value == -1.0) return;

				if (newImp.getOrientation() == Orientation.VERTICAL) {
					vsp.setPrefHeight(value);
					vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
				} else {
					vsp.setPrefWidth(value);
					vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_PREF_SIZE);
				}
			};
			execute(mode, oAction, nAction);
		}),
		RESET_VIEWPORT_SIZE((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				oldImp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
				oldImp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			};
			Runnable nAction = () -> {
				VirtualScrollPane vsp = (VirtualScrollPane) newImp.getParent().getParent();
				vsp.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
				vsp.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			};
			execute(mode, oAction, nAction);
		}),
		CHANGE_CELLS_SIZE_TO((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				String details = """
						The chosen action: "Change cells size to" is not supported by the old implementation
						since the cells' fixed size had to be hard-coded in the cells source code
						""";
				showDialog(root, DialogType.ERROR, "Unsupported Operation...", details);
			};
			Runnable nAction = () -> {
				double value = getSizeFromUser(root, "Change Cells Size To...", "Current Size: " + newImp.getCellSize());
				if (value == -1.0) return;
				newImp.setCellSize(value);
			};
			execute(mode, oAction, nAction);
		}),
		REPLACE_LIST((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
						.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
						.collect(FXCollectors.toList());
				oldImp.setItems(integers);
			};
			Runnable nAction = () -> {
				ObservableList<Integer> integers = IntStream.rangeClosed(0, 200)
						.mapToObj(i -> RandomUtils.random.nextInt(0, 9999))
						.collect(FXCollectors.toList());
				newImp.setItems(integers);
			};
			execute(mode, oAction, nAction);
		}),
		SWITCH_CELLS_FACTORY((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				Class<? extends Cell<Integer>> lastFactory = lastFactoryMap.get(oldImp);
				Function<Integer, Cell<Integer>> factory;
				if (lastFactory == DetailedCell.class) {
					factory = AlternativeCell::new;
					lastFactoryMap.put(oldImp, AlternativeCell.class);
				} else {
					factory = DetailedCell::new;
					lastFactoryMap.put(oldImp, DetailedCell.class);
				}
				oldImp.setCellFactory(factory);
			};
			Runnable nAction = () -> {
				Class<? extends Cell<Integer>> lastFactory = lastFactoryMap.get(newImp);
				Function<Integer, Cell<Integer>> factory;
				if (lastFactory == DetailedCell.class) {
					factory = AlternativeCell::new;
					lastFactoryMap.put(newImp, AlternativeCell.class);
				} else {
					factory = DetailedCell::new;
					lastFactoryMap.put(newImp, DetailedCell.class);
				}
				newImp.setCellFactory(factory);
			};
			execute(mode, oAction, nAction);
		}),
		SWITCH_ORIENTATION((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				Orientation newOr;
				if (oldImp.getOrientation() == Orientation.VERTICAL) {
					newOr = Orientation.HORIZONTAL;
					oldImp.setOrientation(newOr);
				} else {
					newOr = Orientation.VERTICAL;
					oldImp.setOrientation(newOr);
				}
				String details = """
						This test works partially for the old implementation as the cells size
						cannot be changed due to the fact it was hard-coded in the cells source code
						""";
				showDialog(root, DialogType.WARNING, "Warning...", details);
			};
			Runnable nAction = () -> {
				Orientation newOr;
				if (newImp.getOrientation() == Orientation.VERTICAL) {
					newOr = Orientation.HORIZONTAL;
					newImp.setOrientation(newOr);
					newImp.setCellSize(100);
				} else {
					newOr = Orientation.VERTICAL;
					newImp.setOrientation(newOr);
					newImp.setCellSize(64);
				}
			};
			execute(mode, oAction, nAction);
		}),
		TEST_VARIABLE_BREADTH((mode, oldImp, newImp) -> {
			Runnable oAction = () -> {
				String details = """
						This feature was not available in the old implementation as all
						cells were automatically adjusted to fit the viewport.
						""";
				showDialog(root, DialogType.WARNING, "Warning...", details);
			};
			Runnable nAction = () -> {
				if (!newImp.isFitToBreadth()) {
					newImp.setFitToBreadth(true);
					return;
				}

				Orientation o = newImp.getOrientation();
				Function<Integer, Cell<Integer>> factory;
				if (o == Orientation.VERTICAL) {
					factory = i -> new DetailedCell(i) {
						{
							setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
						}

						@Override
						protected double computePrefWidth(double height) {
							double def = newImp.getWidth();
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
							double def = newImp.getHeight();
							int delta = RandomUtils.random.nextInt(0, 100);
							boolean mul = RandomUtils.random.nextBoolean();
							return def + (mul ? delta : -delta);
						}
					};
				}
				newImp.setCellFactory(factory);
				newImp.setFitToBreadth(false);
			};
			execute(mode, oAction, nAction);
		}),
		SCROLL_BY_64PX((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.scrollBy(64.0);
			Runnable nAction = () -> newImp.scrollBy(64.0);
			execute(mode, oAction, nAction);
		}),
		SCROLL_TO_64PX((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.scrollToPixel(64.0);
			Runnable nAction = () -> newImp.scrollToPixel(64.0);
			execute(mode, oAction, nAction);
		}),
		SCROLL_TO_INDEX_20((mode, oldImp, newImp) -> {
			Runnable oAction = () -> oldImp.scrollTo(20);
			Runnable nAction = () -> newImp.scrollToIndex(20);
			execute(mode, oAction, nAction);
		}),
		SCROLL_TO_FIRST((mode, oldImp, newImp) -> {
			Runnable oAction = oldImp::scrollToFirst;
			Runnable nAction = newImp::scrollToFirst;
			execute(mode, oAction, nAction);
		}),
		SCROLL_TO_LAST((mode, oldImp, newImp) -> {
			Runnable oAction = oldImp::scrollToLast;
			Runnable nAction = newImp::scrollToLast;
			execute(mode, oAction, nAction);
		});

		private final TestAction<Integer> action;

		Action(TestAction<Integer> action) {
			this.action = action;
		}

		public TestAction<Integer> getAction() {
			return action;
		}

		public void run(Mode mode, SimpleVirtualFlow<Integer, Cell<Integer>> oldImp, VirtualFlow<Integer, Cell<Integer>> newImp) {
			action.accept(mode, oldImp, newImp);
		}

		private static void execute(Mode mode, Runnable oAction, Runnable nAction) {
			switch (mode) {
				case BOTH -> {
					oAction.run();
					nAction.run();
				}
				case OLD_IMP -> oAction.run();
				case NEW_IMP -> nAction.run();
			}
		}

		@Override
		public String toString() {
			return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
		}
	}

	public abstract static class CommonCell extends HBox implements Cell<Integer> {
		protected final Label label;
		protected Integer item;
		protected int index;

		public CommonCell(Integer item) {
			this.item = item;
			label = new Label(dataToString());
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

			setPrefSize(64, 64);
			setAlignment(Pos.CENTER_LEFT);
			setHgrow(label, Priority.ALWAYS);
			getStyleClass().add("cell");
			getChildren().setAll(label);

			StyleUtils.setBackground(this, Color.TRANSPARENT);

			addEventHandler(MouseEvent.MOUSE_CLICKED, event -> updateItem(CommonCell.this.item));
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void updateIndex(int index) {
			this.index = index;
			label.setText(dataToString());
		}

		@Override
		public void updateItem(Integer item) {
			this.item = item;
			label.setText(dataToString());
			animateBackground(this);
		}

		protected abstract String dataToString();
	}

	public static class DetailedCell extends CommonCell {
		private final MFXIconWrapper icon;

		public DetailedCell(Integer item) {
			super(item);

			icon = IconWrapperBuilder.build()
					.setIcon(MFXFontIcon.getRandomIcon(12, Color.LAWNGREEN))
					.setSize(16)
					.setManaged(false)
					.getNode();

			layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
				Pair<Double, Double> o = Optional.ofNullable(oldValue)
						.map(b -> new Pair<>(b.getWidth(), b.getHeight()))
						.orElse(new Pair<>(-1.0, -1.0));
				Pair<Double, Double> n = new Pair<>(newValue.getWidth(), newValue.getHeight());
				if (!n.equals(o)) label.setText(dataToString());
			});

			getChildren().addAll(icon);
		}

		protected String dataToString() {
			int spaces = (item > 0) ?
					10 - (int) (Math.log10(item) + 1) :
					9;
			return "Data: " + item +
					" ".repeat(spaces) +
					"Index: " + index +
					" ".repeat(spaces) +
					"Size[W/H]: " + MessageFormat.format("[{0}/{1}]", getWidth(), getHeight());
		}

		@Override
		protected void layoutChildren() {
			super.layoutChildren();
			double w = getWidth();
			double h = getHeight();

			PositionBean pos = LayoutUtils.computePosition(
					this, icon,
					0, 0, w - 8.0, h, 0,
					Insets.EMPTY,
					HPos.RIGHT, VPos.CENTER
			);
			icon.resizeRelocate(pos.getX(), pos.getY(), icon.getSize(), icon.getSize());
		}
	}

	public static class AlternativeCell extends CommonCell {
		private final MFXIconWrapper icon;

		public AlternativeCell(Integer item) {
			super(item);
			icon = new MFXIconWrapper(randomIcon(), 32.0);
			getChildren().setAll(icon, label);
			setSpacing(20);
		}

		private Node randomIcon() {
			return MFXFontIcon.getRandomIcon(18, Color.web("#35ce8f"));
		}

		@Override
		protected String dataToString() {
			return "Data: " + item;
		}
	}

	public static void animateBackground(Region region) {
		int r = RandomUtils.random.nextInt(0, 255);
		int g = RandomUtils.random.nextInt(0, 255);
		int b = RandomUtils.random.nextInt(0, 255);
		ConsumerTransition ct1 = ConsumerTransition.of(frac -> StyleUtils.setBackground(region, Color.rgb(r, g, b, frac)))
				.setDuration(animationDuration)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		ConsumerTransition ct2 = ConsumerTransition.of(frac -> StyleUtils.setBackground(region, Color.rgb(r, g, b, 1.0 - frac)))
				.setDuration(500)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		SequentialBuilder.build()
				.add(ct1)
				.add(ct2)
				.getAnimation()
				.play();
	}

	protected static double getSizeFromUser(Pane owner, String title, String fieldText) {
		MFXTextField field = new MFXTextField("", "", fieldText);
		field.setFloatMode(FloatMode.INLINE);
		field.setPrefSize(240, 32);
		StackPane content = new StackPane(field);
		content.setAlignment(Pos.CENTER);

		MFXStageDialog sd = MFXGenericDialogBuilder.build()
				.setHeaderText(title)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.setContent(content)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();

		AtomicReference<Double> val = new AtomicReference<>((double) 0);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				val.set(Double.parseDouble(field.getText()));
				if (val.get() <= 0.0) {
					field.setFloatingText("Invalid value, > 0");
					return;
				}
				sd.close();
			} catch (NumberFormatException ignored) {
			}
		});
		MFXButton cancelAction = new MFXButton("Cancel");
		cancelAction.setOnAction(event -> {
			val.set(-1.0);
			sd.close();
		});
		((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);
		sd.showAndWait();
		return val.get();
	}

	public static int getIntFromUser(Pane owner, String title, String fieldText) {
		MFXTextField field = new MFXTextField("", "", fieldText);
		field.setFloatMode(FloatMode.INLINE);
		field.setPrefSize(240, 32);
		StackPane content = new StackPane(field);
		content.setAlignment(Pos.CENTER);

		MFXStageDialog sd = MFXGenericDialogBuilder.build()
				.setHeaderText(title)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.setContent(content)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();

		AtomicInteger val = new AtomicInteger(0);
		MFXButton okAction = new MFXButton("OK");
		okAction.setOnAction(event -> {
			try {
				val.set(Integer.parseInt(field.getText()));
				if (val.get() <= 0) {
					field.setFloatingText("Invalid value, > 0");
					return;
				}
				sd.close();
			} catch (NumberFormatException ignored) {
			}
		});
		MFXButton cancelAction = new MFXButton("Cancel");
		cancelAction.setOnAction(event -> {
			val.set(-1);
			sd.close();
		});
		((MFXGenericDialog) sd.getContent()).addActions(okAction, cancelAction);
		sd.showAndWait();
		return val.get();
	}

	public static void showDialog(Pane owner, DialogType type, String title, String details) {
		MFXGenericDialogBuilder builder = switch (type) {
			case ERROR -> MFXDialogs.error();
			case WARNING -> MFXDialogs.warn();
			case INFO -> MFXDialogs.info();
			case GENERIC -> MFXGenericDialogBuilder.build();
		};
		MFXStageDialog sd = builder
				.setHeaderText(title)
				.setContentText(details)
				.setShowAlwaysOnTop(false)
				.setShowMinimize(false)
				.toStageDialogBuilder()
				.initModality(Modality.WINDOW_MODAL)
				.initOwner(owner.getScene().getWindow())
				.setOwnerNode(owner)
				.setCenterInOwnerNode(true)
				.setScrimOwner(true)
				.get();
		sd.showDialog();
	}
}
