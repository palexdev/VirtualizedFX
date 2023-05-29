package app.table;

import app.model.User;
import app.others.Constraint;
import app.others.DialogUtils;
import app.others.Utils;
import app.table.TableTestController.AltCell;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.IntegerRange;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.TableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.SimpleTableCell;
import io.github.palexdev.virtualizedfx.table.paginated.PaginatedVirtualTable;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static app.others.DialogUtils.getIntFromUser;
import static app.others.DialogUtils.getSizeFromUser;

@SuppressWarnings({"rawtypes", "unchecked"})
public enum PTableTestActions {
	UPDATE_AT_5((p, t) -> t.getItems().set(5, User.rand())),
	UPDATE_AT_25((p, t) -> t.getItems().set(25, User.rand())),
	UPDATE_AT((p, t) -> {
		int index = DialogUtils.getIntFromUser(
				p.getRoot(),
				"Change User At",
				"Input index to change at",
				Constraint.listIndexConstraint(t.getItems(), false)
		);
		if (index < 0) return;
		t.getItems().set(index, User.rand());
	}),
	ADD_AT_0((p, t) -> t.getItems().add(0, User.rand())),
	ADD_AT_END((p, t) -> t.getItems().add(t.getItems().size(), User.rand())),
	ADD_AT((p, t) -> {
		int index = DialogUtils.getIntFromUser(
				p.getRoot(),
				"Add User At",
				"Input index to add at",
				Constraint.listIndexConstraint(t.getItems(), true)
		);
		if (index < 0) return;
		t.getItems().add(index, User.rand());
	}),
	ADD_MULTIPLE_AT_3((p, t) -> {
		List<User> newUsers = IntStream.range(0, 3)
				.mapToObj(i -> User.rand())
				.toList();
		t.getItems().addAll(3, newUsers);
	}),
	ADD_MULTIPLE_AT((p, t) -> {
		int index = DialogUtils.getIntFromUser(
				p.getRoot(),
				"Change User At",
				"Input index to change at",
				Constraint.listIndexConstraint(t.getItems(), true)
		);
		if (index < 0) return;
		List<User> newUsers = IntStream.range(0, 3)
				.mapToObj(i -> User.rand())
				.toList();
		t.getItems().addAll(index, newUsers);
	}),
	DELETE_AT_3((p, t) -> t.getItems().remove(3)),
	DELETE_SPARSE((p, t) -> t.getItems().removeAll(Utils.listGetAll(t.getItems(), 2, 5, 6, 8, 25, 53))),
	DELETE_FIRST((p, t) -> t.getItems().remove(0)),
	DELETE_LAST((p, t) -> t.getItems().remove(t.getItems().size() - 1)),
	DELETE_ALL_VISIBLE((p, t) -> {
		IntegerRange rowsRange = t.getState().getRowsRange();
		Integer[] indexes = IntegerRange.expandRangeToSet(rowsRange).toArray(Integer[]::new);
		t.getItems().removeAll(Utils.listGetAll(t.getItems(), indexes));
	}),
	REPLACE_ALL((p, t) -> t.getItems().setAll(User.randList(100))),
	SORT_BY((p, t) -> {
		String choice = DialogUtils.getChoice(p.getRoot(), "Sort Users", "Choose Comparator", List.of(
				"ID", "Name", "Age", "Height", "Gender", "Nation", "Phone", "University"
		));
		if (choice == null) {
			FXCollections.sort(t.getItems(), null);
			p.setLastComparator(null);
			return;
		}

		Comparator<User> comparator = switch (choice) {
			case "ID" -> User.ID_COMPARATOR;
			case "Name" -> User.NAME_COMPARATOR;
			case "Age" -> User.AGE_COMPARATOR;
			case "Height" -> User.HEIGHT_COMPARATOR;
			case "Gender" -> User.GENDER_COMPARATOR;
			case "Nation" -> User.NATION_COMPARATOR;
			case "Phone" -> User.PHONE_COMPARATOR;
			case "University" -> User.UNI_COMPARATOR;
			default -> null;
		};
		FXCollections.sort(t.getItems(), comparator);
		p.setLastComparator(comparator);
	}),
	REVERSE_SORT((p, t) -> {
		if (p.getLastComparator() != null) {
			Comparator<User> reversed = p.getLastComparator().reversed();
			FXCollections.sort(t.getItems(), reversed);
		}
	}),
	CLEAR_LIST((p, t) -> t.getItems().clear()),
	CHANGE_ROWS_PER_PAGE((p, t) -> {
		int rpp = getIntFromUser(
				p.getRoot(),
				"Change Rows Per Page",
				"Rows Number",
				Constraint.of("Invalid number", i -> i >= 0)
		);
		if (rpp == -1) return;
		t.setRowsPerPage(rpp);
	}),
	CHANGE_CELLS_HEIGHT_TO((p, t) -> {
		double newHeight = getSizeFromUser(p.getRoot(), "Change Cell Height To...", "Current Height: " + t.getCellHeight(), Constraint.of("Invalid!", i -> i > 0));
		if (newHeight < 0) return;
		t.setCellHeight(newHeight);
	}),
	CHANGE_COLUMNS_SIZE((p, t) -> {
		Size columnsSize = t.getColumnSize();
		Size newSize = Size.of(columnsSize.getWidth(), columnsSize.getHeight());

		double newWidth = getSizeFromUser(p.getRoot(), "Change Column Width To...", "Current Width: " + columnsSize.getWidth(), Constraint.of("Invalid!", i -> i > 0));
		if (newWidth > 0) newSize.setWidth(newWidth);

		double newHeight = getSizeFromUser(p.getRoot(), "Change Column Height To...", "Current Height: " + columnsSize.getHeight(), Constraint.of("Invalid!", i -> i > 0));
		if (newHeight > 0) newSize.setHeight(newHeight);

		t.setColumnSize(newSize);
	}),
	REPLACE_LIST((p, t) -> t.setItems(User.randObsList(150))),
	REMOVE_ROW_FACTORY((p, t) -> t.setRowFactory(null)),
	RESTORE_ROW_FACTORY((p, t) -> t.defaultRowFactory()),
	SWITCH_CELL_FACTORY_FOR((p, t) -> {
		if (t.getColumns().isEmpty()) return;
		int index = DialogUtils.getIntFromUser(
				p.getRoot(),
				"Change Column Cell Factory",
				"Choose Column By Index",
				Constraint.of("Invalid index", i -> i >= 0 && i < t.getColumns().size())
		);
		if (index == -1) return;

		TableColumn<User, ? extends TableCell<User>> column = t.getColumn(index);
		Function<User, ? extends TableCell<User>> factory;

		Function<User, ?> extractor = switch (index) {
			case 1 -> User::id;
			case 2 -> User::name;
			case 3 -> User::age;
			case 4 -> User::height;
			case 5 -> User::gender;
			case 6 -> User::nationality;
			case 7 -> User::cellPhone;
			case 8 -> User::university;
			default -> String::valueOf;
		};

		// Fuck Java generics
		if (p.getLastCell(column) == SimpleTableCell.class) {
			factory = u -> new AltCell<>(u, extractor);
			p.setLastCell(column, AltCell.class);
		} else {
			factory = u -> new SimpleTableCell<>(u, extractor);
			p.setLastCell(column, SimpleTableCell.class);
		}

		// Again fuck Java generics
		column.setCellFactory(((Function) factory));
	}),
	SWITCH_COLUMNS((p, t) -> {
		int fIndex = getIntFromUser(
				p.getRoot(),
				"Switch Columns",
				"Choose First Column By Index",
				Constraint.of("Invalid index", i -> i >= 0 && i < t.getColumns().size())
		);
		int sIndex = getIntFromUser(
				p.getRoot(),
				"Switch Columns",
				"Choose Second Column By Index",
			Constraint.of("Invalid index", i -> i >= 0 && i < t.getColumns().size())
		);
		if (fIndex == -1 || sIndex == -1) return;

		List<TableColumn<User, ? extends TableCell<User>>> tmp = new ArrayList<>(t.getColumns());
		Collections.swap(tmp, fIndex, sIndex);
		t.getColumns().setAll(tmp);
	}),
	CLEAR_COLUMNS((p, t) -> t.getColumns().clear()),
	RESET_COLUMNS((p, t) -> t.getColumns().setAll(p.getColumns())),
	AUTOSIZE_COLUMNS((p, t) -> t.autosizeColumns()),
	SWITCH_LAYOUT_MODE((p, t) -> t.setColumnsLayoutMode(EnumUtils.next(ColumnsLayoutMode.class, t.getColumnsLayoutMode()))),
	GO_TO_PAGE((p, t) -> {
		int index = getIntFromUser(p.getRoot(), "Choose Page...", "Number of pages: " + t.getMaxPage(), Constraint.of("Invalid", i -> i > 0 && i <= t.getMaxPage()));
		t.goToPage(index);
	}),
	GO_TO_FIRST_PAGE((p, t) -> t.goToFirstPage()),
	GO_TO_LAST_PAGE((p, t) -> t.goToLastPage()),
	SCROLL_TO_COLUMN((p, t) -> {
		int index = getIntFromUser(p.getRoot(), "Choose Column...", "Number of columns: " + t.getColumns().size(), Constraint.of("Invalid!", i -> i >= 0));
		t.scrollToColumn(index);
	}),
	SCROLL_HORIZONTAL_BY((p, t) -> {
		double amount = getSizeFromUser(p.getRoot(), "How Many Pixels To Scroll?", "Current Position: " + t.getHPos(), Constraint.of("Invalid!", i -> i >= 0));
		t.scrollBy(amount, Orientation.HORIZONTAL);
	}),
	SCROLL_HORIZONTAL_TO((p, t) -> {
		double val = getSizeFromUser(p.getRoot(), "Input Pixel Value To Scroll To...", "Curren Position: " + t.getHPos(), Constraint.of("Invalid!", i -> i >= 0));
		t.scrollTo(val, Orientation.HORIZONTAL);
	});

	private final BiConsumer<TableTestParameters<User>, PaginatedVirtualTable<User>> action;

	PTableTestActions(BiConsumer<TableTestParameters<User>, PaginatedVirtualTable<User>> action) {
		this.action = action;
	}

	public BiConsumer<TableTestParameters<User>, PaginatedVirtualTable<User>> getAction() {
		return action;
	}

	public void run(TableTestParameters<User> parameters, PaginatedVirtualTable<User> table) {
		action.accept(parameters, table);
	}

	private static Region getRegion(Control control) {
		assert control != null;
		Parent parent = control;
		Set<String> classes;

		parent = parent.getParent();
		classes = Optional.ofNullable(parent)
				.map(p -> new HashSet<>(p.getStyleClass()))
				.orElse(new HashSet<>());
		while (!(parent instanceof Region) || (classes.contains("viewport") || classes.contains("container"))) {
			parent = parent.getParent();
			classes = Optional.ofNullable(parent)
					.map(p -> new HashSet<>(p.getStyleClass()))
					.orElse(new HashSet<>());
		}
		return (Region) parent;
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().substring(1).replace("_", " ").toLowerCase();
	}
}
