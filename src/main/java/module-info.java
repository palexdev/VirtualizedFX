module VirtualizedFX {
	requires transitive javafx.base;
	requires transitive javafx.graphics;
	requires transitive javafx.controls;

	requires transitive MFXCore;
	requires transitive MFXResources;

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

	// Deprecated Flow Package
	exports io.github.palexdev.virtualizedfx.unused.base;
	exports io.github.palexdev.virtualizedfx.unused.simple;
	exports io.github.palexdev.virtualizedfx.flow.paginated;

}
