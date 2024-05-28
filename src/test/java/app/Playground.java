package app;

import interactive.table.TableTestUtils;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static model.User.users;
import static utils.Utils.debugView;

public class Playground extends Application {

	@Override
	public void start(Stage primaryStage) {
		StackPane pane = new StackPane();
		pane.setAlignment(Pos.CENTER);

		TableTestUtils.Table container = new TableTestUtils.Table(users(100));
		container.setColumnsLayoutMode(ColumnsLayoutMode.VARIABLE);
		container.autosizeColumns();
		container.setMinWidth(400);

		VFXScrollPane sp = new VFXScrollPane(container).bindTo(container);
		sp.setSmoothScroll(true);
		sp.setDragToScroll(true);
		//sp.setDragSmoothScroll(true);
		//sp.setShowButtons(true);
		sp.getStylesheets().add(VFXResources.loadResource("VFXScrollPane.css"));
		//sp.setVMin(0.25 * container.getMaxVScroll());
		//sp.setVMax(0.75 * container.getMaxVScroll());
		CSSFragment.Builder.build()
			.addSelector(".vfx-scroll-pane")
			.padding("5px")
			.closeSelector()
			.applyOn(sp);

		pane.getChildren().addAll(sp);
		Scene scene = new Scene(pane, 600, 400);
		primaryStage.setScene(scene);
		primaryStage.show();
		primaryStage.centerOnScreen();

		debugView(null, pane);
	}
}
