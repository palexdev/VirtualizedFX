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

package io.github.palexdev.virtualizedfx.controls;

import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.ResourceManager;
import io.github.palexdev.virtualizedfx.beans.VirtualBounds;
import io.github.palexdev.virtualizedfx.controls.behavior.MFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.skins.VirtualScrollPaneSkin;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.HBarPos;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.VBarPos;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Function;

/**
 * Implementation of a scroll pane which is intended to use with virtualized controls.
 * <p>
 * The thing is, usually, virtualized controls do not expose the real bounds of the content because they are virtual bounds.
 * For this reason a scroll pane would have no way to determine its content bounds.
 * This scroll pane allows to specify the {@link Bounds} object to use to determine the content bounds, the scrollable area
 * length.
 * <p></p>
 * Apart from this peculiarity, this is a normal scroll pane, <b>BUT</b>, since it's made from scratch (copied from MaterialFX mostly),
 * it differs in many aspects from the JavaFX's one.
 * <p></p>
 * Since this uses the new {@link MFXScrollBar}s the values are also clamped between 0.0 and 1.0, see {@link MFXScrollBar}
 * for a better explanation.
 * <p>
 * Listing all the features of this scroll pane we have:
 * <p> - The possibility to change the layout strategy for the scroll bars, see {@link LayoutMode} and {@link VirtualScrollPaneSkin}
 * <p> - The possibility of auto hide the scroll bars after a certain amount of time
 * <p> - The scroll bars policy, which <b>differs</b> from the JavaFX's one
 * <p> - The possibility to change the position of the scroll bars, see {@link HBarPos} and {@link VBarPos}
 * <p> - The possibility to specify extra padding for the scroll bars
 * <p> - The possibility to scroll with the mouse (not called pannable anymore but dragToScroll, {@link #dragToScrollProperty()})
 * <p> - Ports all the new features of {@link MFXScrollBar} which they'll be bound to, such as:
 * {@link MFXScrollBar#buttonsVisibleProperty()}, {@link MFXScrollBar#buttonsGapProperty()},
 * {@link MFXScrollBar#trackIncrementProperty()}, {@link MFXScrollBar#unitIncrementProperty()},
 * {@link MFXScrollBar#smoothScrollProperty()} and {@link MFXScrollBar#trackSmoothScrollProperty()}.
 * <p></p>
 * This also offers two new PseudoClasses:
 * <p> - ":compact": active when the {@link #layoutModeProperty()} is set to {@link LayoutMode#COMPACT}
 * <p> - ":drag-to-scroll": active when the {@link #dragToScrollProperty()} is set to true
 * <p></p>
 * Removed features from the JavaFX's one are: fitToHeight and fitToWidth (just here because it's virtual),
 * the possibility to specify the viewport's bounds.
 * <p></p>
 * Last but not least, since this uses the new {@link MFXScrollBar}s, it also allows to change
 * their behavior with {@link #hBarBehaviorProperty()} and {@link #vBarBehaviorProperty()}.
 */
public class VirtualScrollPane extends Control {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "virtual-scroll-pane";
	private final String STYLESHEET = ResourceManager.loadResource("VirtualScrollPane.css");

	private final ObjectProperty<Node> content = new SimpleObjectProperty<>();
	private final ObjectProperty<VirtualBounds> contentBounds = new SimpleObjectProperty<>(VirtualBounds.EMPTY);

	//================================================================================
	// ScrollBars Properties & Config
	//================================================================================
	private final DoubleProperty hMin = PropUtils.clampedDoubleProperty(() -> 0.0, this::getHMax);
	private final DoubleProperty hVal = PropUtils.clampedDoubleProperty(this::getHMin, this::getHMax);
	private final DoubleProperty hMax = PropUtils.clampedDoubleProperty(this::getHMin, () -> 1.0);

	private final DoubleProperty vMin = PropUtils.clampedDoubleProperty(() -> 0.0, this::getVMax);
	private final DoubleProperty vVal = PropUtils.clampedDoubleProperty(this::getVMin, this::getVMax);
	private final DoubleProperty vMax = PropUtils.clampedDoubleProperty(this::getVMin, () -> 1.0);

	private final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(Orientation.VERTICAL);

	private final FunctionProperty<MFXScrollBar, MFXScrollBarBehavior> hBarBehavior = PropUtils.function(MFXScrollBarBehavior::new);
	private final FunctionProperty<MFXScrollBar, MFXScrollBarBehavior> vBarBehavior = PropUtils.function(MFXScrollBarBehavior::new);

