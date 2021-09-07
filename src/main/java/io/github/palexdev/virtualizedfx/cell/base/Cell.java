package io.github.palexdev.virtualizedfx.cell.base;

import javafx.scene.Node;

public interface Cell {
    static <N extends Node> Cell wrapNode(N node) {
        return new Cell() {

            @Override
            public N getNode() { return node; }

            @Override
            public String toString() { return node.toString(); }
        };
    }

    Node getNode();

    default void updateIndex(int index) {}

    default void afterLayout() {}
    default void beforeLayout() {}
    default void dispose() {}
}
