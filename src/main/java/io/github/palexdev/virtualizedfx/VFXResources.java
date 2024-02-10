package io.github.palexdev.virtualizedfx;

import java.net.URL;

public class VFXResources {

	//================================================================================
	// Constructors
	//================================================================================
	private VFXResources() {}

	//================================================================================
	// Static Methods
	//================================================================================
	public static URL getResource(String name) {
		return VFXResources.class.getResource(name);
	}

	public static String loadResource(String name) {
		return getResource(name).toExternalForm();
	}
}
