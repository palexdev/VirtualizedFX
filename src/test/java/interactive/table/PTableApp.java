package interactive.table;

import interactive.Tests;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.scenicview.ScenicView;

public class PTableApp extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		System.setProperty("prism.lcdtext", "false");
		Scene scene = new Scene(Tests.PTABLE.load());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Paginated VirtualTable Test");
		primaryStage.show();
		ScenicView.show(scene);
	}
}
