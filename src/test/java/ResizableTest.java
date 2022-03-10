import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
        SimpleVirtualFlow<String, Cell<String>> virtualFlow = new SimpleVirtualFlow<>(
		        strings,
		        SimpleCell::new,
		        Orientation.VERTICAL
        );

        stackPane.getChildren().add(virtualFlow);
        Scene scene = new Scene(stackPane, 250, 450);
        primaryStage.setScene(scene);
        primaryStage.show();

        ScenicView.show(scene);
    }

    private Label build(String s) {
        Label label = new Label(s);
        label.setPadding(new Insets(0, 0, 0, 10));
        return label;
    }
}