	//================================================================================
	// Constructors
	//================================================================================
	public VirtualScrollPane() {
		this(null);
	}

	public VirtualScrollPane(Node content) {
		setContent(content);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected Skin<?> createDefaultSkin() {
		return new VirtualScrollPaneSkin(this);
	}

	@Override
	protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
		return getClassCssMetaData();
	}

	@Override
	public String getUserAgentStylesheet() {
		return STYLESHEET;
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
	private final StyleableObjectProperty<LayoutMode> layoutMode = new StyleableObjectProperty<>(
			StyleableProperties.LAYOUT_MODE,
			this,
			"layoutMode",
			LayoutMode.DEFAULT
	);

	private final StyleableBooleanProperty autoHideBars = new StyleableBooleanProperty(
			StyleableProperties.AUTO_HIDE_BARS,
			this,
			"autoHideBars",
			false
	);

	private final StyleableObjectProperty<ScrollBarPolicy> hBarPolicy = new StyleableObjectProperty<>(
			StyleableProperties.HBAR_POLICY,
			this,
			"hBarPolicy",
			ScrollBarPolicy.DEFAULT
	);

	private final StyleableObjectProperty<ScrollBarPolicy> vBarPolicy = new StyleableObjectProperty<>(
			StyleableProperties.VBAR_POLICY,
			this,
			"vBarPolicy",
			ScrollBarPolicy.DEFAULT
	);

	private final StyleableObjectProperty<HBarPos> hBarPos = new StyleableObjectProperty<>(
			StyleableProperties.HBAR_POS,
			this,
			"hBarPos",
			HBarPos.BOTTOM
	);

	private final StyleableObjectProperty<VBarPos> vBarPos = new StyleableObjectProperty<>(
			StyleableProperties.VBAR_POS,
			this,
			"vBarPos",
			VBarPos.RIGHT
	);

	private final StyleableObjectProperty<Insets> hBarPadding = new StyleableObjectProperty<>(
			StyleableProperties.HBAR_PADDING,
			this,
			"hBarPadding",
			Insets.EMPTY
	);

	private final StyleableObjectProperty<Insets> vBarPadding = new StyleableObjectProperty<>(
			StyleableProperties.VBAR_PADDING,
			this,
			"vBarPadding",
			Insets.EMPTY
	);

	private final StyleableBooleanProperty dragToScroll = new StyleableBooleanProperty(
			StyleableProperties.DRAG_TO_SCROLL,
			this,
			"dragToScroll",
			false
	);

	private final StyleableBooleanProperty buttonsVisible = new StyleableBooleanProperty(
			StyleableProperties.BUTTONS_VISIBLE,
			this,
			"buttonsVisible",
			false
	);

	private final StyleableDoubleProperty buttonsGap = new StyleableDoubleProperty(
			StyleableProperties.BUTTONS_GAP,
			this,
			"buttonsGap",
			3.0
	);

	private final StyleableDoubleProperty hTrackIncrement = new StyleableDoubleProperty(
			StyleableProperties.H_TRACK_INCREMENT,
			this,
			"hTrackIncrement",
			0.1
	);

	private final StyleableDoubleProperty hUnitIncrement = new StyleableDoubleProperty(
			StyleableProperties.H_UNIT_INCREMENT,
			this,
			"hUnitIncrement",
			0.01
	);

	private final StyleableDoubleProperty vTrackIncrement = new StyleableDoubleProperty(
			StyleableProperties.V_TRACK_INCREMENT,
			this,
			"vTrackIncrement",
			0.1
	);

	private final StyleableDoubleProperty vUnitIncrement = new StyleableDoubleProperty(
			StyleableProperties.V_UNIT_INCREMENT,
			this,
			"vUnitIncrement",
			0.01
	);

	private final StyleableBooleanProperty smoothScroll = new StyleableBooleanProperty(
			StyleableProperties.SMOOTH_SCROLL,
			this,
			"smoothScroll",
			false
	);

	private final StyleableBooleanProperty trackSmoothScroll = new StyleableBooleanProperty(
			StyleableProperties.SMOOTH_SCROLL_ON_TRACK_PRESS,
			this,
			"trackSmoothScroll",
			false
	);

	private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
			StyleableProperties.CLIP_BORDER_RADIUS,
			this,
			"clipBorderRadius",
			0.0
	);

