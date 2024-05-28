/*
 * Copyright (C) 2024 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.palexdev.virtualizedfx.controls.skins;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollPaneBehavior;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import javafx.animation.Animation;
import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.function.Supplier;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollPane.COMPACT_MODE_PSEUDO_CLASS;
import static io.github.palexdev.virtualizedfx.controls.VFXScrollPane.DRAG_TO_SCROLL_PSEUDO_CLASS;

/**
 * Default skin implementation for {@link VFXScrollPane}.
 * <p></p>
 * There are three components: the content container also referred to as the "viewport", and the two scroll bars.
 * <p></p>
 * All the components are positioned and sized according to a custom layout strategy
 * which varies based on several properties, five are the main ones: {@link VFXScrollPane#layoutModeProperty()},
 * {@link VFXScrollPane#vBarPosProperty()}, {@link VFXScrollPane#hBarPosProperty()},
 * {@link VFXScrollPane#vBarPolicyProperty()} and {@link VFXScrollPane#hBarPolicyProperty()}.
 * <p>
 * <p> - The LayoutMode specifies if the scroll pane should reserve extra space for the
 * scroll bars ({@link LayoutMode#DEFAULT}), or they can be positioned above the content ({@link LayoutMode#COMPACT}).
 * This last option is especially useful in combination with {@link VFXScrollPane#autoHideBarsProperty()}.
 * <p> - The vBarPos allows to specify the position of the vertical scroll bar LEFT/RIGHT(default)
 * <p> - The hBarPos allows to specify the position of the horizontal scroll bar TOP/BOTTOM(default)
 * <p> - The vBarPolicy allows to specify the visibility policy for the vertical scroll bar
 * <p> - The hBarPolicy allows to specify the visibility policy for the horizontal scroll bar
 */
public class VFXScrollPaneSkin extends SkinBase<VFXScrollPane, VFXScrollPaneBehavior> {
	//================================================================================
	// Properties
	//================================================================================
	private final Pane viewport;
	private final Rectangle clip;
	private final VFXScrollBar hBar;
	private final VFXScrollBar vBar;
	private Node content;
	protected double DEFAULT_SIZE = 100.0;

	// Animations
	protected double SHOW_HIDE_DURATION = 250;
	private Animation hShow;
	private Animation hHide;
	private Animation vShow;
	private Animation vHide;

	//================================================================================
	// Constructors
	//================================================================================
	public VFXScrollPaneSkin(VFXScrollPane pane) {
		super(pane);
		this.content = pane.getContent();

		// Init viewport
		viewport = new Pane() {
			@Override
			protected void layoutChildren() {
				if (content != null) content.resizeRelocate(0, 0, getWidth(), getHeight());
			}
		};
		viewport.getStyleClass().add("viewport");
		if (content != null) viewport.getChildren().add(content);

		clip = new Rectangle();
		clip.widthProperty().bind(viewport.widthProperty());
		clip.heightProperty().bind(viewport.heightProperty());
		clip.arcWidthProperty().bind(pane.clipBorderRadiusProperty());
		clip.arcHeightProperty().bind(pane.clipBorderRadiusProperty());
		viewport.setClip(clip);

		// Init scroll bars
		// double lambda go brrrr, hahahaha
		hBar = new VFXScrollBar(Orientation.HORIZONTAL);
		hBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
			.setMapper(() -> () -> pane.getHBarBehavior().apply(hBar))
			.addSources(pane.hBarBehaviorProperty())
			.get()
		);

