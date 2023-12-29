package app;

import interactive.list.ListTestUtils.SimpleCell;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxcore.events.WhenEvent;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.mfxeffects.animations.MomentumTransition;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.mfxeffects.animations.motion.Motion;
import io.github.palexdev.virtualizedfx.list.VFXList;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.scenicview.ScenicView;
import utils.Utils;

public class Playground extends Application {
	@Override
	public void start(Stage primaryStage) {
		StackPane left = new StackPane();
		VBox right = new VBox(20);
		right.setAlignment(Pos.CENTER);
		SplitPane pane = new SplitPane(left, right);
		pane.setPadding(InsetsBuilder.all(20));

		ObservableList<Integer> items = Utils.items(50);
		VFXList<Integer, SimpleCell> list = new VFXList<>(items, SimpleCell::new);
		list.setPrefSize(400, 400);
		list.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
		list.setStyle("-fx-border-color: blue; -fx-border-insets: -1");
		list.setSpacing(10.0);

		WhenEvent.intercept(pane, ScrollEvent.SCROLL)
			.asFilter()
			.process(e -> {
				ScrollUtils.ScrollDirection sd = ScrollUtils.determineScrollDirection(e);
				if (sd != ScrollUtils.ScrollDirection.UP && sd != ScrollUtils.ScrollDirection.DOWN) return;
				int mul = sd.intDirection();
				MomentumTransition.fromTime(20, 600)
					.setOnUpdate(v -> list.setVPos(list.getVPos() + v * mul))
					.setInterpolatorFluent(Motion.EASE_OUT_SINE)
					.play();
				//list.setVPos(list.getVPos() + 20.0 * mul);
			})
			.register();

		Button cList = new Button("Change List");
		cList.setOnAction(e -> list.setItems(Utils.items(50)));

		Button cInList = new Button("Change in List");
		cInList.setOnAction(e -> list.getItems().set(
			RandomUtils.random.nextInt(0, list.getItems().size()),
			RandomUtils.random.nextInt()
		));

		list.spacingProperty().addListener(i -> list.requestViewportLayout());
		Button sSpacing = new Button("Enable/Disable Spacing");
		sSpacing.setOnAction(e -> {
			double target = list.getSpacing() != 30.0 ? 30.0 : 0.0;
			Animations.TimelineBuilder.build()
				.add(Animations.KeyFrames.of(500, list.spacingProperty(), target, M3Motion.STANDARD))
				.setOnFinished(f -> list.setSpacing(target))
				.getAnimation()
				.play();
		});

		right.getChildren().addAll(cList, cInList, sSpacing);
		left.getChildren().add(list);
		Scene scene = new Scene(pane, 600, 600);
		primaryStage.setScene(scene);
		primaryStage.show();
		ScenicView.show(scene);
	}
}
