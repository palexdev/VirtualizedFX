package app;

import interactive.TestFXUtils.SimpleCell;
import interactive.grid.GridTestUtils.Grid;
import io.github.palexdev.mfxcore.base.TriConsumer;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxcore.builders.bindings.StringBindingBuilder;
import io.github.palexdev.mfxcore.controls.Label;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils;
import io.github.palexdev.mfxeffects.animations.MomentumTransition;
import io.github.palexdev.virtualizedfx.grid.VFXGridHelper;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.NodeMover;

import java.util.List;

import static utils.Utils.items;

public class Playground extends Application {

	@Override
	public void start(Stage primaryStage) {
		StackPane pane = new StackPane();
		pane.setAlignment(Pos.TOP_LEFT);
		pane.setPadding(InsetsBuilder.all(20));

		Grid grid = new Grid(items(200));
		//grid.setBufferSize(BufferSize.standard());
		//grid.setColumnsNum(10);
		grid.setSpacing(10);
		grid.setAlignment(Pos.CENTER);
		//grid.setMaxSize(400, 400);
		pane.getChildren().add(grid);

		TriConsumer<Boolean, Double, Integer> scrollFn = (b, d, m) -> {
			if (b) {
				grid.setHPos(grid.getHPos() + d * m);
			} else {
				grid.setVPos(grid.getVPos() + d * m);
			}
		};

		WhenEvent.intercept(grid, ScrollEvent.SCROLL)
			.process(e -> {
				ScrollUtils.ScrollDirection sd = ScrollUtils.determineScrollDirection(e);
				int mul = switch (sd) {
					case UP, RIGHT -> -1;
					case DOWN, LEFT -> 1;
				};
				MomentumTransition.fromTime(50, 500)
					.setOnUpdate(d -> scrollFn.accept(e.isShiftDown(), d, mul))
					.setInterpolatorFluent(Interpolator.EASE_OUT)
					.play();
			})
			.asFilter()
			.register();
		NodeMover.install(pane);

		When.onInvalidated(grid.widthProperty())
			.then(w -> grid.autoArrange())
			.listen();

		Scene scene = new Scene(pane, 400, 400);
		primaryStage.setScene(scene);
		primaryStage.setOnHidden(e -> Platform.exit());
		primaryStage.show();
		primaryStage.centerOnScreen();

		showDebugInfo(grid);
		//debugView(null, pane);
	}

	void showDebugInfo(Grid grid) {
		VFXGridHelper<Integer, SimpleCell> helper = grid.getHelper();
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
			.addSources(helper.virtualMaxXProperty(), grid.widthProperty())
			.addSources(helper.virtualMaxYProperty(), grid.helperProperty())
			.get()
		);

		Label positions = new Label();
		positions.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "VPos/HPos: %f  /  %f".formatted(grid.getVPos(), grid.getHPos()))
			.addSources(grid.vPosProperty(), grid.hPosProperty())
			.get()
		);

		Label ranges = new Label();
		ranges.textProperty().bind(StringBindingBuilder.build()
			.setMapper(() -> "Range R/C: %s  / %s".formatted(helper.rowsRange(), helper.columnsRange()))
			.addSources(helper.rowsRangeProperty(), helper.columnsRangeProperty())
			.get()
		);

		Button action = new Button("Test Items Change");
		action.setOnAction(e -> grid.getItems().addAll(62, List.of(-1, -2, -3, -4, -5)));

		pane.getChildren().addAll(estimate, scrollable, positions, ranges, action);

		Stage stage = new Stage();
		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.show();
		stage.centerOnScreen();
		stage.setX(0);
	}
}
