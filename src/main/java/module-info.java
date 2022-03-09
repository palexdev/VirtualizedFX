module virtualizedfx {
	requires javafx.base;
	requires javafx.graphics;
	requires javafx.controls;

	exports io.github.palexdev.virtualizedfx;

	exports io.github.palexdev.virtualizedfx.beans;
	exports io.github.palexdev.virtualizedfx.cache;
	exports io.github.palexdev.virtualizedfx.cell;
	exports io.github.palexdev.virtualizedfx.collections;

	exports io.github.palexdev.virtualizedfx.flow.base;
	exports io.github.palexdev.virtualizedfx.flow.simple;

	exports io.github.palexdev.virtualizedfx.utils;
}