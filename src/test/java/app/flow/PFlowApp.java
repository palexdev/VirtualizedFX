package app.flow;

import app.Tests;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PFlowApp extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		Scene scene = new Scene(Tests.PFLOW.load());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Paginated VirtualFlow Test");
		primaryStage.show();
	}
}