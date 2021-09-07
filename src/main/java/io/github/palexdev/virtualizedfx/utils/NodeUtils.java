package io.github.palexdev.virtualizedfx.utils;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

public class NodeUtils {

    /**
     * Convenience method to execute a given action after that the given control
     * has been laid out and its skin is not null anymore.
     * <p></p>
     * If the skin is not null when called, the action is executed immediately.
     * <p>
     * The listener is added only if the skin is null or the addListenerIfNotNull parameter is true.
     *
     * @param control              the control to check for skin initialization
     * @param action               the action to perform when the skin is not null
     * @param addListenerIfNotNull to specify if the listener should be added anyway even if the scene is not null
     * @param isOneShot            to specify if the listener added to the skin property
     *                             should be removed after it is not null anymore
     */
    public static void waitForSkin(Control control, Runnable action, boolean addListenerIfNotNull, boolean isOneShot) {
        if (control.getSkin() != null) {
            action.run();
        }

        if (control.getSkin() == null || addListenerIfNotNull) {
            control.skinProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Skin<?>> observable, Skin<?> oldValue, Skin<?> newValue) {
                    if (newValue != null) {
                        action.run();
                        if (isOneShot) {
                            control.skinProperty().removeListener(this);
                        }
                    }
                }
            });
        }
    }

    /**
     * Convenience method to execute a given action after that the given node
     * has been laid out and its scene is not null anymore.
     * <p></p>
     * If the scene is not null when called, the action is executed immediately.
     * <p>
     * The listener is added only if the scene is null or the addListenerIfNotNull parameter is true.
     *
     * @param node              the node to check for scene initialization
     * @param action               the action to perform when the scene is not null
     * @param addListenerIfNotNull to specify if the listener should be added anyway even if the scene is not null
     * @param isOneShot            to specify if the listener added to the scene property
     *                             should be removed after it is not null anymore
     */
    public static void waitForScene(Node node, Runnable action, boolean addListenerIfNotNull, boolean isOneShot) {
        if (node.getScene() != null) {
            action.run();
        }

        if (node.getScene() == null || addListenerIfNotNull) {
            node.sceneProperty().addListener(new ChangeListener<>() {
                @Override
                public void changed(ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {
                    if (newValue != null) {
                        action.run();
                        if (isOneShot) {
                            node.sceneProperty().removeListener(this);
                        }
                    }
                }
            });
        }
    }
}
