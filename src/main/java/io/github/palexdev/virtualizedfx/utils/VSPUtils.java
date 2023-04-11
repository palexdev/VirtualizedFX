/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.utils;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.bindings.ExternalSource;
import io.github.palexdev.mfxcore.base.bindings.MFXBindings;
import io.github.palexdev.mfxcore.base.bindings.Mapper;
import io.github.palexdev.mfxcore.base.bindings.MappingSource;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.EnumUtils;
import io.github.palexdev.virtualizedfx.beans.VirtualBounds;
import io.github.palexdev.virtualizedfx.cell.Cell;
import io.github.palexdev.virtualizedfx.cell.GridCell;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.flow.OrientationHelper;
import io.github.palexdev.virtualizedfx.flow.VirtualFlow;
import io.github.palexdev.virtualizedfx.flow.paginated.PaginatedVirtualFlow;
import io.github.palexdev.virtualizedfx.grid.GridHelper;
import io.github.palexdev.virtualizedfx.grid.VirtualGrid;
import io.github.palexdev.virtualizedfx.grid.paginated.PaginatedVirtualGrid;
import io.github.palexdev.virtualizedfx.table.TableHelper;
import io.github.palexdev.virtualizedfx.table.VirtualTable;
import io.github.palexdev.virtualizedfx.table.paginated.PaginatedVirtualTable;
import javafx.beans.property.Property;
import javafx.geometry.Orientation;

/**
 * Class that offers various utilities for {@link VirtualScrollPane}
 */
public class VSPUtils {

	//================================================================================
	// Constructors
	//================================================================================
	private VSPUtils() {
	}

	//================================================================================
	// VirtualFlow
	//================================================================================

