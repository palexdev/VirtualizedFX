module VirtualizedFX {
	requires transitive javafx.base;
	requires transitive javafx.graphics;
	requires transitive javafx.controls;

	exports io.github.palexdev.virtualizedfx;
	
	// Beans Package
	exports io.github.palexdev.virtualizedfx.beans;
	
	// Cell Package
	exports io.github.palexdev.virtualizedfx.cell;
	
	// Collections Package
	exports io.github.palexdev.virtualizedfx.collections;
	
	// Flow Package
	exports io.github.palexdev.virtualizedfx.flow.base;
	exports io.github.palexdev.virtualizedfx.flow.simple;
	
	// Utils Package
	exports io.github.palexdev.virtualizedfx.utils;
}
