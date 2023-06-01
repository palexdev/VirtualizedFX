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

package io.github.palexdev.virtualizedfx.controls.skins;

import io.github.palexdev.mfxcore.base.beans.Position;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxcore.utils.fx.NodeUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.mfxeffects.animations.Animations.KeyFrames;
import io.github.palexdev.mfxeffects.animations.Animations.TimelineBuilder;
import io.github.palexdev.virtualizedfx.controls.MFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.VirtualScrollPane;
import io.github.palexdev.virtualizedfx.controls.behavior.MFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import javafx.animation.Animation;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * Default skin implementation for {@link VirtualScrollPane}.
 * <p></p>
 * There are three components: the content container and the two scroll bars.
 * <p></p>
 * All the components are positioned and sized according to a custom layout strategy
 * which varies based on several properties, five are the main ones: {@link VirtualScrollPane#layoutModeProperty()},
 * {@link VirtualScrollPane#vBarPosProperty()}, {@link VirtualScrollPane#hBarPosProperty()},
 * {@link VirtualScrollPane#vBarPolicyProperty()} and {@link VirtualScrollPane#hBarPolicyProperty()}.
 * <p>
 * <p> - The LayoutMode specifies if the scroll pane should reserve extra space for the
 * scroll bars ({@link LayoutMode#DEFAULT}), or they can be positioned above the content ({@link LayoutMode#COMPACT}).
 * This last option is especially useful in combination with {@link VirtualScrollPane#autoHideBarsProperty()}.
 * <p> - The vBarPos allows to specify the position of the vertical scroll bar LEFT/RIGHT(default)
 * <p> - The hBarPos allows to specify the position of the horizontal scroll bar TOP/BOTTOM(default)
 * <p> - The vBarPolicy allows to specify the visibility policy for the vertical scroll bar
 * <p> - The hBarPolicy allows to specify the visibility policy for the horizontal scroll bar
 */
public class VirtualScrollPaneSkin extends SkinBase<VirtualScrollPane> {
	//================================================================================
	// Properties
	//================================================================================
	private final Pane container;
	private final Rectangle clip;
	private final MFXScrollBar vBar;
	private final MFXScrollBar hBar;

	private final double DEFAULT_SIZE = 100.0;
	private Node content;

	private Position initValues = Position.of(0, 0);
	private Position dragStart = Position.of(-1, -1);

	private static final PseudoClass COMPACT_MODE_PSEUDO_CLASS = PseudoClass.getPseudoClass("compact");
	private static final PseudoClass DRAG_TO_SCROLL_PSEUDO_CLASS = PseudoClass.getPseudoClass("drag-to-scroll");

	private final double duration = 250;
	private Animation hShow;
	private Animation hHide;
	private Animation vShow;
	private Animation vHide;

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualScrollPaneSkin(VirtualScrollPane scrollPane) {
		super(scrollPane);
		this.content = scrollPane.getContent();

		// Init Container
		container = new Pane() {
			@Override
			protected void layoutChildren() {
				if (content != null) content.resizeRelocate(0, 0, getWidth(), getHeight());
			}
		};
		container.getStyleClass().add("container");
		if (content != null) container.getChildren().add(content);

		clip = new Rectangle();
		clip.widthProperty().bind(container.widthProperty());
		clip.heightProperty().bind(container.heightProperty());
		clip.arcWidthProperty().bind(scrollPane.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(scrollPane.clipBorderRadiusProperty());
		container.setClip(clip);

		// Init Scroll Bars
		hBar = new MFXScrollBar(Orientation.HORIZONTAL);
        hBar.behaviorProperty().bind(ObjectBindingBuilder.<MFXScrollBarBehavior>build()
            .setMapper(() -> scrollPane.getHBarBehavior().apply(hBar))
            .addSources(scrollPane.hBarBehaviorProperty())
            .get()
        );

        vBar = new MFXScrollBar(Orientation.VERTICAL);
        vBar.behaviorProperty().bind(ObjectBindingBuilder.<MFXScrollBarBehavior>build()
            .setMapper(() -> scrollPane.getVBarBehavior().apply(vBar))
            .addSources(scrollPane.vBarBehaviorProperty())
            .get()
        );

		if (scrollPane.isAutoHideBars()) {
			hBar.setOpacity(0.0);
			vBar.setOpacity(0.0);
		}

		// Init PseudoClasses
		pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, scrollPane.getLayoutMode() == LayoutMode.COMPACT);
		pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, scrollPane.isDragToScroll());

		// Animations
		hShow = TimelineBuilder.build()
				.add(KeyFrames.of(duration, hBar.opacityProperty(), 1.0))
				.setOnFinished(event -> {
					if (hBar.isHover() || hBar.isPressed()) return;
					hHide.playFromStart();
				})
				.getAnimation();
		hHide = TimelineBuilder.build()
				.setDelay(600)
				.add(KeyFrames.of(duration, hBar.opacityProperty(), 0.0))
				.getAnimation();

		vShow = TimelineBuilder.build()
				.add(KeyFrames.of(duration, vBar.opacityProperty(), 1.0))
				.setOnFinished(event -> {
					if (vBar.isHover() || vBar.isPressed()) return;
					vHide.playFromStart();
				})
				.getAnimation();
		vHide = TimelineBuilder.build()
				.setDelay(600)
				.add(KeyFrames.of(duration, vBar.opacityProperty(), 0.0))
				.getAnimation();

		// Finalize Initialization
		bindings();
		addListeners();
		getChildren().setAll(container, vBar, hBar);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds the following bindings for the scroll bars:
	 * <p> - Two bindings for the visible amount, which is computes by using
	 * the provided {@link VirtualScrollPane#contentBoundsProperty()}
	 * <p> - Two bindings for the visibility, which in addition to the visible amount must
	 * also take into account the scroll bars policy
	 * <p> The necessary bindings for the min/val/max properties, as well
	 * as for the buttonsGap, trackIncrement, unitIncrement and smooth scroll properties
	 */
	private void bindings() {
		VirtualScrollPane sp = getSkinnable();

		// HBar
		hBarVisibleAmountProperty().bind(DoubleBindingBuilder.build()
				.setMapper(() -> sp.getWidth() / sp.getContentBounds().getVirtualWidth())
				.addSources(sp.widthProperty(), sp.contentBoundsProperty())
				.get()
		);
		hBar.visibleProperty().bind(BooleanBindingBuilder.build()
				.setMapper(() -> hBar.getVisibleAmount() < 1.0 && sp.getHBarPolicy() != ScrollBarPolicy.NEVER)
				.addSources(hBar.visibleAmountProperty(), sp.hBarPolicyProperty())
				.get()
		);

		sp.hMinProperty().bindBidirectional(hBar.minProperty());
		sp.hValProperty().bindBidirectional(hBar.valueProperty());
		sp.hMaxProperty().bindBidirectional(hBar.maxProperty());
		hBar.buttonsGapProperty().bind(sp.buttonsGapProperty());
		hBar.trackIncrementProperty().bind(sp.hTrackIncrementProperty());
		hBar.unitIncrementProperty().bind(sp.hUnitIncrementProperty());
		hBar.smoothScrollProperty().bind(sp.smoothScrollProperty());
		hBar.trackSmoothScrollProperty().bind(sp.trackSmoothScrollProperty());

		// VBar
		vBarVisibleAmountProperty().bind(DoubleBindingBuilder.build()
				.setMapper(() -> sp.getHeight() / sp.getContentBounds().getVirtualHeight())
				.addSources(sp.heightProperty(), sp.contentBoundsProperty())
				.get()
		);
		vBar.visibleProperty().bind(BooleanBindingBuilder.build()
				.setMapper(() -> vBar.getVisibleAmount() < 1.0 && sp.getVBarPolicy() != ScrollBarPolicy.NEVER)
				.addSources(vBar.visibleAmountProperty(), sp.vBarPolicyProperty())
				.get()
		);

		sp.vMinProperty().bindBidirectional(vBar.minProperty());
		sp.vValProperty().bindBidirectional(vBar.valueProperty());
		sp.vMaxProperty().bindBidirectional(vBar.maxProperty());
		vBar.buttonsVisibleProperty().bind(sp.buttonsVisibleProperty());
		vBar.buttonsGapProperty().bind(sp.buttonsGapProperty());
		vBar.trackIncrementProperty().bind(sp.vTrackIncrementProperty());
		vBar.unitIncrementProperty().bind(sp.vUnitIncrementProperty());
		vBar.smoothScrollProperty().bind(sp.smoothScrollProperty());
		vBar.trackSmoothScrollProperty().bind(sp.trackSmoothScrollProperty());
	}

	/**
	 * Adds the following listeners:
	 * <p> - A listener to the {@link VirtualScrollPane#contentProperty()} to update
	 * the content container
	 * <p> - A listener to the {@link VirtualScrollPane#contentBoundsProperty()} to show the horizontal or vertical
	 * scroll bars when the virtual width or height change, if the {@link VirtualScrollPane#autoHideBarsProperty()} feature is on
	 * <p> - A listener to the {@link VirtualScrollPane#layoutModeProperty()} to change the state
	 * of the ":compact" PseudoClass and update the layout
	 * <p> - A listener to the bars policy properties, tha bars position properties and the bars
	 * padding properties to update the layout
	 * <p> - A listener to the {@link VirtualScrollPane#dragToScrollProperty()} to change the state
	 * of the ":drag-to-scroll" PseudoClass
	 * <p> A set of listeners to the {@link VirtualScrollPane#autoHideBarsProperty()} and
	 * bars' value and hover properties to make the autoHideBarsProperty work as intended
	 * <p></p>
	 * Additionally, the following handlers are added:
	 * <p> - Two event handlers for {@link MouseEvent#MOUSE_PRESSED} and {@link MouseEvent#MOUSE_DRAGGED}
	 * for the {@link VirtualScrollPane#dragToScrollProperty()} feature to work as intended
	 * <p> - An event handler which is responsible for capturing {@link ScrollEvent#SCROLL} events and
	 * re-route them on the scroll bars according to the {@link VirtualScrollPane#orientationProperty()} and
	 * if the target scroll bar is visible
	 */
	private void addListeners() {
		VirtualScrollPane sp = getSkinnable();
		InvalidationListener reLayout = invalidated -> sp.requestLayout();

		// Listeners
		sp.contentProperty().addListener((observable, oldValue, newValue) -> {
			if (content != null) {
				content = null;
				container.getChildren().clear();
			}
			if (newValue != null) {
				content = newValue;
				container.getChildren().setAll(content);
			}
		});

		sp.contentBoundsProperty().addListener((observable, oldValue, newValue) -> {
			if (!sp.isAutoHideBars()) return;
			if (oldValue.getVirtualWidth() != newValue.getVirtualHeight()) {
				hShow.playFromStart();
			}
			if (oldValue.getVirtualHeight() != newValue.getVirtualHeight()) {
				vShow.playFromStart();
			}
		});

		sp.layoutModeProperty().addListener((observable, oldValue, newValue) -> {
			pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, newValue == LayoutMode.COMPACT);
			sp.requestLayout();
		});

		sp.hBarPolicyProperty().addListener(reLayout);
		sp.vBarPolicyProperty().addListener(reLayout);
		sp.vBarPosProperty().addListener(reLayout);
		sp.hBarPosProperty().addListener(reLayout);
		sp.vBarPaddingProperty().addListener(reLayout);
		sp.hBarPaddingProperty().addListener(reLayout);

		sp.dragToScrollProperty().addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, newValue));

		// Animations
		sp.autoHideBarsProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				if (!hBar.isHover() && !hBar.isPressed()) hHide.playFromStart();
				if (!vBar.isHover() && !vBar.isPressed()) vHide.playFromStart();
			} else {
				hBar.setOpacity(1.0);
				vBar.setOpacity(1.0);
			}
		});

		hBar.valueProperty().addListener(invalidated -> {
			if (!sp.isAutoHideBars()) return;
			hHide.stop();
			hShow.play();
		});
		hBar.hoverProperty().addListener((observable, oldValue, newValue) -> {
			if (!sp.isAutoHideBars()) return;
			if (!newValue && hBar.isPressed()) {
				When.onChanged(hBar.pressedProperty())
						.then((ov, nv) -> {
							if (!nv) hHide.playFromStart();
						})
						.oneShot()
						.listen();
				return;
			}

			if (newValue) {
				hHide.stop();
				hShow.playFromStart();
			} else if (!Animations.isPlaying(hShow))
				hHide.playFromStart();
		});

		vBar.valueProperty().addListener(invalidated -> {
			if (!sp.isAutoHideBars()) return;
			vHide.stop();
			vShow.play();
		});
		vBar.hoverProperty().addListener((observable, oldValue, newValue) -> {
			if (!sp.isAutoHideBars()) return;
			if (!newValue && vBar.isPressed()) {
				When.onChanged(vBar.pressedProperty())
						.then((ov, nv) -> {
							if (!nv) vHide.playFromStart();
						})
						.oneShot()
						.listen();
				return;
			}

			if (newValue) {
				vHide.stop();
				vShow.playFromStart();
			} else if (!Animations.isPlaying(vShow))
				vHide.playFromStart();
		});

		// Handlers
		container.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
			if (vBar.isDragging() || hBar.isDragging()) return;
			if (!sp.isDragToScroll()) {
				dragStart = Position.of(-1, -1);
				initValues = Position.of(0, 0);
				return;
			}

			dragStart = Position.of(event.getX(), event.getY());
			initValues = Position.of(hBar.getValue(), vBar.getValue());
		});

		container.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
			if (dragStart.getX() == -1 || dragStart.getY() == -1) return;

			if (hBar.isVisible()) {
				double xDelta = -(event.getX() - dragStart.getX());
				double xvDelta = posToVal(hBar.getTrackLength(), hBar.getVisibleAmount(), xDelta, hBar.getMin(), hBar.getMax());
				hBar.setValue(initValues.getX() + xvDelta);
			}

			if (vBar.isVisible()) {
				double yDelta = -(event.getY() - dragStart.getY());
				double yvDelta = posToVal(vBar.getTrackLength(), vBar.getVisibleAmount(), yDelta, vBar.getMin(), vBar.getMax());
				vBar.setValue(initValues.getY() + yvDelta);
			}
		});

		sp.addEventHandler(ScrollEvent.SCROLL, event -> {
			if (NodeUtils.inHierarchy(event.getPickResult().getIntersectedNode(), vBar) ||
					NodeUtils.inHierarchy(event.getPickResult().getIntersectedNode(), hBar)) {
				event.consume();
				return;
			}

			Node target;
			Orientation orientation = sp.getOrientation();
			switch (orientation) {
				case VERTICAL: {
					target = event.isShiftDown() ? hBar : vBar;
					break;
				}
				case HORIZONTAL: {
					target = event.isShiftDown() ? vBar : hBar;
					break;
				}
				default:
					target = vBar;
			}
			if (target.isVisible()) reRouteScrollEvent(target, event);
		});
	}

	/**
	 * Core method responsible for converting the given position to a value which is in the
	 * scroll bar's values range (between min and max).
	 *
	 * @param trackLength   the scroll bar's track length
	 * @param visibleAmount the scroll bar's visible amount
	 * @param pos           the position
	 * @param vMin          the scroll bar's minimum value
	 * @param vMax          the scroll bar's maximum value
	 */
	private double posToVal(double trackLength, double visibleAmount, double pos, double vMin, double vMax) {
		double maxPos = trackLength - (trackLength * visibleAmount);
		return NumberUtils.mapOneRangeToAnother(
				pos,
				DoubleRange.of(0.0, maxPos),
				DoubleRange.of(vMin, vMax)
		);
	}

	/**
	 * Convenience method to re-route the given {@link ScrollEvent} on
	 * the given target.
	 */
	private void reRouteScrollEvent(Node target, ScrollEvent se) {
		Event.fireEvent(target, new ScrollEvent(
				null, target, ScrollEvent.SCROLL,
				0, 0, 0, 0,
				false, false, false, false, false, false,
				se.getDeltaX(), se.getDeltaY(),
				0, 0,
				ScrollEvent.HorizontalTextScrollUnits.NONE,
				0,
				ScrollEvent.VerticalTextScrollUnits.NONE,
				0, 0, null
		));
	}

	private DoubleProperty hBarVisibleAmountProperty() {
		return ((DoubleProperty) hBar.visibleAmountProperty());
	}

	private DoubleProperty vBarVisibleAmountProperty() {
		return ((DoubleProperty) vBar.visibleAmountProperty());
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		return leftInset + DEFAULT_SIZE + LayoutUtils.boundWidth(vBar) + rightInset;
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		return topInset + DEFAULT_SIZE + LayoutUtils.boundHeight(hBar) + bottomInset;
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		VirtualScrollPane sp = getSkinnable();
		Node content = sp.getContent();
		LayoutMode layoutMode = sp.getLayoutMode();
		ScrollBarPolicy vBarPolicy = sp.getVBarPolicy();

		double contentW = (content != null) ? content.prefWidth(-1) : 0.0;
		double prefW = leftInset + contentW + rightInset;
		if (layoutMode == LayoutMode.DEFAULT && vBarPolicy != ScrollBarPolicy.NEVER)
			prefW += LayoutUtils.boundWidth(vBar);
		return prefW;
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		VirtualScrollPane sp = getSkinnable();
		Node content = sp.getContent();
		LayoutMode layoutMode = sp.getLayoutMode();
		ScrollBarPolicy hBarPolicy = sp.getHBarPolicy();

		double contentH = (content != null) ? content.prefHeight(-1) : 0.0;
		double prefH = topInset + contentH + bottomInset;
		if (layoutMode == LayoutMode.DEFAULT && hBarPolicy != ScrollBarPolicy.NEVER)
			prefH += LayoutUtils.boundHeight(hBar);
		return prefH;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VirtualScrollPane sp = getSkinnable();
		LayoutMode layoutMode = sp.getLayoutMode();
		HPos vBarPos = sp.getVBarPos().toHPos();
		VPos hBarPos = sp.getHBarPos().toVPos();
		ScrollBarPolicy vBarPolicy = sp.getVBarPolicy();
		ScrollBarPolicy hBarPolicy = sp.getHBarPolicy();

		double vBarOffset = sp.getVBarOffset();
		Insets vBarPadding = sp.getVBarPadding();
		double hPadding = snappedLeftInset() + snappedRightInset();

		double hBarOffset = sp.getHBarOffset();
		Insets hBarPadding = sp.getHBarPadding();
		double vPadding = snappedTopInset() + snappedBottomInset();

		double totalWidth = w + hPadding;
		double totalHeight = h + vPadding;

		double vBarW = LayoutUtils.boundWidth(vBar);
		double vBarH = totalHeight - vBarPadding.getBottom() - vBarPadding.getTop() - vBarOffset;
		double vBarX = (vBarPos == HPos.LEFT) ? vBarPadding.getLeft() : totalWidth - vBarW - vBarPadding.getRight();
		double vBarY = vBarPadding.getTop() + vBarOffset;

		double hBarW = totalWidth - hBarPadding.getLeft() - hBarPadding.getRight() - hBarOffset;
		double hBarH = LayoutUtils.boundHeight(hBar);
		double hBarX = hBarPadding.getLeft() + hBarOffset;
		double hBarY = (hBarPos == VPos.TOP) ? hBarPadding.getTop() : totalHeight - hBarH - hBarPadding.getBottom();

		if (layoutMode != LayoutMode.COMPACT ||
				hBarPolicy != ScrollBarPolicy.NEVER &&
						!hBar.isVisible())
			vBarH -= hBarH;

		if (layoutMode != LayoutMode.COMPACT ||
				vBarPolicy != ScrollBarPolicy.NEVER &&
						!vBar.isVisible())
			hBarW -= vBarW;

		vBar.resizeRelocate(vBarX, vBarY, vBarW, vBarH);
		hBar.resizeRelocate(hBarX, hBarY, hBarW, hBarH);

		double viewW = (layoutMode == LayoutMode.DEFAULT && vBarPolicy != ScrollBarPolicy.NEVER) ? w - vBarW : w;
		double viewH = (layoutMode == LayoutMode.DEFAULT && hBarPolicy != ScrollBarPolicy.NEVER) ? h - hBarH : h;
		container.resizeRelocate(snappedLeftInset(), snappedTopInset(), viewW, viewH);
	}

	@Override
	public void dispose() {
		vShow.stop();
		hShow.stop();
		vHide.stop();
		hHide.stop();
		vShow = null;
		hShow = null;
		vHide = null;
		hHide = null;
		content = null;
		super.dispose();
	}
}
