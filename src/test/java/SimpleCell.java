import io.github.palexdev.virtualizedfx.cell.Cell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class SimpleCell<T> extends HBox implements Cell<T> {
    private final Label label;

    public SimpleCell(T data) {
        setMinHeight(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);
        setPrefHeight(32);
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(5);

        label = new Label(data.toString());
        label.setPadding(new Insets(0, 0, 0, 10));
        getChildren().setAll(label);
    }

    @Override
    public Node getNode() {
        return this;
    }

    @Override
    public void updateItem(T item) {
        label.setText(item.toString());
    }
}
