import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.enums.Gravity;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.scenicview.ScenicView;

import java.util.stream.IntStream;

public class ResizableTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane stackPane = new StackPane();

        ObservableList<String> strings = FXCollections.observableArrayList();
        IntStream.rangeClosed(0, 50).forEach(i -> strings.add("S" + i));
        SimpleVirtualFlow<String, ISimpleCell> virtualFlow = SimpleVirtualFlow.Builder.create(
                strings,
                SimpleCell::new,
                Gravity.TOP_BOTTOM
        );

        virtualFlow.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
/*                virtualFlow.setCellFactory(s -> ISimpleCell.wrapNode(
                        build(s + " NewCellFactory!"),
                        -1, 32
                ));*/
                virtualFlow.setGravity(virtualFlow.getGravity() == Gravity.TOP_BOTTOM ? Gravity.LEFT_RIGHT : Gravity.TOP_BOTTOM);
            }
        });

        stackPane.getChildren().add(virtualFlow);
        Scene scene = new Scene(stackPane, 250, 450);
        primaryStage.setScene(scene);
        primaryStage.show();

        ScenicView.show(scene);
    }

    private Label build(String s) {
        Label label = new Label(s);
        label.setPadding(new Insets(0, 0, 0, 10));
        label.setStyle("-fx-border-color: gold");
        return label;
    }
}
