package utils;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import org.scenicview.ScenicView;
import org.testfx.api.FxRobot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Utils {

	//================================================================================
	// Constructors
	//================================================================================
	private Utils() {}

	//================================================================================
	// Static Methods
	//================================================================================
	public static void debugView(FxRobot robot, Node node) {
		if (robot != null) {
			robot.interact(() -> ScenicView.show(node.getScene()));
			return;
		}
		ScenicView.show(node.getScene());
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception ignored) {}
	}

	public static void setWindowSize(Node node, double size) {
		setWindowSize(node, size, size);
	}

	public static void setWindowSize(Node node, double w, double h) {
		Scene scene = node.getScene();
		if (scene == null) throw new NullPointerException("Node is not in a Scene");
		Window window = scene.getWindow();
		if (window == null) throw new NullPointerException("Scene is not in a Window");
		if (w >= 0) window.setWidth(w);
		if (h >= 0) window.setHeight(h);
	}

	public static <T> void removeAll(VFXContainer<T> container, int... indexes) {
		List<T> rem = Arrays.stream(indexes)
			.mapToObj(container.getItems()::get)
			.toList();
		container.getItems().removeAll(rem);
	}

	public static <T> void removeAll(VFXContainer<T> container, IntegerRange range) {
		List<T> rem = IntStream.rangeClosed(range.getMin(), range.getMax())
			.mapToObj(container.getItems()::get)
			.toList();
		container.getItems().removeAll(rem);
	}

	public static ObservableList<Integer> items(int cnt) {
		return FXCollections.observableArrayList(IntegerRange.expandRangeToArray(0, cnt - 1));
	}

	public static ObservableList<Integer> items(int start, int cnt) {
		return IntStream.range(0, cnt)
			.mapToObj(i -> start + i)
			.collect(FXCollectors.toList());
	}

	public static boolean isTouchSupported() {
		String env = System.getenv().getOrDefault("TOUCH_SUPPORTED", "false");
		return Boolean.parseBoolean(env);
	}
}
