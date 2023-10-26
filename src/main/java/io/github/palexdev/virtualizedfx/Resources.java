package io.github.palexdev.virtualizedfx;

import java.net.URL;

public class Resources {

	//================================================================================
	// Constructors
	//================================================================================
	private Resources() {}

	//================================================================================
	// Static Methods
	//================================================================================
	public static URL getResource(String name) {
		return Resources.class.getResource(name);
	}

	public static String loadResource(String name) {
		return getResource(name).toExternalForm();
	}
}
