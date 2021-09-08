package io.github.palexdev.virtualizedfx.cell;

import javafx.scene.Node;

/**
 * Extension of {@link Cell}.
 * <p>
 * SimpleCells are cells that have fixed sizes (both height and width)
 * so this makes it easy to estimate what the totalHeight/totalWidth will be.
 * <p></p>
 * Also offers a static method to quickly wrap a node into a cell.
 */
public interface ISimpleCell extends Cell {
    static <N extends Node> ISimpleCell wrapNode(N node, double width, double height) {
        return new ISimpleCell() {

            @Override
            public double getFixedHeight() {
                return height;
            }

            @Override
            public double getFixedWidth() {
                return width;
            }

            @Override
            public N getNode() { return node; }

            @Override
            public String toString() { return node.toString(); }
        };
    }

    /**
     * The cell's height.
     * <p>
     * In case of a Horizontal VirtualFlow you probably want this to
     * return -1. By returning a negative value the height will be the same
     * as the VirtualFlow.
     * <p>
     * Please DO NOT attempt to make this method dynamic!
     */
    double getFixedHeight();

    /**
     * The cell's width.
     * <p>
     * In case of a Vertical VirtualFlow you probably want this to
     * return -1. By returning a negative value the width will be the same
     * as the VirtualFlow.
     * <p>
     * Please DO NOT attempt to make this method dynamic!
     */
    double getFixedWidth();
}
