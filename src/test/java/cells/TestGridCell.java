package cells;

import javafx.geometry.Pos;

public class TestGridCell extends TestCell<Integer> {
	public TestGridCell(Integer item) {
		super(item);
		setAlignment(Pos.CENTER);
	}
}
