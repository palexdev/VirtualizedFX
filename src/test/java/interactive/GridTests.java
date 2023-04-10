package interactive;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.collections.ObservableGrid;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.fx.ColorUtils;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.MFXScrollBar;
import io.github.palexdev.virtualizedfx.grid.GridRow;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(ApplicationExtension.class)
public class GridTests {
	private static Stage stage;

	@Start
	void start(Stage stage) {
		GridTests.stage = stage;
		stage.show();
	}

	@Test
	void testCorrectLayoutWithScale(FxRobot robot) {
		double width = 595.50;
		double height = 446.629;
		DoubleProperty scale = new SimpleDoubleProperty(1.0);

		List<Color> colors = IntStream.range(0, 45)
				.mapToObj(i -> ColorUtils.getRandomColor())
				.toList();
		ObservableGrid<Color> data = ObservableGrid.fromList(colors, 15);
		VirtualGrid<Color, ScalingCell> grid = new VirtualGrid<>(
				data,
				c -> new ScalingCell(c, width, height, scale)
		);
		grid.setCellSize(Size.of(width, height));
		scale.addListener(i -> grid.setCellSize(Size.of(
				width * scale.get(),
				height * scale.get()
		)));


		Button btn0 = new Button("-");
		Button btn1 = new Button("+");
		btn0.setOnAction(e -> scale.set(scale.get() / 1.15));
		btn1.setOnAction(e -> scale.set(scale.get() * 1.15));

		BorderPane root = new BorderPane(grid, null, btn1, null, btn0);
		setupStage(root, new Size(1440, 900));

		assertNotEquals(IntegerRange.of(-1), grid.getState().getRowsRange());
		GridRow<Color, ScalingCell> firstRow = grid.getState().getRowsUnmodifiable().get(0);
		firstRow.getCellsUnmodifiable().values().forEach(c -> assertEquals(0, c.getNode().getLayoutY()));

		// Increase 5 times and test again
		for (int i = 0; i < 5; i++) {
			robot.clickOn(btn1);
		}
		firstRow.getCellsUnmodifiable().values().forEach(c -> assertEquals(0, c.getNode().getLayoutY()));

		// Decrease 5 times and test again
		for (int i = 0; i < 5; i++) {
			robot.clickOn(btn0);
		}
		firstRow.getCellsUnmodifiable().values().forEach(c -> assertEquals(0, c.getNode().getLayoutY()));
	}

    @Test
    void testCorrectScrollingWithAutoHideBars(FxRobot robot) {
        double width = 200.0;
        double height = 200.0;

        List<Color> colors = IntStream.range(0, 100)
                .mapToObj(i -> ColorUtils.getRandomColor())
                .toList();
        ObservableGrid<Color> data = ObservableGrid.fromList(colors, 10);
        VirtualGrid<Color, SimpleTestCell> grid = new VirtualGrid<>(
                data,
                c -> new SimpleTestCell(c, width, height)
        );
        grid.setCellSize(Size.of(width, height));

        var vsp = VSPUtils.wrap(grid);
        vsp.setAutoHideBars(true);

        BorderPane root = new BorderPane(vsp, null, null, null, null);
        setupStage(root, new Size(1000, 900));

        var child = (MFXScrollBar) vsp.getChildrenUnmodifiable().get(1);
        var thumb = child.getChildrenUnmodifiable().get(1);

        robot.drag(thumb);

        robot.moveBy(-10.0, 10.0);
        Assertions.assertDoesNotThrow(() -> robot.moveBy(20.0, 10.0));
    }

    static class SimpleTestCell implements GridCell<Color> {
        private final Rectangle rt;

        SimpleTestCell(Color color, double width, double height) {
            rt = new Rectangle(width, height, color);
        }

        @Override
        public Node getNode() {
            return rt;
        }

        @Override
        public void updateItem(Color item) {
            rt.setFill(item);
        }
    }

	//================================================================================
	// Utilities
	//================================================================================
	private void setupStage(Parent content, Size sceneSize) {
		try {
			Scene scene = new Scene(content, sceneSize.getWidth(), sceneSize.getHeight());
			FxToolkit.setupStage(s -> s.setScene(scene));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	//================================================================================
	// Internal Classes
	//================================================================================
	static class ScalingCell implements GridCell<Color> {
		private DoubleProperty scale;
		private final double width;
		private final double height;
		private final Rectangle rt;

		public ScalingCell(Color color, double width, double height, DoubleProperty scale) {
			this.width = width;
			this.height = height;
			this.scale = scale;
			this.rt = new Rectangle(width, height, color);
			scale.addListener(i -> {
				rt.setWidth(ScalingCell.this.width * scale.get());
				rt.setHeight(ScalingCell.this.height * scale.get());
			});
		}

		@Override
		public Node getNode() {
			return rt;
		}

		@Override
		public void updateItem(Color item) {
			rt.setFill(item);
		}

		@Override
		public void dispose() {
			When.disposeFor(scale);
			scale = null;
		}
	}
}