	// TODO add clip to original scroll pane

	public LayoutMode getLayoutMode() {
		return layoutMode.get();
	}

	/**
	 * Specifies the layout strategy for the scroll bars, see {@link LayoutMode} or {@link VirtualScrollPaneSkin}
	 * for an explanation.
	 */
	public StyleableObjectProperty<LayoutMode> layoutModeProperty() {
		return layoutMode;
	}

	public void setLayoutMode(LayoutMode layoutMode) {
		this.layoutMode.set(layoutMode);
	}

	public boolean isAutoHideBars() {
		return autoHideBars.get();
	}

	/**
	 * Specifies whether to auto hide the scroll bars after a certain amount of time.
	 */
	public StyleableBooleanProperty autoHideBarsProperty() {
		return autoHideBars;
	}

	public void setAutoHideBars(boolean autoHideBars) {
		this.autoHideBars.set(autoHideBars);
	}

	public ScrollBarPolicy getHBarPolicy() {
		return hBarPolicy.get();
	}

	/**
	 * Specifies the horizontal scroll bar visibility policy.
	 */
	public StyleableObjectProperty<ScrollBarPolicy> hBarPolicyProperty() {
		return hBarPolicy;
	}

	public void setHBarPolicy(ScrollBarPolicy hBarPolicy) {
		this.hBarPolicy.set(hBarPolicy);
	}

	public ScrollBarPolicy getVBarPolicy() {
		return vBarPolicy.get();
	}

	/**
	 * Specifies the vertical scroll bar visibility policy.
	 */
	public StyleableObjectProperty<ScrollBarPolicy> vBarPolicyProperty() {
		return vBarPolicy;
	}

	public void setVBarPolicy(ScrollBarPolicy vBarPolicy) {
		this.vBarPolicy.set(vBarPolicy);
	}

	public HBarPos getHBarPos() {
		return hBarPos.get();
	}

	/**
	 * Specifies the position of the horizontal scroll bar.
	 */
	public StyleableObjectProperty<HBarPos> hBarPosProperty() {
		return hBarPos;
	}

	public void setHBarPos(HBarPos hBarPos) {
		this.hBarPos.set(hBarPos);
	}

	public VBarPos getVBarPos() {
		return vBarPos.get();
	}

	/**
	 * Specifies the position of the vertical scroll bar.
	 */
	public StyleableObjectProperty<VBarPos> vBarPosProperty() {
		return vBarPos;
	}

	public void setVBarPos(VBarPos vBarPos) {
		this.vBarPos.set(vBarPos);
	}

	public Insets getHBarPadding() {
		return hBarPadding.get();
	}

	/**
	 * Specifies the extra padding for the horizontal scroll bar.
	 */
	public StyleableObjectProperty<Insets> hBarPaddingProperty() {
		return hBarPadding;
	}

	public void setHBarPadding(Insets hBarPadding) {
		this.hBarPadding.set(hBarPadding);
	}

	public Insets getVBarPadding() {
		return vBarPadding.get();
	}

	/**
	 * Specifies the extra padding for the vertical scroll bar.
	 */
	public StyleableObjectProperty<Insets> vBarPaddingProperty() {
		return vBarPadding;
	}

	public void setVBarPadding(Insets vBarPadding) {
		this.vBarPadding.set(vBarPadding);
	}

	public boolean isDragToScroll() {
		return dragToScroll.get();
	}

	/**
	 * Specifies whether the content can be scrolled with mouse dragging.
	 */
	public StyleableBooleanProperty dragToScrollProperty() {
		return dragToScroll;
	}

	public void setDragToScroll(boolean dragToScroll) {
		this.dragToScroll.set(dragToScroll);
	}

	public boolean isButtonsVisible() {
		return buttonsVisible.get();
	}

	/**
	 * Specifies whether to show or not the scroll bars' buttons.
	 */
	public StyleableBooleanProperty buttonsVisibleProperty() {
		return buttonsVisible;
	}

	public void setButtonsVisible(boolean buttonsVisible) {
		this.buttonsVisible.set(buttonsVisible);
	}

	public double getButtonsGap() {
		return buttonsGap.get();
	}

	/**
	 * Specifies the gap between the scroll bars' thumb and their buttons.
	 */
	public StyleableDoubleProperty buttonsGapProperty() {
		return buttonsGap;
	}

	public void setButtonsGap(double buttonsGap) {
		this.buttonsGap.set(buttonsGap);
	}

