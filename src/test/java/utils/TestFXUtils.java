package utils;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.testfx.api.FxToolkit;

import java.util.concurrent.TimeoutException;

import static interactive.table.TableTestUtils.rowsCounter;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFXUtils {
	//================================================================================
	// Static Properties
	//================================================================================
	public static final Counter counter = new Counter();
	public static final double FP_ASSERTIONS_DELTA = 2.0;

	//================================================================================
	// Constructors
	//================================================================================
	private TestFXUtils() {}

	//================================================================================
	// Methods
	//================================================================================
	public static void assertCounter(int created, int layouts, int ixUpdates, int itUpdates, int deCached, int cached, int disposed) {
		assertEquals(created, counter.created);
		assertEquals(layouts, counter.getLayoutCnt());
		assertEquals(ixUpdates, counter.getUpdIndexCnt());
		assertEquals(itUpdates, counter.getUpdItemCnt());
		assertEquals(deCached, counter.getFromCache());
		assertEquals(cached, counter.getToCache());
		assertEquals(disposed, counter.getDisposed());
		counter.reset();
	}

	public static void resetCounters() {
		counter.reset();
		rowsCounter.reset();
	}

	public static StackPane setupStage() {
		StackPane pane = new StackPane();
		try {
			Scene scene = new Scene(pane, 400, 400);
			FxToolkit.setupStage(s -> {
				s.setWidth(400);
				s.setHeight(400);
				s.setScene(scene);
			});
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}
		return pane;
	}

	public static class Counter {
		public int created = 0;
		private int layoutCnt = 0;
		private int updIndexCnt = 0;
		private int updItemCnt = 0;
		private int fromCache = 0;
		private int toCache = 0;
		private int disposed = 0;

		public void created() {created++;}

		public int getCreated() {
			return created;
		}

		public void layout() {layoutCnt++;}

		public int getLayoutCnt() {
			return layoutCnt;
		}

		public void index() {updIndexCnt++;}

		public int getUpdIndexCnt() {
			return updIndexCnt;
		}

		public void item() {updItemCnt++;}

		public int getUpdItemCnt() {
			return updItemCnt;
		}

		public void fCache() {fromCache++;}

		public int getFromCache() {
			return fromCache;
		}

		public void tCache() {toCache++;}

		public int getToCache() {
			return toCache;
		}

		public void disposed() {disposed++;}

		public int getDisposed() {
			return disposed;
		}

		public void reset() {
			created = 0;
			layoutCnt = 0;
			updIndexCnt = 0;
			updItemCnt = 0;
			fromCache = 0;
			toCache = 0;
			disposed = 0;
		}
	}
}
