import io.github.palexdev.virtualizedfx.enums.Gravity;
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
        Button add = new Button("Add Middle");
        Button addSparse = new Button("Add Sparse");
        Button deleteMiddle = new Button("Delete Middle");
        Button deleteSparse = new Button("Delete Sparse");
        Button setAll = new Button("Set All");
        Button clear = new Button("Clear");
        Button replace = new Button("Replace");
        box.getChildren().addAll(updateInside, updateOutside, add, addSparse, deleteMiddle, deleteSparse, setAll, clear, replace);

        ObservableList<String> strings = FXCollections.observableArrayList();
        IntStream.range(0, 1000).forEach(value -> strings.add(String.valueOf(value)));
        SimpleVirtualFlow<String, SimpleCell<String>> virtualFlow = SimpleVirtualFlow.Builder.create(
                strings,
                SimpleCell::new,
                Gravity.LEFT_RIGHT,
                5
        );

        virtualFlow.setPrefSize(700, 100);
        virtualFlow.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        updateInside.setOnAction(event -> virtualFlow.scrollToLast());
        updateOutside.setOnAction(event -> virtualFlow.scrollToFirst());
        add.setOnAction(event -> virtualFlow.scrollTo(30));
        addSparse.setOnAction(event -> virtualFlow.scrollBy(50.0));
        deleteMiddle.setOnAction(event -> virtualFlow.scrollToPixel(50.0));
        deleteSparse.setOnAction(event -> virtualFlow.getItems().removeAll("3", "5", "6", "9", "30"));
        setAll.setOnAction(event -> virtualFlow.getItems().setAll("SNew 1", "SNew 2", "SNew 3"));
        clear.setOnAction(event -> virtualFlow.getItems().clear());
        replace.setOnAction(event -> virtualFlow.setItems(FXCollections.observableArrayList("New1", "New2", "New3")));

        stackPane.getChildren().addAll(box, virtualFlow);
        Scene scene = new Scene(stackPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        ScenicView.show(scene);
    }
}
