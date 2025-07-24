module VirtualizedFX {
    requires transitive javafx.controls;

    requires transitive mfx.core;
    requires transitive mfx.effects;

    // Base
    exports io.github.palexdev.virtualizedfx;
    exports io.github.palexdev.virtualizedfx.base;

    // Cells
    exports io.github.palexdev.virtualizedfx.cells;
    exports io.github.palexdev.virtualizedfx.cells.base;

    // Controls
    exports io.github.palexdev.virtualizedfx.controls;
    exports io.github.palexdev.virtualizedfx.controls.behaviors;
    exports io.github.palexdev.virtualizedfx.controls.skins;

    // Enums
    exports io.github.palexdev.virtualizedfx.enums;

    // Events
    exports io.github.palexdev.virtualizedfx.events;

    // Grid
    exports io.github.palexdev.virtualizedfx.grid;

    // List
    exports io.github.palexdev.virtualizedfx.list;
    exports io.github.palexdev.virtualizedfx.list.paginated;

    // Properties
    exports io.github.palexdev.virtualizedfx.properties;

    // Table
    exports io.github.palexdev.virtualizedfx.table;
    exports io.github.palexdev.virtualizedfx.table.defaults;

    // Utils
    exports io.github.palexdev.virtualizedfx.utils;
}