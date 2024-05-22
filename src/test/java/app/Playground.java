package app;

import assets.User;
import interactive.table.TableTestUtils.Table;
import io.github.palexdev.mfxcore.base.TriConsumer;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.cells.base.TableCell;
import io.github.palexdev.virtualizedfx.table.VFXTableHelper;
import io.github.palexdev.virtualizedfx.table.defaults.VFXDefaultTableColumn;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static utils.Utils.debugView;

public class Playground extends Application {
	private final int cnt = 20;

	@Override
	public void start(Stage primaryStage) {
		Table table = new Table(User.users(cnt));
		table.setColumnsSize(80.0, 0.0);
		table.setExtraAutosizeWidth(50.0);
		table.switchColumnsLayoutMode();
		table.autosizeColumns();

		table.getColumns().forEach(c -> c.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			VFXDefaultTableColumn<User, ? extends TableCell<User>> dc = (VFXDefaultTableColumn<User, ? extends TableCell<User>>) c;
			if (e.getButton() == MouseButton.PRIMARY) {
				dc.setIconAlignment(
					EnumUtils.next(HPos.class, dc.getIconAlignment())
				);
			}
		}));

		StackPane pane = new StackPane() {
/*			@Override
			protected void layoutChildren() {
				table.resize(getWidth() - 200.0, getHeight());
				positionInArea(table, 0, 0, getWidth(), getHeight(), 0, getInsets(), HPos.LEFT, VPos.CENTER, true);
			}*/
		};
		pane.setAlignment(Pos.TOP_LEFT);
		pane.setPadding(InsetsBuilder.all(20));
		pane.getChildren().add(table);

		WhenEvent.intercept(table, MouseEvent.MOUSE_CLICKED)
			.condition(e -> e.getButton() == MouseButton.SECONDARY)
			.process(e -> table.autosizeColumns())
			.asFilter()
			.register();

		TriConsumer<Boolean, Double, Integer> scrollFn = (b, d, m) -> {
			if (b) {
				table.setHPos(table.getHPos() + d * m);
			} else {
				table.setVPos(table.getVPos() + d * m);
			}
		};

		WhenEvent.intercept(table, ScrollEvent.SCROLL)
			.process(e -> {
				double delta = e.isShiftDown() ? e.getDeltaX() : e.getDeltaY();
				if (delta == 0) return;
				int mul = (delta > 0) ? -1 : 1;

/*				MomentumTransition.fromTime(50, 500)
					.setOnUpdate(d -> scrollFn.accept(e.isShiftDown(), d, mul))
					.setInterpolatorFluent(Interpolator.EASE_OUT)
					.play();*/
				scrollFn.accept(e.isShiftDown(), 30.0, mul);
			})
			.asFilter()
			.register();
		//NodeMover.install(pane);

		Scene scene = new Scene(pane, 600, 400);
		primaryStage.setScene(scene);
		primaryStage.setOnHidden(e -> Platform.exit());
		primaryStage.show();
		primaryStage.centerOnScreen();

		//showDebugInfo(table);
		debugView(null, pane);
	}

	void showDebugInfo(Table table) {
		VFXTableHelper<User> helper = table.getHelper();
		VBox pane = new VBox(30);
		pane.setPrefWidth(400);
		pane.setPadding(InsetsBuilder.all(10));

		Label estimate = new Label();
		estimate.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "Estimate W/H: %f  /  %f".formatted(helper.getVirtualMaxX(), helper.getVirtualMaxY()))
			.addSources(helper.virtualMaxXProperty())
			.addSources(helper.virtualMaxYProperty())
			.get()
		);

		Label scrollable = new Label();
		scrollable.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "Scrollable X/Y: %f  /  %f".formatted(helper.maxHScroll(), helper.maxVScroll()))
			.addSources(helper.virtualMaxXProperty(), table.widthProperty())
			.addSources(helper.virtualMaxYProperty(), table.helperProperty())
			.get()
		);

		Label positions = new Label();
		positions.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "VPos/HPos: %f  /  %f".formatted(table.getVPos(), table.getHPos()))
			.addSources(table.vPosProperty(), table.hPosProperty())
			.get()
		);

		Label ranges = new Label();
		ranges.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "Range R/C: %s  / %s".formatted(helper.rowsRange(), helper.columnsRange()))
			.addSources(helper.rowsRangeProperty(), helper.columnsRangeProperty())
			.get()
		);

		pane.getChildren().addAll(estimate, scrollable, positions, ranges);

		Stage stage = new Stage();
		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.show();
		stage.centerOnScreen();
		stage.setX(0);
	}
}
