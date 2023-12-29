package utils;

import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.fx.FXCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
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
		robot.interact(() -> ScenicView.show(node.getScene()));
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception ignored) {}
	}

	public static <T> void removeAll(List<T> list, int... indexes) {
		List<T> rem = Arrays.stream(indexes)
			.mapToObj(list::get)
			.toList();
		list.removeAll(rem);
	}

	public static <T> void removeAll(List<T> list, IntegerRange range) {
		List<T> rem = IntStream.rangeClosed(range.getMin(), range.getMax())
			.mapToObj(list::get)
			.toList();
		list.removeAll(rem);
	}

	public static ObservableList<Integer> items(int cnt) {
		return FXCollections.observableArrayList(IntegerRange.expandRangeToArray(0, cnt - 1));
	}

	public static ObservableList<Integer> items(int start, int cnt) {
		return IntStream.range(0, cnt)
			.mapToObj(i -> start + i)
			.collect(FXCollectors.toList());
	}
}
