module VirtualizedFX {
	requires transitive javafx.base;
	requires transitive javafx.graphics;
	requires transitive javafx.controls;

	requires transitive mfx.core;
	requires transitive mfx.effects;
	requires transitive mfx.resources;

	// Base Package
	exports io.github.palexdev.virtualizedfx;

	// Beans Package
	exports io.github.palexdev.virtualizedfx.beans;

	// Cell Package
	exports io.github.palexdev.virtualizedfx.cell;

	// Control Package
	exports io.github.palexdev.virtualizedfx.controls;
	exports io.github.palexdev.virtualizedfx.controls.behavior;
	exports io.github.palexdev.virtualizedfx.controls.skins;

	// Enums
	exports io.github.palexdev.virtualizedfx.enums;

	// Flow Package
	exports io.github.palexdev.virtualizedfx.flow;
	exports io.github.palexdev.virtualizedfx.flow.paginated;

	// Grid Package
	exports io.github.palexdev.virtualizedfx.grid;
	exports io.github.palexdev.virtualizedfx.grid.paginated;

	// Table Package
	exports io.github.palexdev.virtualizedfx.table;
	exports io.github.palexdev.virtualizedfx.table.paginated;
	exports io.github.palexdev.virtualizedfx.table.defaults;

	// Utils
	exports io.github.palexdev.virtualizedfx.utils;

	// Deprecated Flow Package
	exports io.github.palexdev.virtualizedfx.unused.base;
	exports io.github.palexdev.virtualizedfx.unused.simple;
}
