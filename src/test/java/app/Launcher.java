package app;

import javafx.application.Application;

public class Launcher {

	public static void main(String[] args) {
		System.setProperty("prism.verbose", "true");
		Application.launch(Playground.class, args);
	}
}
