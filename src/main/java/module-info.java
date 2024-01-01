module VirtualizedFX {
	requires transitive javafx.controls;

	requires transitive mfx.core;
	requires transitive mfx.effects;
	requires transitive mfx.resources;

	// Base
	exports io.github.palexdev.virtualizedfx;
	exports io.github.palexdev.virtualizedfx.base;

	// Cells
	exports io.github.palexdev.virtualizedfx.cells;

	// Enums
	exports io.github.palexdev.virtualizedfx.enums;

	// List
	exports io.github.palexdev.virtualizedfx.list;
	exports io.github.palexdev.virtualizedfx.list.paginated;

	// Properties
	exports io.github.palexdev.virtualizedfx.properties;

	// Utils
	exports io.github.palexdev.virtualizedfx.utils;
}