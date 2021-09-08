import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class SimpleCell<T> extends HBox implements ISimpleCell {

    public SimpleCell(T data) {
        Label label = new Label(data.toString());
        label.setStyle("-fx-border-color: gold");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        label.setPadding(new Insets(0, 0, 0, 10));
        getChildren().setAll(label);
    }

    @Override
    public Node getNode() {
        return this;
    }

    @Override
    public double getFixedHeight() {
        return 32;
    }

    @Override
    public double getFixedWidth() {
        return 32;
    }
}
