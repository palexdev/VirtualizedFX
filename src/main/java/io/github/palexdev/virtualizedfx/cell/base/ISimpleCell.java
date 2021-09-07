package io.github.palexdev.virtualizedfx.cell.base;

import javafx.scene.Node;

public interface ISimpleCell extends Cell {
    static <N extends Node> ISimpleCell wrapNode(N node) {
        return new ISimpleCell() {

            @Override
            public double getFixedHeight() {
                return 30;
            }

            @Override
            public double getFixedWidth() {
                return -1;
            }

            @Override
            public N getNode() { return node; }

            @Override
            public String toString() { return node.toString(); }
        };
    }

    double getFixedHeight();
    double getFixedWidth();
}
