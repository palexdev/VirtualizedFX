package io.github.palexdev.virtualizedfx.table.paginated;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableIntegerProperty;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.cell.TableCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsLayoutMode;
import io.github.palexdev.virtualizedfx.table.*;
import io.github.palexdev.virtualizedfx.utils.VSPUtils;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.control.Skin;

import java.util.List;

/**
 * Extension of {@link VirtualTable} to offer pagination features.
 * <p></p>
 * In addition to all the inherited features this adds:
 * <p> - The current displayed page, {@link #currentPageProperty()}
 * <p> - The max number of pages, {@link #maxPageProperty()}
 * <p> - The number of rows per page, {@link #rowsPerPageProperty()}. Note that this is also
 * settable via CSS
 * <p>
 * Note that pages start from index 1.
 * <p></p>
 * This table also has its own skin which is basically the same as {@link VirtualTable} but adapted to
 * resize the control depending on the {@link #rowsPerPageProperty()}
 * <p></p>
 * Little tips and <b>warnings</b>:
 * <p>
 * Note that this is a naive implementation. Some things/components, like the {@link TableState} or {@link TableRow}, have been
 * adapted to work with this but if you are not careful you could potentially break or mess things up.
 * <p>
 * For example: the correct way to scroll is to change the current page property, but nothing prevents you to set the position
 * using the related setters.
 * To be more precise you can still set the hPos freely as pages are vertically arranged.
 * <p>
 * This table is intended to use implementations of {@link PaginatedHelper}. Again, nothing prevents you from setting a
 * {@link #tableHelperSupplierProperty()} that only implements {@link TableHelper}, don't do that! In such case note that
 * {@link #goToPage(int)} won't work and will end with an exception.
 */
