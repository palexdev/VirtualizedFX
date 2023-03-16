package interactive;

import app.cells.flow.DetailedCell;
import app.model.Model;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(ApplicationExtension.class)
public class FlowTests {
	private static Stage stage;

	@Start
	void start(Stage stage) {
		FlowTests.stage = stage;
		stage.show();
	}

	@Test
	void testPaginatedFlow() {
		PaginatedVirtualFlow<Integer, Cell<Integer>> flow = new PaginatedVirtualFlow<>(
				FXCollections.observableArrayList(Model.integers),
				DetailedCell::new
		);
		flow.setCellSize(64);
		VirtualScrollPane vsp = flow.wrap();

		setupStage(vsp, Size.of(400, 800));
		assertNotEquals(0, flow.getMaxPage());
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

}
