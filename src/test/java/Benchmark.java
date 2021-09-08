import io.github.palexdev.virtualizedfx.cell.ISimpleCell;
import io.github.palexdev.virtualizedfx.enums.Gravity;
import io.github.palexdev.virtualizedfx.flow.simple.SimpleVirtualFlow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

@SuppressWarnings("All")
public class Benchmark extends Application {
    private int counter = 0;
    private int layoutCounter = 0;
    private final BorderPane bp = new BorderPane();

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(bp, 200, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("\nNumber of LISTVIEW cell creations/layouts when:");
        ListView<String> lv = new ListView<>(getItems());
        lv.setCellFactory(this::cellFactory);
        lv.setFixedCellSize(16.0);
        benchmark(lv, lv.getItems());

        runLater(1000, () -> {
            ObservableList<String> flowItems = getItems();
            System.out.println("\nNumber of VIRTUALFLOW cell creations/layouts when:");
            SimpleVirtualFlow<String, ISimpleCell> flow = SimpleVirtualFlow.Builder.create(
                    flowItems,
                    this::reg,
                    Gravity.TOP_BOTTOM
            );
            benchmark(flow, flowItems);
        });
    }

    private ObservableList<String> getItems() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (int i = 0; i < 20; ++i) {
            items.addAll("red", "green", "blue", "purple");
        }
        return items;
    }

    private void benchmark(Parent listCtrl, ObservableList<String> items) {
        bp.setCenter(listCtrl);

        runLater(125, () -> {
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Updating an item in the viewport
            items.set(10, "yellow");
        });

        runLater(250, () -> {
            // Print counters of previous operation
            System.out.println(" - updating an item in the viewport: " + counter + "/" + layoutCounter);
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Updating an item outside the viewport
            items.set(30, "yellow");
        });

        runLater(375, () -> {
            // Print counters of previous operation
            System.out.println(" - updating an item outside the viewport: " + counter + "/" + layoutCounter);
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Deleting an item in the middle of the viewport
            items.remove(12);
        });

        runLater(500, () -> {
            // Print counters of previous operation
            System.out.println(" - deleting an item in the middle of the viewport: " + counter + "/" + layoutCounter);
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Adding an item in the middle of the viewport
            items.add(12, "yellow");
        });

        runLater(725, () -> {
            // Print counters of previous operation
            System.out.println(" - adding an item in the middle of the viewport: " + counter + "/" + layoutCounter);
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Scrolling 5 items down
            if (listCtrl instanceof ListView) ((ListView<?>) listCtrl).scrollTo(5);
            else ((SimpleVirtualFlow) listCtrl).scrollTo(5);
        });

        runLater(850, () -> {
            // Print counters of previous operation
            System.out.println(" - scrolling 5 items down: " + counter + "/" + layoutCounter);
            // Reset counters
            counter = 0;
            layoutCounter = 0;
            // Scrolling 50 items down
            if (listCtrl instanceof ListView) ((ListView<?>) listCtrl).scrollTo(55);
            else ((SimpleVirtualFlow) listCtrl).scrollTo(55);
        });

        runLater(975, () -> {
            // Print counters of previous operation
            System.out.println(" - scrolling 50 items down: " + counter + "/" + layoutCounter);
        });
    }

    private ListCell<String> cellFactory(ListView<String> lv) {
        return new ListCell<>() {
            {
                setStyle("-fx-padding: 0");
            }

            @Override
            protected void updateItem(String color, boolean empty) {
                super.updateItem(color, empty);
                setText(null);
                if (empty) {
                    setGraphic(null);
                } else {
                    counter += 1;
                    Region reg = new Region() {
                        @Override
                        protected void layoutChildren() {
                            layoutCounter += 1;
                            super.layoutChildren();
                        }
                    };
                    reg.setPrefHeight(16);
                    reg.setStyle("-fx-background-color: " + color);
                    setGraphic(reg);
                }
            }
        };
    }

    private ISimpleCell reg(String color) {
        counter += 1;
        Region reg = new Region() {
            @Override
            protected void layoutChildren() {
                layoutCounter += 1;
                super.layoutChildren();
            }
        };
        reg.setPrefHeight(16);
        reg.setStyle("-fx-background-color: " + color);
        return ISimpleCell.wrapNode(reg, -1, 16);
    }

    /**
     * Delays the execution of a Runnable on the FX thread by <code>time</code> milliseconds.
     *
     * @param time  Milliseconds to wait before executing <code>runFX</code>.
     * @param runFX Runnable to execute on the FX thread.
     */
    public void runLater(final long time, final Runnable runFX) {
        new Thread(() -> {
            long t0 = System.currentTimeMillis();
            long t1 = t0 + time;

            while (t0 < t1) try {
                Thread.sleep(t1 - t0);
            } catch (Exception e) {
            } finally {
                t0 = System.currentTimeMillis();
            }
            Platform.runLater(runFX);
        }, "DelayedFx Thread").start();
    }
}
