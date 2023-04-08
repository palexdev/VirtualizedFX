/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package app.cells.flow;

import app.cells.CellsDebugger;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.animation.Animation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

public abstract class CommonCell extends HBox implements Cell<Integer> {
    protected final Label label;
    protected Integer item;
    protected int index;
    protected Animation bAnimation;

    public CommonCell(Integer item) {
        this.item = item;
        label = new Label(dataToString());
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        setPrefSize(64, 64);
        setAlignment(Pos.CENTER_LEFT);
        setHgrow(label, Priority.ALWAYS);
        getStyleClass().add("cell");
        getChildren().setAll(label);

        StyleUtils.setBackground(this, Color.TRANSPARENT);

        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            updateItem(item);
            Animations.PauseBuilder.build()
                    .setDuration(GlobalCellParameters.getAnimationDuration())
                    .setOnFinished(end -> {
                        if (bAnimation != null && Animations.isStopped(bAnimation))
                            StyleUtils.setBackground(this, Color.TRANSPARENT);
                    })
                    .getAnimation()
                    .play();
        });

        addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (bAnimation != null && !Animations.isPlaying(bAnimation))
                StyleUtils.setBackground(this, Color.TRANSPARENT);
        });
    }

    @Override
    public Node getNode() {
        return this;
    }

    @Override
    public void updateIndex(int index) {
        this.index = index;
        label.setText(dataToString());
    }

    @Override
    public void updateItem(Integer item) {
        this.item = item;
        label.setText(dataToString());

        if (bAnimation != null && Animations.isPlaying(bAnimation)) {
            bAnimation.stop();
            StyleUtils.setBackground(this, Color.TRANSPARENT);
        }

        if (GlobalCellParameters.isAnimEnabled()) {
            bAnimation = CellsDebugger.animateBackground(this, GlobalCellParameters.getAnimationDuration());
            bAnimation.play();
        }
    }

    protected abstract String dataToString();

    @Override
    public void dispose() {
        if (bAnimation != null) {
            bAnimation.stop();
            bAnimation = null;
            StyleUtils.setBackground(this, Color.TRANSPARENT);
        }
    }
}
