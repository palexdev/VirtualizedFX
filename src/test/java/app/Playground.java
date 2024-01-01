package app;

import interactive.list.ListTestUtils.SimpleCell;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.mfxresources.fonts.MFXIconWrapper;
import io.github.palexdev.mfxresources.fonts.fontawesome.FontAwesomeSolid;
import io.github.palexdev.virtualizedfx.list.paginated.VFXPaginatedList;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.Utils;

import static utils.Utils.debugView;

public class Playground extends Application {

	@Override
	public void start(Stage primaryStage) {
		VBox pane = new VBox(30);
		pane.setPadding(InsetsBuilder.top(15));
		pane.setAlignment(Pos.TOP_CENTER);

		VFXPaginatedList<Integer, SimpleCell> list = new VFXPaginatedList<>(
			Utils.items(50),
			SimpleCell::new
		);
		list.setCellsPerPage(15);
		list.stateProperty().addListener(i -> System.out.println(list.getState().getRange()));

		MFXIconWrapper next = new MFXIconWrapper(new MFXFontIcon(FontAwesomeSolid.CHEVRON_RIGHT))
			.setSize(48)
			.enableRippleGenerator(true)
			.makeRound(true);
		next.setOnMouseClicked(e -> list.next());
		MFXIconWrapper doIt = new MFXIconWrapper(new MFXFontIcon(FontAwesomeSolid.VIAL_CIRCLE_CHECK))
			.setSize(48)
			.enableRippleGenerator(true)
			.makeRound(true);
		doIt.setOnMouseClicked(e -> testIt(list));
		MFXIconWrapper previous = new MFXIconWrapper(new MFXFontIcon(FontAwesomeSolid.CHEVRON_LEFT))
			.setSize(48)
			.enableRippleGenerator(true)
			.makeRound(true);
		previous.setOnMouseClicked(e -> list.previous());

		HBox box = new HBox(30, previous, doIt, next);
		box.setAlignment(Pos.CENTER);

		pane.getChildren().addAll(list, box);
		Scene scene = new Scene(pane, 600, 800);
		primaryStage.setScene(scene);
		primaryStage.show();
		debugView(null, list);
	}

	void testIt(VFXPaginatedList<Integer, SimpleCell> list) {
		list.setCellsPerPage(8);
	}
}