public class PaginatedVirtualTable<T> extends VirtualTable<T> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "paginated-virtual-table";

	private final IntegerProperty currentPage = new SimpleIntegerProperty(1) {
		@Override
		public void set(int newValue) {
			int clamped = NumberUtils.clamp(newValue, 1, getMaxPage());
			super.set(clamped);
		}

		@Override
		protected void invalidated() {
			int page = get();
			changePage(page);
		}
	};

	private final ReadOnlyIntegerWrapper maxPage = new ReadOnlyIntegerWrapper() {
		@Override
		public void set(int newValue) {
			int clamped = Math.max(1, newValue);
			super.set(clamped);
		}

		@Override
		protected void invalidated() {
			int max = get();
			int curr = NumberUtils.clamp(getCurrentPage(), 1, max);
			setCurrentPage(curr);
		}
	};

	//================================================================================
	// Constructors
	//================================================================================
	public PaginatedVirtualTable() {
		super();
		initialize();
	}

	public PaginatedVirtualTable(ObservableList<T> items, TableColumn<T, ? extends TableCell<T>>... columns) {
		super(items, columns);
		initialize();
	}

	public PaginatedVirtualTable(ObservableList<T> items, ObservableList<TableColumn<T, ? extends TableCell<T>>> tableColumns) {
		super(items, tableColumns);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setTableHelperSupplier(() -> {
			ColumnsLayoutMode mode = getColumnsLayoutMode();
			return (mode == ColumnsLayoutMode.FIXED) ?
					new PaginatedHelper.FixedPaginatedTableHelper(this) :
					new PaginatedHelper.VariablePaginatedTableHelper(this);
		});
	}

	/**
	 * Shortcut for {@link #setRowsPerPage(int)}.
	 * When the {@link #currentPageProperty()} is invalidated {@link #changePage(int)} is automatically called
	 */
	public void goToPage(int page) {
		setCurrentPage(page);
	}

	/**
	 * Shortcut for {@link #goToPage(int)} with 1 as parameter.
	 */
	public void goToFirstPage() {
		goToPage(1);
	}

	/**
	 * Shortcut for {@link #goToPage(int)} with {@link #getMaxPage()} as parameter.
	 */
	public void goToLastPage() {
		goToPage(getMaxPage());
	}

	/**
	 * Responsible for updating {@link #maxPageProperty()} when needed.
	 * <p>
	 * The value is given by {@code Math.ceil(rows / rowsPerPage)}.
	 */
	public void updateMaxPage() {
		int rows = getItems().size();
		int rpp = getRowsPerPage();
		int max = (int) Math.ceil(rows / (double) rpp);
		setMaxPage(max);
	}

	/**
	 * Gets the current {@link PaginatedHelper} and calls {@link PaginatedHelper#goToPage(int)}, but
	 * before doing so it ensures that the max page is correct by calling {@link #updateMaxPage()}.
	 */
	protected void changePage(int page) {
		TableHelper helper = getTableHelper();
		if (!(helper instanceof PaginatedHelper))
			throw new IllegalStateException("The table's helper is not of type PaginatedHelper!");

		updateMaxPage();
		PaginatedHelper pHelper = (PaginatedHelper) helper;
		pHelper.goToPage(page);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * The paginated table cannot scroll to a desired row.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void scrollToFirstRow() {
		throw new UnsupportedOperationException("The paginated table cannot scroll to a desired row");
	}

	/**
	 * The paginated table cannot scroll to a desired row.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void scrollToLastRow() {
		throw new UnsupportedOperationException("The paginated table cannot scroll to a desired row");
	}

	@Override
	protected void onCellHeightChanged() {
		TableHelper helper = getTableHelper();
		if (helper != null) {
			helper.computeEstimatedSize();
		}

		if (getWidth() != 0.0 && getHeight() != 0.0) { // TODO test with w and h = 0 initially
			if (!getViewportManager().init()) {
				requestViewportLayout();
			} else {
				goToPage(1);
				scrollToColumn(0);
			}
		}
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new PaginatedVirtualTableSkin<>(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	@Override
	public VirtualScrollPane wrap() {
		return VSPUtils.wrap(this);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableIntegerProperty rowsPerPage = new StyleableIntegerProperty(
			StyleableProperties.ROWS_PER_PAGE,
			this,
			"rowsPerPage",
			5
	) {
		@Override
		protected void invalidated() {
			updateMaxPage();
			changePage(getCurrentPage());
			requestViewportLayout();
		}
	};

	public int getRowsPerPage() {
		return rowsPerPage.get();
	}

	/**
	 * Specifies the number of rows to display per page.
	 * <p></p>
	 * Note that this, combined with {@link #cellHeightProperty()}, determines the height of the virtual table.
	 * <p></p>
	 * This is settable via CSS with the "-fx-rows-per-page" property.
	 */
	public StyleableIntegerProperty rowsPerPageProperty() {
		return rowsPerPage;
	}

	public void setRowsPerPage(int rowsPerPage) {
		this.rowsPerPage.set(rowsPerPage);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<PaginatedVirtualTable<?>> FACTORY = new StyleablePropertyFactory<>(VirtualTable.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<PaginatedVirtualTable<?>, Number> ROWS_PER_PAGE =
				FACTORY.createSizeCssMetaData(
						"-fx-rows-per-page",
						PaginatedVirtualTable::rowsPerPageProperty,
						5
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					VirtualTable.getClassCssMetaData(),
					ROWS_PER_PAGE
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public int getCurrentPage() {
		return currentPage.get();
	}

	/**
	 * Specifies the current displayed page.
	 */
	public IntegerProperty currentPageProperty() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage.set(currentPage);
	}

	public int getMaxPage() {
		return maxPage.get();
	}

	/**
	 * Specifies the maximum number of pages, aka last page number.
	 */
	public ReadOnlyIntegerProperty maxPageProperty() {
		return maxPage.getReadOnlyProperty();
	}

	protected void setMaxPage(int maxPage) {
		this.maxPage.set(maxPage);
	}
}