	/**
	 * Does the hard job for you by creating a new {@code VirtualScrollPane} wrapping the
	 * given {@link VirtualFlow}, initializing the needed bindings for the content bounds, the scrolling and the
	 * orientation.
	 * <p></p>
	 * <b>NOTE:</b> once this is not needed anymore you should call {@link #disposeFor(VirtualScrollPane)}
	 * to avoid memory leaks which may occur because of {@link MFXBindings}.
	 */
	public static <T, C extends Cell<T>> VirtualScrollPane wrap(VirtualFlow<T, C> flow) {
		VirtualScrollPane vsp = new VirtualScrollPane(flow);
		MFXBindings bindings = MFXBindings.instance();

		vsp.orientationProperty().bind(flow.orientationProperty());
		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				Orientation o = flow.getOrientation();
				double breadth = flow.getMaxBreadth();
				double length = flow.getEstimatedLength();
				return (o == Orientation.VERTICAL) ?
					VirtualBounds.of(flow.getWidth(), flow.getHeight(), breadth, length) :
					VirtualBounds.of(flow.getWidth(), flow.getHeight(), length, breadth);
			})
			.addSources(flow.orientationProperty(), flow.maxBreadthProperty(), flow.estimatedLengthProperty())
			.get()
		);

		MappingSource<Number, Number> vSource = MappingSource.<Number, Number>of(flow.vPosProperty())
			.setTargetUpdater(Mapper.of(val -> {
				OrientationHelper helper = flow.getOrientationHelper();
				double max = helper.maxVScroll();
				return (max != 0) ? val.doubleValue() / max : 0.0;
			}), (o, n) -> vsp.setVVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				OrientationHelper helper = flow.getOrientationHelper();
				return val.doubleValue() * helper.maxVScroll();
			}), (o, n) -> flow.setVPos(n.doubleValue()));
		bindings.bindBidirectional(vsp.vValProperty())
			.addSources(vSource)
			.addInvalidatingSource(ExternalSource.of(flow.estimatedLengthProperty(), (o, n) -> {
				if (flow.getOrientation() != Orientation.VERTICAL) return;
				flow.getOrientationHelper().invalidatePos();
				bindings.biInvalidate(vsp.vValProperty());
			})).get();

		MappingSource<Number, Number> hSource = new MappingSource<Number, Number>(flow.hPosProperty())
			.setTargetUpdater(Mapper.of(val -> {
				OrientationHelper helper = flow.getOrientationHelper();
				double max = helper.maxHScroll();
				return (max != 0) ? val.doubleValue() / max : 0.0;
			}), (o, n) -> vsp.setHVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				OrientationHelper helper = flow.getOrientationHelper();
				return val.doubleValue() * helper.maxHScroll();
			}), (o, n) -> flow.setHPos(n.doubleValue()));
		bindings.bindBidirectional(vsp.hValProperty())
			.addSource(hSource)
			.addInvalidatingSource(ExternalSource.of(flow.estimatedLengthProperty(), (o, n) -> {
				if (flow.getOrientation() != Orientation.HORIZONTAL) return;
				flow.getOrientationHelper().invalidatePos();
				bindings.biInvalidate(vsp.hValProperty());
			})).get();
		return vsp;
	}

	/**
	 * Does the hard job for you by creating a new {@code VirtualScrollPane} wrapping the
	 * given {@link PaginatedVirtualFlow}, initializing the needed bindings for the content bounds, the scrolling and the
	 * orientation.
	 */
	public static <T, C extends Cell<T>> VirtualScrollPane wrap(PaginatedVirtualFlow<T, C> flow) {
		VirtualScrollPane vsp = new VirtualScrollPane(flow) {
			@Override
			protected double computeMinHeight(double width) {
				Orientation o = flow.getOrientation();
				if (o == Orientation.VERTICAL) return computePrefHeight(width);
				return super.computeMinHeight(width);
			}

			@Override
			protected double computeMaxHeight(double width) {
				Orientation o = flow.getOrientation();
				if (o == Orientation.VERTICAL) return computePrefHeight(width);
				return super.computeMaxHeight(width);
			}

			@Override
			protected double computeMinWidth(double height) {
				Orientation o = flow.getOrientation();
				if (o == Orientation.HORIZONTAL) return computePrefWidth(height);
				return super.computeMinWidth(height);
			}

			@Override
			protected double computeMaxWidth(double height) {
				Orientation o = flow.getOrientation();
				if (o == Orientation.HORIZONTAL) return computePrefWidth(height);
				return super.computeMaxWidth(height);
			}
		};

		vsp.orientationProperty().bind(ObjectBindingBuilder.<Orientation>build()
			.setMapper(() -> {
				createBindingsFor(vsp, flow);
				return EnumUtils.next(Orientation.class, flow.getOrientation());
			})
			.addSources(flow.orientationProperty())
			.get()
		);
		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				Orientation o = flow.getOrientation();
				double breadth = flow.getMaxBreadth();
				return (o == Orientation.VERTICAL) ?
					VirtualBounds.of(flow.getWidth(), flow.getHeight(), breadth, 0) :
					VirtualBounds.of(flow.getWidth(), flow.getHeight(), 0, breadth);
			})
			.addSources(flow.orientationProperty())
			.addSources(flow.maxBreadthProperty())
			.addSources(flow.estimatedLengthProperty())
			.get()
		);
		return vsp;
	}

	//================================================================================
	// VirtualGrid
	//================================================================================

	/**
	 * Does the hard job for you by creating a new {@code VirtualScrollPane} wrapping the
	 * given {@link VirtualGrid}, initializing the needed bindings for the content bounds, the scrolling and the
	 * orientation.
	 * <p></p>
	 * <b>NOTE:</b> once this is not needed anymore you should call {@link #disposeFor(VirtualScrollPane)}
	 * to avoid memory leaks which may occur because of {@link MFXBindings}
	 */
	public static <T, C extends GridCell<T>> VirtualScrollPane wrap(VirtualGrid<T, C> grid) {
		VirtualScrollPane vsp = new VirtualScrollPane(grid);
		MFXBindings bindings = MFXBindings.instance();

		vsp.setOrientation(Orientation.VERTICAL);
		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				Size eSize = grid.getEstimatedSize();
				return VirtualBounds.of(grid.getWidth(), grid.getHeight(), eSize.getWidth(), eSize.getHeight());
			})
			.addSources(grid.widthProperty(), grid.heightProperty(), grid.estimatedSizeProperty())
			.get()
		);

		MappingSource<Position, Number> vSource = MappingSource.<Position, Number>of(grid.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				double max = helper.maxVScroll();
				return (max != 0) ? val.getY() / max : 0;
			}), (o, n) -> vsp.setVVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				Position currPos = grid.getPosition();
				return Position.of(currPos.getX(), val.doubleValue() * helper.maxVScroll());
			}), (o, n) -> grid.setPosition(n));
		bindings.bindBidirectional(vsp.vValProperty())
			.addSources(vSource)
			.addTargetInvalidatingSource(grid.heightProperty())
			.get();

		MappingSource<Position, Number> hSource = MappingSource.<Position, Number>of(grid.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				double max = helper.maxHScroll();
				return (max != 0) ? val.getX() / max : 0;
			}), (o, n) -> vsp.setHVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				Position currPos = grid.getPosition();
				return Position.of(val.doubleValue() * helper.maxHScroll(), currPos.getY());
			}), (o, n) -> grid.setPosition(n));
		bindings.bindBidirectional(vsp.hValProperty())
			.addSources(hSource)
			.addTargetInvalidatingSource(grid.widthProperty())
			.get();
		return vsp;
	}

	/**
	 * Does the hard job for you by creating a new {@code VirtualScrollPane} wrapping the
	 * given {@link PaginatedVirtualFlow}, initializing the needed bindings for the content bounds, the scrolling and the
	 * orientation.
	 * <p></p>
	 * <b>NOTE:</b> once this is not needed anymore you should call {@link #disposeFor(VirtualScrollPane)}
	 * to avoid memory leaks which may occur because of {@link MFXBindings} and {@link When}.
	 */
	public static <T, C extends GridCell<T>> VirtualScrollPane wrap(PaginatedVirtualGrid<T, C> grid) {
		MFXBindings bindings = MFXBindings.instance();
		VirtualScrollPane vsp = new VirtualScrollPane(grid) {
			@Override
			protected double computeMinHeight(double width) {
				return computePrefHeight(width);
			}

			@Override
			protected double computeMaxHeight(double width) {
				return computePrefHeight(width);
			}

			@Override
			protected double computeMaxWidth(double height) {
				return grid.maxWidth(height);
			}
		};
		vsp.setOrientation(Orientation.HORIZONTAL);

		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				double breadth = grid.getEstimatedSize().getWidth();
				return VirtualBounds.of(grid.getWidth(), grid.getHeight(), breadth, 0);
			})
			.addSources(grid.widthProperty(), grid.heightProperty(), grid.estimatedSizeProperty())
			.get()
		);

		MappingSource<Position, Number> hSource = MappingSource.<Position, Number>of(grid.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				double max = helper.maxHScroll();
				return (max != 0) ? val.getX() / max : 0;
			}), (o, n) -> vsp.setHVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				GridHelper helper = grid.getGridHelper();
				Position currPos = grid.getPosition();
				return Position.of(val.doubleValue() * helper.maxHScroll(), currPos.getY());
			}), (o, n) -> grid.setPosition(n));
		bindings.bindBidirectional(vsp.hValProperty())
			.addSources(hSource)
			.addTargetInvalidatingSource(grid.widthProperty())
			.get();
		return vsp;
	}

	//================================================================================
	// VirtualTable
	//================================================================================
	public static <T> VirtualScrollPane wrap(VirtualTable<T> table) {
		VirtualScrollPane vsp = new VirtualScrollPane(table);
		MFXBindings bindings = MFXBindings.instance();

		vsp.setOrientation(Orientation.VERTICAL);
		vsp.vBarOffsetProperty().bind(table.columnSizeProperty().map(Size::getHeight));
		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				Size eSize = table.getEstimatedSize();
				double vH = Math.max(0, eSize.getHeight() + table.getColumnSize().getHeight());
				return VirtualBounds.of(table.getWidth(), table.getHeight(), eSize.getWidth(), vH);
			})
			.addSources(table.widthProperty(), table.heightProperty(), table.columnSizeProperty(), table.estimatedSizeProperty())
			.get()
		);

		MappingSource<Position, Number> vSource = MappingSource.<Position, Number>of(table.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				double max = helper.maxVScroll();
				return (max != 0) ? val.getY() / max : 0;
			}), (o, n) -> vsp.setVVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				Position currPos = table.getPosition();
				return Position.of(currPos.getX(), val.doubleValue() * helper.maxVScroll());
			}), (o, n) -> table.setPosition(n));
		bindings.bindBidirectional(vsp.vValProperty())
			.addSources(vSource)
			.addTargetInvalidatingSource(table.heightProperty())
			.addInvalidatingSource(ExternalSource.of(table.estimatedSizeProperty(), (o, n) -> {
				if (o.getHeight() != n.getHeight()) {
					table.getTableHelper().invalidatedPos();
					bindings.biInvalidate(vsp.vValProperty());
				}
			}))
			.get();

		MappingSource<Position, Number> hSource = MappingSource.<Position, Number>of(table.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				double max = helper.maxHScroll();
				return (max != 0) ? val.getX() / max : 0;
			}), (o, n) -> vsp.setHVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				Position currPos = table.getPosition();
				return Position.of(val.doubleValue() * helper.maxHScroll(), currPos.getY());
			}), (o, n) -> table.setPosition(n));
		bindings.bindBidirectional(vsp.hValProperty())
			.addSources(hSource)
			.addTargetInvalidatingSource(table.widthProperty())
			.addInvalidatingSource(ExternalSource.of(table.estimatedSizeProperty(), (o, n) -> {
				if (o.getWidth() != n.getWidth()) {
					table.getTableHelper().invalidatedPos();
					bindings.biInvalidate(vsp.hValProperty());
				}
			}))
			.get();
		return vsp;
	}

	public static <T> VirtualScrollPane wrap(PaginatedVirtualTable<T> table) {
		MFXBindings bindings = MFXBindings.instance();
		VirtualScrollPane vsp = new VirtualScrollPane(table) {
			@Override
			protected double computeMinHeight(double width) {
				return computePrefHeight(width);
			}

			@Override
			protected double computePrefHeight(double width) {
				return super.computePrefHeight(width);
			}

			@Override
			protected double computeMaxHeight(double width) {
				return computePrefHeight(width);
			}

			@Override
			protected double computeMaxWidth(double height) {
				return table.maxWidth(height);
			}
		};
		vsp.setOrientation(Orientation.VERTICAL);

		vsp.contentBoundsProperty().bind(ObjectBindingBuilder.<VirtualBounds>build()
			.setMapper(() -> {
				Size eSize = table.getEstimatedSize();
				return VirtualBounds.of(table.getWidth(), table.getHeight(), eSize.getWidth(), 0);
			})
			.addSources(table.widthProperty(), table.heightProperty(), table.estimatedSizeProperty())
			.get()
		);

		MappingSource<Position, Number> hSource = MappingSource.<Position, Number>of(table.positionProperty())
			.setTargetUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				double max = helper.maxHScroll();
				return (max != 0) ? val.getX() / max : 0;
			}), (o, n) -> vsp.setHVal(n.doubleValue()))
			.setSourceUpdater(Mapper.of(val -> {
				TableHelper helper = table.getTableHelper();
				Position currPos = table.getPosition();
				return Position.of(val.doubleValue() * helper.maxHScroll(), currPos.getY());
			}), (o, n) -> table.setPosition(n));
		bindings.bindBidirectional(vsp.hValProperty())
			.addSources(hSource)
			.addTargetInvalidatingSource(table.widthProperty())
			.addInvalidatingSource(ExternalSource.of(table.estimatedSizeProperty(), (o, n) -> {
				if (o.getWidth() != n.getWidth()) {
					table.getTableHelper().invalidatedPos();
					bindings.biInvalidate(vsp.hValProperty());
				}
			}))
			.get();
		return vsp;
	}

	//================================================================================
	// Misc
	//================================================================================

	/**
	 * Sets the horizontal scroll speed for the given {@link VirtualScrollPane}.
	 *
	 * @param unitIncrement  the amount of pixels to scroll when using buttons or mouse scroll
	 * @param smoothUnit     the amount of pixels to scroll when smooth scrolling with the mouse
	 * @param trackIncrement the amount of pixels to scroll when using the track
	 */
	public static void setHSpeed(VirtualScrollPane vsp, double unitIncrement, double smoothUnit, double trackIncrement) {
		vsp.hUnitIncrementProperty().bind(DoubleBindingBuilder.build()
			.setMapper(() -> {
				Orientation o = vsp.getOrientation();
				VirtualBounds cBounds = vsp.getContentBounds();
				double viewL = cBounds.getWidth();
				double contentL;
				if (vsp.getContent() != null && vsp.getContent() instanceof VirtualFlow) {
					contentL = (o == Orientation.VERTICAL) ? cBounds.getVirtualHeight() : cBounds.getVirtualWidth();
				} else {
					contentL = cBounds.getVirtualWidth();
				}
				double pixels = vsp.isSmoothScroll() ? smoothUnit : unitIncrement;
				return Math.max(0, pixels / (contentL - viewL));
			})
			.addSources(vsp.orientationProperty(), vsp.heightProperty(), vsp.widthProperty())
			.addSources(vsp.contentBoundsProperty(), vsp.smoothScrollProperty())
			.get()
		);
		vsp.hTrackIncrementProperty().bind(DoubleBindingBuilder.build()
			.setMapper(() -> {
				Orientation o = vsp.getOrientation();
				VirtualBounds cBounds = vsp.getContentBounds();
				double viewL = cBounds.getWidth();
				double contentL = (o == Orientation.VERTICAL) ? cBounds.getVirtualHeight() : cBounds.getVirtualWidth();
				return Math.max(0, trackIncrement / (contentL - viewL));
			})
			.addSources(vsp.orientationProperty(), vsp.heightProperty(), vsp.widthProperty(), vsp.contentBoundsProperty())
			.get()
		);
	}

	/**
	 * Sets the vertical scroll speed for the given {@link VirtualScrollPane}.
	 *
	 * @param unitIncrement  the amount of pixels to scroll when using buttons or mouse scroll
	 * @param smoothUnit     the amount of pixels to scroll when smooth scrolling with the mouse
	 * @param trackIncrement the amount of pixels to scroll when using the track
	 */
	public static void setVSpeed(VirtualScrollPane vsp, double unitIncrement, double smoothUnit, double trackIncrement) {
		vsp.vUnitIncrementProperty().bind(DoubleBindingBuilder.build()
			.setMapper(() -> {
				Orientation o = vsp.getOrientation();
				VirtualBounds cBounds = vsp.getContentBounds();
				double viewL = cBounds.getHeight();
				double contentL;
				if (vsp.getContent() != null && vsp.getContent() instanceof VirtualFlow) {
					contentL = (o == Orientation.VERTICAL) ? cBounds.getVirtualHeight() : cBounds.getVirtualWidth();
				} else {
					contentL = cBounds.getVirtualHeight();
				}
				double pixels = vsp.isSmoothScroll() ? smoothUnit : unitIncrement;
				return Math.max(0, pixels / (contentL - viewL));
			})
			.addSources(vsp.orientationProperty(), vsp.heightProperty(), vsp.widthProperty())
			.addSources(vsp.contentBoundsProperty(), vsp.smoothScrollProperty())
			.get()
		);
		vsp.vTrackIncrementProperty().bind(DoubleBindingBuilder.build()
			.setMapper(() -> {
				Orientation o = vsp.getOrientation();
				VirtualBounds cBounds = vsp.getContentBounds();
				double viewL = cBounds.getHeight();
				double contentL = (o == Orientation.VERTICAL) ? cBounds.getVirtualHeight() : cBounds.getVirtualWidth();
				return Math.max(0, trackIncrement / (contentL - viewL));
			})
			.addSources(vsp.orientationProperty(), vsp.heightProperty(), vsp.widthProperty(), vsp.contentBoundsProperty())
			.get()
		);
	}

	/**
	 * This is automatically called by a listener added in {@link #wrap(PaginatedVirtualFlow)} when the flow's orientation
	 * changes. This type of flow is special because we want to scroll only in the opposite direction of the orientation
	 * and of course only if there are cells that are larger than the viewport. When the flow's orientation changes,
	 * the bindings for the {@code VirtualScrollPane} must be re-built taking into consideration the new orientation.
	 * <p>
	 * When the orientation is HORIZONTAL a binding for the vertical position/scroll is built, otherwise when the
	 * orientation is VERTICAL a binding for the horizontal position/scroll is built.
	 * <p>
	 * Exactly as for {@link #wrap(VirtualFlow)}, bindings are bidirectional and constructed by the {@link MFXBindings}
	 * utility, since these bindings need some extra processing/mapping before setting the values. Which means that
	 * for disposal you need to use {@link MFXBindings} methods or the ones provided here by the scroll pane.
	 */
	private static <T, C extends Cell<T>> void createBindingsFor(VirtualScrollPane vsp, PaginatedVirtualFlow<T, C> flow) {
		MFXBindings bindings = MFXBindings.instance();
		bindings.disposeBidirectional(vsp.vValProperty());
		bindings.disposeBidirectional(vsp.hValProperty());

		Orientation o = flow.getOrientation();
		MappingSource<Number, Number> source;
		Property<Number> prop;
		if (o == Orientation.HORIZONTAL) {
			prop = vsp.vValProperty();
			source = MappingSource.<Number, Number>of(flow.vPosProperty())
				.setTargetUpdater(Mapper.of(val -> {
					OrientationHelper helper = flow.getOrientationHelper();
					double max = helper.maxVScroll();
					return (max != 0) ? val.doubleValue() / max : 0;
				}), (ov, n) -> vsp.setVVal(n.doubleValue()))
				.setSourceUpdater(Mapper.of(val -> {
					OrientationHelper helper = flow.getOrientationHelper();
					return val.doubleValue() * helper.maxVScroll();
				}), (ov, n) -> flow.setVPos(n.doubleValue()));
		} else {
			prop = vsp.hValProperty();
			source = MappingSource.<Number, Number>of(flow.hPosProperty())
				.setTargetUpdater(Mapper.of(val -> {
					OrientationHelper helper = flow.getOrientationHelper();
					double max = helper.maxHScroll();
					return (max != 0) ? val.doubleValue() / max : 0;
				}), (ov, n) -> vsp.setHVal(n.doubleValue()))
				.setSourceUpdater(Mapper.of(val -> {
					OrientationHelper helper = flow.getOrientationHelper();
					return val.doubleValue() * helper.maxHScroll();
				}), (ov, n) -> flow.setHPos(n.doubleValue()));
		}
		bindings.bindBidirectional(prop).addSources(source).get();
	}

	/**
	 * Disposes the bindings created by the various {@code wrap()} methods
	 */
	public static void disposeFor(VirtualScrollPane vsp) {
		MFXBindings bindings = MFXBindings.instance();
		bindings.disposeBidirectional(vsp.vValProperty());
		bindings.disposeBidirectional(vsp.hValProperty());
	}
}