	public double getHTrackIncrement() {
		return hTrackIncrement.get();
	}

	/**
	 * Specifies the amount added/subtracted to the horizontal scroll bar's value used by the
	 * scroll bar's track.
	 */
	public StyleableDoubleProperty hTrackIncrementProperty() {
		return hTrackIncrement;
	}

	public void setHTrackIncrement(double hTrackIncrement) {
		this.hTrackIncrement.set(hTrackIncrement);
	}

	public double getHUnitIncrement() {
		return hUnitIncrement.get();
	}

	/**
	 * Specifies the amount added/subtracted to the horizontal scroll bar's value used by the
	 * buttons and by scrolling.
	 */
	public StyleableDoubleProperty hUnitIncrementProperty() {
		return hUnitIncrement;
	}

	public void setHUnitIncrement(double hUnitIncrement) {
		this.hUnitIncrement.set(hUnitIncrement);
	}

	public double getVTrackIncrement() {
		return vTrackIncrement.get();
	}

	/**
	 * Specifies the amount added/subtracted to the vertical scroll bar's value used by the
	 * scroll bar's track.
	 */
	public StyleableDoubleProperty vTrackIncrementProperty() {
		return vTrackIncrement;
	}

	public void setVTrackIncrement(double trackIncrement) {
		this.vTrackIncrement.set(trackIncrement);
	}

	public double getVUnitIncrement() {
		return vUnitIncrement.get();
	}

	/**
	 * Specifies the amount added/subtracted to the vertical scroll bar's value used by the
	 * buttons and by scrolling.
	 */
	public StyleableDoubleProperty vUnitIncrementProperty() {
		return vUnitIncrement;
	}

	public void setVUnitIncrement(double unitIncrement) {
		this.vUnitIncrement.set(unitIncrement);
	}

	public boolean isSmoothScroll() {
		return smoothScroll.get();
	}

	/**
	 * Specifies whether the scrolling should be smooth, done by animations.
	 */
	public StyleableBooleanProperty smoothScrollProperty() {
		return smoothScroll;
	}

	public void setSmoothScroll(boolean smoothScroll) {
		this.smoothScroll.set(smoothScroll);
	}

	public boolean isTrackSmoothScroll() {
		return trackSmoothScroll.get();
	}

	/**
	 * Specifies if the scrolling made by the track should be smooth, done by animations.
	 * <p></p>
	 * The default behavior considers this feature an addition to the {@link #smoothScrollProperty()},
	 * meaning that for this to work the aforementioned feature must be enabled too.
	 */
	public StyleableBooleanProperty trackSmoothScrollProperty() {
		return trackSmoothScroll;
	}

	public void setTrackSmoothScroll(boolean trackSmoothScroll) {
		this.trackSmoothScroll.set(trackSmoothScroll);
	}

	public double getClipBorderRadius() {
		return clipBorderRadius.get();
	}

	/**
	 * Used by the viewport's clip to set its border radius.
	 * This is useful when you want to make a rounded scroll pane, this
	 * prevents the content from going outside the view.
	 * <p></p>
	 * <b>Side note:</b> the clip is a {@link Rectangle}, now for some
	 * fucking reason the rectangle's arcWidth and arcHeight values used to make
	 * it round do not act like the border-radius or background-radius properties,
	 * instead their value is usually 2 / 2.5 times the latter.
	 * So for a border radius of 5 you want this value to be at least 10/13.
	 */
	public StyleableDoubleProperty clipBorderRadiusProperty() {
		return clipBorderRadius;
	}

	public void setClipBorderRadius(double clipBorderRadius) {
		this.clipBorderRadius.set(clipBorderRadius);
	}

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<VirtualScrollPane> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<VirtualScrollPane, LayoutMode> LAYOUT_MODE =
				FACTORY.createEnumCssMetaData(
						LayoutMode.class,
						"-fx-layout-mode",
						VirtualScrollPane::layoutModeProperty,
						LayoutMode.DEFAULT
				);

		private static final CssMetaData<VirtualScrollPane, Boolean> AUTO_HIDE_BARS =
				FACTORY.createBooleanCssMetaData(
						"-fx-autohide-bars",
						VirtualScrollPane::autoHideBarsProperty,
						false
				);

