import io.github.palexdev.virtualizedfx.cell.base.ISimpleCell;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.scenicview.ScenicView;

import java.util.List;
import java.util.stream.IntStream;

public class SimpleVirtualFlowTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane stackPane = new StackPane();

        HBox box = new HBox(20);
        box.setAlignment(Pos.CENTER);
        StackPane.setAlignment(box, Pos.TOP_CENTER);
        StackPane.setMargin(box, new Insets(20, 0, 0, 0));
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Button updateInside = new Button("Update Inside");
        Button updateOutside = new Button("Update Outside");
        Button addMiddle = new Button("Add Middle");
        Button addSparse = new Button("Add Sparse");
        Button deleteMiddle = new Button("Delete Middle");
        Button deleteSparse = new Button("Delete Sparse");
        Button setAll = new Button("Set All");
        Button clear = new Button("Clear");
        box.getChildren().addAll(updateInside, updateOutside, addMiddle, addSparse, deleteMiddle, deleteSparse, setAll, clear);

        ObservableList<String> strings = FXCollections.observableArrayList();
        IntStream.range(0, 1000).forEach(value -> strings.add("String " + value));
        SimpleVirtualFlow<String, ISimpleCell> virtualFlow = new SimpleVirtualFlow.Builder<String, ISimpleCell>()
                .setCellFactory(SimpleCell::new)
                .setItems(strings)
                .setOverscan(5)
                .build();

        virtualFlow.setPrefSize(200, 500);
        virtualFlow.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        virtualFlow.setStyle("-fx-background-color: white;\n" + "-fx-border-color: red");

        updateInside.setOnAction(event -> strings.set(12, "Replaced"));
        updateOutside.setOnAction(event -> strings.set(30, "Replaced Outside"));
        addMiddle.setOnAction(event -> strings.add(13, "ChangeBean Middle"));
        addSparse.setOnAction(event -> strings.addAll(13, List.of("String New1", "String New2", "String New3")));
        deleteMiddle.setOnAction(event -> strings.remove(0));
        deleteSparse.setOnAction(event -> strings.removeAll("String 3", "String 5", "String 6"));
        setAll.setOnAction(event -> strings.setAll("New 1", "New 2", "New 3"));
        clear.setOnAction(event -> strings.clear());

        stackPane.getChildren().addAll(box, virtualFlow);
        Scene scene = new Scene(stackPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        ScenicView.show(scene);
    }
}