		vBar = new VFXScrollBar(Orientation.VERTICAL);
		vBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
			.setMapper(() -> () -> pane.getVBarBehavior().apply(vBar))
			.addSources(pane.vBarBehaviorProperty())
			.get()
		);

		if (pane.isAutoHideBars()) {
			hBar.setOpacity(0.0);
			vBar.setOpacity(0.0);
		}

		// Init PseudoClasses
		pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, pane.getLayoutMode() == LayoutMode.COMPACT);
		pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, pane.isDragToScroll());

		// Animations
		hShow = Animations.TimelineBuilder.build()
			.add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), 1.0))
			.setOnFinished(event -> {
				if (hBar.isHover() || hBar.isPressed()) return;
				hHide.playFromStart();
			})
			.getAnimation();
		hHide = Animations.TimelineBuilder.build()
			.setDelay(600)
			.add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), 0.0))
			.getAnimation();

		vShow = Animations.TimelineBuilder.build()
			.add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), 1.0))
			.setOnFinished(event -> {
				if (vBar.isHover() || vBar.isPressed()) return;
				vHide.playFromStart();
			})
			.getAnimation();
		vHide = Animations.TimelineBuilder.build()
			.setDelay(600)
			.add(Animations.KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), 0.0))
			.getAnimation();

		// Finalize init
		addListeners();
		getChildren().setAll(viewport, vBar, hBar);
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Creates the following bindings:
	 * <p> - binds the {@link VFXScrollPane#viewportSizeProperty()} to the viewport's width and height.
	 * <p> - binds all the horizontal scroll bar's properties to the ones of the scroll pane.
	 * The {@link VFXScrollPane#hValProperty()} is bound bidirectionally to the {@link VFXScrollBar#valueProperty()}
	 * <p> - binds all the vertical scroll bar's properties to the ones of the scroll pane.
	 * The {@link VFXScrollPane#vValProperty()} is bound bidirectionally to the {@link VFXScrollBar#valueProperty()}
	 * <p></p>
	 * <p>
	 * Adds the following listeners:
	 * <p> - A listener to update the layout to the following properties: {@link VFXScrollPane#hBarPolicyProperty()},
	 * {@link VFXScrollPane#vBarPolicyProperty()}, {@link VFXScrollPane#hBarPosProperty()}, {@link VFXScrollPane#vBarPosProperty()},
	 * {@link VFXScrollPane#hBarPaddingProperty()}, {@link VFXScrollPane#vBarPaddingProperty()}
	 * <p> - A listener on the {@link VFXScrollPane#contentProperty()} to update the viewport
	 * <p> - A listener on the {@link VFXScrollPane#contentBoundsProperty()} to update the scroll bars' visibility if the
	 * {@link VFXScrollPane#autoHideBarsProperty()} is active
	 * <p> - A listener on the {@link VFXScrollPane#layoutModeProperty()} to update the layout and enable/disable the
	 * ':compact' {@link PseudoClass}
	 * <p> - A listener on the {@link VFXScrollPane#dragToScrollProperty()} to enable/disable the ':drag-to-scroll' {@link PseudoClass}
	 * <p> - A listener on the {@link VFXScrollPane#hValProperty()} to show the horizontal scroll bar if the
	 * {@link VFXScrollPane#autoHideBarsProperty()} is active
	 * <p> - A listener on the horizontal scroll bar's {@link VFXScrollBar#hoverProperty()} to show/hide it when the
	 * {@link VFXScrollPane#autoHideBarsProperty()} is active
	 * <p> - A listener on the {@link VFXScrollPane#vValProperty()} to show the vertical scroll bar if the
	 * {@link VFXScrollPane#autoHideBarsProperty()} is active
	 * <p> - A listener on the vertical scroll bar's {@link VFXScrollBar#hoverProperty()} to show/hide it when the
	 * {@link VFXScrollPane#autoHideBarsProperty()} is active
	 */
	private void addListeners() {
		VFXScrollPane pane = getSkinnable();

		// Bindings
		((SizeProperty) pane.viewportSizeProperty()).bind(
			viewport.layoutBoundsProperty().map(b -> Size.of(b.getWidth(), b.getHeight()))
		);

		// HBar
		hBar.scrollBoundsProperty().bind(pane.contentBoundsProperty());
		hBar.visibleProperty().bind(BooleanBindingBuilder.build()
			.setMapper(() -> hBar.getVisibleAmount() < 1.0 && pane.getHBarPolicy() != ScrollBarPolicy.NEVER)
			.addSources(pane.contentBoundsProperty(), pane.hBarPolicyProperty())
			.get()
		);
		hBar.minProperty().bind(pane.hMinProperty());
		pane.hValProperty().bindBidirectional(hBar.valueProperty());
		hBar.maxProperty().bind(pane.hMaxProperty());
		hBar.showButtonsProperty().bind(pane.showButtonsProperty());
		hBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
		hBar.trackIncrementProperty().bind(pane.hTrackIncrementProperty());
		hBar.unitIncrementProperty().bind(pane.hUnitIncrementProperty());
		hBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
		hBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

		// VBar
		vBar.scrollBoundsProperty().bind(pane.contentBoundsProperty());
		vBar.visibleProperty().bind(BooleanBindingBuilder.build()
			.setMapper(() -> vBar.getVisibleAmount() < 1.0 && pane.getVBarPolicy() != ScrollBarPolicy.NEVER)
			.addSources(pane.contentBoundsProperty(), pane.vBarPolicyProperty())
			.get()
		);
		vBar.minProperty().bind(pane.vMinProperty());
		pane.vValProperty().bindBidirectional(vBar.valueProperty());
		vBar.maxProperty().bind(pane.vMaxProperty());
		vBar.showButtonsProperty().bind(pane.showButtonsProperty());
		vBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
		vBar.trackIncrementProperty().bind(pane.vTrackIncrementProperty());
		vBar.unitIncrementProperty().bind(pane.vUnitIncrementProperty());
		vBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
		vBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

		// Listeners
		InvalidationListener ll = o -> pane.requestLayout();
		listeners(
			// Base
			onInvalidated(pane.contentProperty())
				.then(c -> {
					if (content != null) {
						content = null;
						viewport.getChildren().clear();
					}
					if (c != null) {
						content = c;
						viewport.getChildren().setAll(content);
					}
				}),
			onChanged(pane.contentBoundsProperty())
				.condition((o, n) -> pane.isAutoHideBars())
				.then((o, n) -> {
					if (o.contentWidth() != n.contentWidth()) {
						hShow.playFromStart();
					}
					if (o.contentHeight() != n.contentHeight()) {
						vShow.playFromStart();
					}
				}),
			// PseudoClasses
			onInvalidated(pane.layoutModeProperty())
				.then(m -> {
					pseudoClassStateChanged(COMPACT_MODE_PSEUDO_CLASS, m == LayoutMode.COMPACT);
					pane.requestLayout();
				}),
			onInvalidated(pane.dragToScrollProperty())
				.then(v -> pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, v)),
			// Layout
			withListener(pane.hBarPolicyProperty(), ll),
			withListener(pane.vBarPolicyProperty(), ll),
			withListener(pane.hBarPosProperty(), ll),
			withListener(pane.vBarPosProperty(), ll),
			withListener(pane.hBarPaddingProperty(), ll),
			withListener(pane.vBarPaddingProperty(), ll),
			// Animations
			onInvalidated(pane.autoHideBarsProperty())
				.then(v -> {
					if (v) {
						if (!hBar.isHover() && !hBar.isPressed()) hHide.playFromStart();
						if (!vBar.isHover() && !vBar.isPressed()) vHide.playFromStart();
					} else {
						hBar.setOpacity(1.0);
						vBar.setOpacity(1.0);
					}
				}),
			// HBar
			onInvalidated(pane.hValProperty())
				.condition(v -> pane.isAutoHideBars())
				.then(v -> {
					hHide.stop();
					hShow.play();
				}),
			onInvalidated(hBar.hoverProperty())
				.condition(v -> pane.isAutoHideBars())
				.then(v -> {
					if (!v && hBar.isPressed()) {
						onChanged(hBar.pressedProperty())
							.then((ov, nv) -> {
								if (!nv) hHide.playFromStart();
							})
							.oneShot()
							.listen();
						return;
					}

					if (v) {
						hHide.stop();
						hShow.playFromStart();
					} else if (!Animations.isPlaying(hShow)) {
						hHide.playFromStart();
					}
				}),
			// VBar
			onInvalidated(pane.vValProperty())
				.then(v -> {
					if (!pane.isAutoHideBars()) return;
					vHide.stop();
					vShow.play();
				}),
			onInvalidated(hBar.hoverProperty())
				.condition(v -> pane.isAutoHideBars())
				.then(v -> {
					if (!v && vBar.isPressed()) {
						onChanged(vBar.pressedProperty())
							.then((ov, nv) -> {
								if (!nv) vHide.playFromStart();
							})
							.oneShot()
							.listen();
						return;
					}

					if (v) {
						vHide.stop();
						vShow.playFromStart();
					} else if (!Animations.isPlaying(vShow)) {
						vHide.playFromStart();
					}
				})
		);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================

	/**
	 * Initializes the behavior by calling {@link VFXScrollPaneBehavior#init()} and by registering the following handlers:
	 * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} to call {@link VFXScrollBarBehavior#mousePressed(MouseEvent)}
	 * <p> - intercepts events of type {@link MouseEvent#MOUSE_DRAGGED} to call {@link VFXScrollBarBehavior#mouseDragged(MouseEvent)}
	 * <p> - intercepts events of type {@link ScrollEvent#SCROLL} and re-routes such events to the appropriate scroll bar,
	 * see {@link VFXScrollBarBehavior#scroll(ScrollEvent)}
	 */
	@Override
	protected void initBehavior(VFXScrollPaneBehavior behavior) {
		VFXScrollPane pane = getSkinnable();
		behavior.init();
		events(
			intercept(viewport, MouseEvent.MOUSE_PRESSED)
				.asFilter()
				.process(behavior::mousePressed),
			intercept(viewport, MouseEvent.MOUSE_DRAGGED)
				.asFilter()
				.process(behavior::mouseDragged),
			intercept(viewport, ScrollEvent.SCROLL).process(e -> behavior.scroll(e, c -> {
				// Re-route scroll events to the appropriate scroll bar's behavior
				Orientation o = pane.getOrientation();
				VFXScrollBar target = switch (o) {
					case VERTICAL -> c.isShiftDown() ? hBar : vBar;
					case HORIZONTAL -> c.isShiftDown() ? vBar : hBar;
				};
				if (target.isVisible()) target.getBehavior().scroll(c);
			})),
			intercept(pane, KeyEvent.KEY_PRESSED).process(behavior::keyPressed)
		);
	}

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
		VFXScrollPane pane = getSkinnable();
		Node content = pane.getContent();
		LayoutMode layoutMode = pane.getLayoutMode();
		ScrollBarPolicy vBarPolicy = pane.getVBarPolicy();

		double contentW = (content != null) ? LayoutUtils.boundWidth(content) : 0.0;
		double prefW = leftInset + contentW + rightInset;
		if (layoutMode == LayoutMode.DEFAULT && vBarPolicy != ScrollBarPolicy.NEVER)
			prefW += LayoutUtils.boundWidth(vBar);
		return prefW;
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		VFXScrollPane pane = getSkinnable();
		Node content = pane.getContent();
		LayoutMode layoutMode = pane.getLayoutMode();
		ScrollBarPolicy hBarPolicy = pane.getHBarPolicy();

		double contentH = (content != null) ? LayoutUtils.boundHeight(content) : 0.0;
		double prefH = topInset + contentH + bottomInset;
		if (layoutMode == LayoutMode.DEFAULT && hBarPolicy != ScrollBarPolicy.NEVER)
			prefH += LayoutUtils.boundHeight(hBar);
		return prefH;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		VFXScrollPane pane = getSkinnable();
		LayoutMode layoutMode = pane.getLayoutMode();
		HPos vBarPos = pane.getVBarPos().toHPos();
		VPos hBarPos = pane.getHBarPos().toVPos();
		ScrollBarPolicy vBarPolicy = pane.getVBarPolicy();
		ScrollBarPolicy hBarPolicy = pane.getHBarPolicy();

		double vBarOffset = pane.getVBarOffset();
		Insets vBarPadding = pane.getVBarPadding();
		double hPadding = snappedLeftInset() + snappedRightInset();

		double hBarOffset = pane.getHBarOffset();
		Insets hBarPadding = pane.getHBarPadding();
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

		if (hBarPolicy != ScrollBarPolicy.NEVER && hBar.isVisible()) vBarH -= hBarH;
		if (vBarPolicy != ScrollBarPolicy.NEVER && vBar.isVisible()) hBarW -= vBarW;

		vBar.resizeRelocate(vBarX, vBarY, vBarW, vBarH);
		hBar.resizeRelocate(hBarX, hBarY, hBarW, hBarH);

		double viewW = (layoutMode == LayoutMode.DEFAULT && vBarPolicy != ScrollBarPolicy.NEVER) ? w - vBarW : w;
		double viewH = (layoutMode == LayoutMode.DEFAULT && hBarPolicy != ScrollBarPolicy.NEVER) ? h - hBarH : h;
		viewport.resizeRelocate(snappedLeftInset(), snappedTopInset(), viewW, viewH);
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