		private static final CssMetaData<VirtualScrollPane, ScrollBarPolicy> HBAR_POLICY =
				FACTORY.createEnumCssMetaData(
						ScrollBarPolicy.class,
						"-fx-hbar-policy",
						VirtualScrollPane::hBarPolicyProperty,
						ScrollBarPolicy.DEFAULT
				);

		private static final CssMetaData<VirtualScrollPane, ScrollBarPolicy> VBAR_POLICY =
				FACTORY.createEnumCssMetaData(
						ScrollBarPolicy.class,
						"-fx-vbar-policy",
						VirtualScrollPane::vBarPolicyProperty,
						ScrollBarPolicy.DEFAULT
				);

		private static final CssMetaData<VirtualScrollPane, HBarPos> HBAR_POS =
				FACTORY.createEnumCssMetaData(
						HBarPos.class,
						"-fx-hbar-pos",
						VirtualScrollPane::hBarPosProperty,
						HBarPos.BOTTOM
				);

		private static final CssMetaData<VirtualScrollPane, VBarPos> VBAR_POS =
				FACTORY.createEnumCssMetaData(
						VBarPos.class,
						"-fx-vbar-pos",
						VirtualScrollPane::vBarPosProperty,
						VBarPos.RIGHT
				);

		private static final CssMetaData<VirtualScrollPane, Insets> HBAR_PADDING =
				FACTORY.createInsetsCssMetaData(
						"-fx-hbar-padding",
						VirtualScrollPane::hBarPaddingProperty,
						Insets.EMPTY
				);

		private static final CssMetaData<VirtualScrollPane, Insets> VBAR_PADDING =
				FACTORY.createInsetsCssMetaData(
						"-fx-vbar-padding",
						VirtualScrollPane::vBarPaddingProperty,
						Insets.EMPTY
				);

		private static final CssMetaData<VirtualScrollPane, Boolean> DRAG_TO_SCROLL =
				FACTORY.createBooleanCssMetaData(
						"-fx-drag-to-scroll",
						VirtualScrollPane::dragToScrollProperty,
						false
				);

		private static final CssMetaData<VirtualScrollPane, Boolean> BUTTONS_VISIBLE =
				FACTORY.createBooleanCssMetaData(
						"-fx-buttons-visible",
						VirtualScrollPane::buttonsVisibleProperty,
						false
				);

		private static final CssMetaData<VirtualScrollPane, Number> BUTTONS_GAP =
				FACTORY.createSizeCssMetaData(
						"-fx-buttons-gap",
						VirtualScrollPane::buttonsGapProperty,
						3.0
				);

