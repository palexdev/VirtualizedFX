package io.github.palexdev.virtualizedfx.base;

import java.util.List;

/**
 * A simple interface that makes any implementor node define a list of default style classes to use in CSS.
 */
public interface VFXStyleable {

	/**
	 * @return a list containing all the component's default style classes
	 */
	List<String> defaultStyleClasses();

}