		private static final CssMetaData<VirtualScrollPane, Number> H_TRACK_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-fx-htrack-increment",
						VirtualScrollPane::hTrackIncrementProperty,
						0.1
				);

		private static final CssMetaData<VirtualScrollPane, Number> H_UNIT_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-fx-hunit-increment",
						VirtualScrollPane::hUnitIncrementProperty,
						0.01
				);

		private static final CssMetaData<VirtualScrollPane, Number> V_TRACK_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-fx-vtrack-increment",
						VirtualScrollPane::vTrackIncrementProperty,
						0.1
				);

		private static final CssMetaData<VirtualScrollPane, Number> V_UNIT_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-fx-vunit-increment",
						VirtualScrollPane::vUnitIncrementProperty,
						0.01
				);

		private static final CssMetaData<VirtualScrollPane, Boolean> SMOOTH_SCROLL =
				FACTORY.createBooleanCssMetaData(
						"-fx-smooth-scroll",
						VirtualScrollPane::smoothScrollProperty,
						false
				);

		private static final CssMetaData<VirtualScrollPane, Boolean> SMOOTH_SCROLL_ON_TRACK_PRESS =
				FACTORY.createBooleanCssMetaData(
						"-fx-track-smooth-scroll",
						VirtualScrollPane::trackSmoothScrollProperty,
						false
				);

		private static final CssMetaData<VirtualScrollPane, Number> CLIP_BORDER_RADIUS =
				FACTORY.createSizeCssMetaData(
						"-fx-clip-border-radius",
						VirtualScrollPane::clipBorderRadiusProperty,
						0.0
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Control.getClassCssMetaData(),
					LAYOUT_MODE, HBAR_POLICY, VBAR_POLICY, HBAR_POS, VBAR_POS,
					HBAR_PADDING, VBAR_PADDING,
					AUTO_HIDE_BARS, DRAG_TO_SCROLL,
					BUTTONS_VISIBLE, BUTTONS_GAP,
					H_TRACK_INCREMENT, H_UNIT_INCREMENT, V_TRACK_INCREMENT, V_UNIT_INCREMENT,
					SMOOTH_SCROLL, SMOOTH_SCROLL_ON_TRACK_PRESS,
					CLIP_BORDER_RADIUS
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public Node getContent() {
		return content.get();
	}

	/**
	 * Specifies the current scroll pane's content.
	 */
	public ObjectProperty<Node> contentProperty() {
		return content;
	}

	public void setContent(Node content) {
		this.content.set(content);
	}

	public VirtualBounds getContentBounds() {
		return contentBounds.get();
	}

	/**
	 * Specifies the content bounds, this cannot be ignored to make the scroll pane
	 * work as intended.
	 */
	public ObjectProperty<VirtualBounds> contentBoundsProperty() {
		return contentBounds;
	}

	public void setContentBounds(VirtualBounds contentBounds) {
		this.contentBounds.set(contentBounds);
	}

	public double getHMin() {
		return hMin.get();
	}

	/**
	 * Specifies the horizontal scroll bar's minimum value.
	 */
	public DoubleProperty hMinProperty() {
		return hMin;
	}

	public void setHMin(double hMin) {
		this.hMin.set(hMin);
	}

	public double getHVal() {
		return hVal.get();
	}

	/**
	 * Specifies the horizontal scroll bar's value.
	 */
	public DoubleProperty hValProperty() {
		return hVal;
	}

	public void setHVal(double hVal) {
		this.hVal.set(hVal);
	}

	public double getHMax() {
		return hMax.get();
	}

	/**
	 * Specifies the horizontal scroll bar's maximum value.
	 */
	public DoubleProperty hMaxProperty() {
		return hMax;
	}

	public void setHMax(double hMax) {
		this.hMax.set(hMax);
	}

	public double getVMin() {
		return vMin.get();
	}

	/**
	 * Specifies the vertical scroll bar's minimum value.
	 */
	public DoubleProperty vMinProperty() {
		return vMin;
	}

	public void setVMin(double vMin) {
		this.vMin.set(vMin);
	}

	public double getVVal() {
		return vVal.get();
	}

	/**
	 * Specifies the vertical scroll bar's value.
	 */
	public DoubleProperty vValProperty() {
		return vVal;
	}

	public void setVVal(double vVal) {
		this.vVal.set(vVal);
	}

	public double getVMax() {
		return vMax.get();
	}

	/**
	 * Specifies the vertical scroll bar's maximum value.
	 */
	public DoubleProperty vMaxProperty() {
		return vMax;
	}

	public void setVMax(double vMax) {
		this.vMax.set(vMax);
	}

	public Orientation getOrientation() {
		return orientation.get();
	}

	/**
	 * Specifies the main orientation of the scroll pane.
	 * <p>
	 * This is used by the skin to determine the behavior of the scroll when the Shift button is
	 * pressed.
	 * By default, for:
	 * <p> - VERTICAL orientation: if Shift is pressed the scroll will be horizontal
	 * <p> - HORIZONTAL orientation: if Shift is pressed the scroll will be vertical
	 */
	public ObjectProperty<Orientation> orientationProperty() {
		return orientation;
	}

	public void setOrientation(Orientation orientation) {
		this.orientation.set(orientation);
	}

	public Function<MFXScrollBar, MFXScrollBarBehavior> getHBarBehavior() {
		return hBarBehavior.get();
	}

	/**
	 * Specifies the function used to build the horizontal scroll bar's behavior.
	 */
	public FunctionProperty<MFXScrollBar, MFXScrollBarBehavior> hBarBehaviorProperty() {
		return hBarBehavior;
	}

	public void setHBarBehavior(Function<MFXScrollBar, MFXScrollBarBehavior> hBarBehavior) {
		this.hBarBehavior.set(hBarBehavior);
	}

	public Function<MFXScrollBar, MFXScrollBarBehavior> getVBarBehavior() {
		return vBarBehavior.get();
	}

	/**
	 * Specifies the function used to build the vertical scroll bar's behavior.
	 */
	public FunctionProperty<MFXScrollBar, MFXScrollBarBehavior> vBarBehaviorProperty() {
		return vBarBehavior;
	}

	public void setVBarBehavior(Function<MFXScrollBar, MFXScrollBarBehavior> vBarBehavior) {
		this.vBarBehavior.set(vBarBehavior);
	}
}
