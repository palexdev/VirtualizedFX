/*
 * Copyright (C) 2025 Parisi Alessandro - alessandro.parisi406@gmail.com
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

package io.github.palexdev.virtualizedfx.controls;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollPaneBehavior;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollPaneSkin;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

import static io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.HBarPos;
import static io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.VBarPos;

/**
 * My personal custom implementation of a scroll pane from scratch, follows the MVC pattern as enforced by {@link Control}.
 * The default skin is {@link VFXScrollPaneSkin}. The default behavior is {@link VFXScrollPaneBehavior}. Also implements {@link VFXStyleable}.
 * <p></p>
 * <b>Features:</b>
 * <p> - You can change how the scroll bars are laid out as well as their appearance with the {@link #layoutModeProperty()}
 * <p> - You can align the content within the viewport by setting the {@link #alignmentProperty()}
 * (works only when the content is smaller than the viewport)
 * <p> - The {@link #mainAxisProperty()} determines which is the primary scroll direction. By default, it's vertical and
 * to scroll horizontally you have to press the {@code Shift} key
 * <p> - You can make the content always fit the size of the scroll pane by setting the properties
 * {@link #fitToWidthProperty()} and {@link #fitToHeightProperty()} (works only for non-virtualized contents)
 * <p> - You choose on which side to have the two scroll bars with {@link #vBarPosProperty()} and {@link #hBarPosProperty()}
 * <p> - The {@link #scrollBarsGapProperty()} determines how much space separates the scroll bars
 * <p> - You can make the scroll bars hide automatically after a certain amount of time, {@link #autoHideBarsProperty()}
 * <p> - You can disable the bars and the scrolling by setting the relative policies:
 * {@link #vBarPolicyProperty()}, {@link #hBarPolicyProperty()}
 * <p> - Allows scrolling by dragging the mouse on the viewport by enabling the {@link #dragToScrollProperty()}, and
 * it's also possible to enable smooth scrolling for it: {@link #dragSmoothScrollProperty()}
 * <p> - All the properties of {@link VFXScrollBar} are ported and bound here:
 * {@link #vTrackIncrementProperty()}, {@link #vUnitIncrementProperty()}, {@link #hTrackIncrementProperty()}, {@link #hUnitIncrementProperty()},
 * {@link #showButtonsProperty()}, {@link #buttonsGapProperty()}, {@link #smoothScrollProperty()}, {@link #trackSmoothScrollProperty()}
 * <p> - Finally you can set the radius for the viewport's clip with the {@link #clipBorderRadiusProperty()}
 * <p></p>
 * Uses two new PseudoClasses:
 * <p> - ":compact": active when the {@link #layoutModeProperty()} is set to {@link LayoutMode#COMPACT}
 * <p> - ":drag-to-scroll": active when the {@link #dragToScrollProperty()} is set to true
 * <p></p>
 * Last but not least, since this uses the new {@link io.github.palexdev.virtualizedfx.controls.VFXScrollBar}s, it also allows to change their behavior with
 * {@link #hBarBehaviorProperty()} and {@link #vBarBehaviorProperty()}.
 */
public class VFXScrollPane extends Control<VFXScrollPaneBehavior> implements VFXStyleable {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final PseudoClass DRAG_TO_SCROLL_PSEUDO_CLASS = PseudoClass.getPseudoClass("drag-to-scroll");

    //================================================================================
    // Properties
    //================================================================================
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>();

    // VBar
    private final DoubleProperty vMin = PropUtils.clampedDoubleProperty(
        () -> 0.0,
        this::getVMax
    );
    private final DoubleProperty vValue = PropUtils.clampedDoubleProperty(
        this::getVMin,
        this::getVMax
    );
    private final DoubleProperty vMax = PropUtils.clampedDoubleProperty(
        this::getVMin,
        () -> 1.0
    );
    private final FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> vBarBehavior = PropUtils.function(VFXScrollBarBehavior::new);

    // HBar
    private final DoubleProperty hMin = PropUtils.clampedDoubleProperty(
        () -> 0.0,
        this::getHMax
    );
    private final DoubleProperty hValue = PropUtils.clampedDoubleProperty(
        this::getHMin,
        this::getHMax
    );
    private final DoubleProperty hMax = PropUtils.clampedDoubleProperty(
        this::getHMin,
        () -> 1.0
    );
    private final FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> hBarBehavior = PropUtils.function(VFXScrollBarBehavior::new);

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollPane() {
        this(null);
    }

    public VFXScrollPane(Node content) {
        setContent(content);
        init();
    }

    //================================================================================
    // Methods
    //================================================================================
    private void init() {
        setDefaultStyleClasses();
        getStylesheets().add(VFXResources.loadResource("VFXScrollPane.css"));
        setDefaultBehaviorProvider();

        setVMin(0.0);
        setVMax(1.0);
        setHMin(0.0);
        setHMax(1.0);
    }

    /**
     * @return the appropriate sizes for the scroll pane's content. For virtualized containers ({@link VFXContainer})
     * the values are given by the {@link VFXContainer#virtualMaxXProperty()} and the {@link VFXContainer#virtualMaxYProperty()}
     */
    public Size getContentBounds() {
        Node content = getContent();
        return switch (content) {
            case null -> Size.zero();
            // TODO should we snap these sizes in the helpers?
            case VFXContainer<?> c -> Size.of(
                snapSizeX(c.getVirtualMaxX()),
                snapSizeY(c.getVirtualMaxY())
            );
            default -> {
                Bounds b = content.getLayoutBounds();
                yield Size.of(b.getWidth(), b.getHeight());
            }
        };
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXScrollPaneSkin(this);
    }

    @Override
    public Supplier<VFXScrollPaneBehavior> defaultBehaviorProvider() {
        return () -> new VFXScrollPaneBehavior(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("vfx-scroll-pane");
    }

    //================================================================================
    // Styleable Properties
    //================================================================================
    private final StyleableObjectProperty<LayoutMode> layoutMode = new StyleableObjectProperty<>(
        StyleableProperties.LAYOUT_MODE,
        this,
        "layoutMode",
        LayoutMode.DEFAULT
    ) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(LayoutMode.COMPACT_PSEUDO_CLASS, get() == LayoutMode.COMPACT);
        }
    };

    private final StyleableObjectProperty<Pos> alignment = new StyleableObjectProperty<>(
        StyleableProperties.ALIGNMENT,
        this,
        "alignment",
        Pos.TOP_LEFT
    );

    private final StyleableObjectProperty<Orientation> mainAxis = new StyleableObjectProperty<>(
        StyleableProperties.MAIN_AXIS,
        this,
        "mainAxis",
        Orientation.VERTICAL
    );

    private final StyleableBooleanProperty fitToWidth = new StyleableBooleanProperty(
        StyleableProperties.FIT_TO_WIDTH,
        this,
        "fitToWidth",
        false
    );

    private final StyleableBooleanProperty fitToHeight = new StyleableBooleanProperty(
        StyleableProperties.FIT_TO_HEIGHT,
        this,
        "fitToHeight",
        false
    );

    private final StyleableObjectProperty<VBarPos> vBarPos = new StyleableObjectProperty<>(
        StyleableProperties.VBAR_POS,
        this,
        "vBarPos",
        VBarPos.RIGHT
    );

    private final StyleableObjectProperty<HBarPos> hBarPos = new StyleableObjectProperty<>(
        StyleableProperties.HBAR_POS,
        this,
        "hBarPos",
        HBarPos.BOTTOM
    );

    private final StyleableDoubleProperty scrollBarsGap = new StyleableDoubleProperty(
        StyleableProperties.SCROLL_BARS_GAP,
        this,
        "scrollBarsGap",
        4.0
    );

    private final StyleableBooleanProperty autoHideBars = new StyleableBooleanProperty(
        StyleableProperties.AUTO_HIDE_BARS,
        this,
        "autoHideBars",
        false
    );

    private final StyleableObjectProperty<ScrollBarPolicy> vBarPolicy = new StyleableObjectProperty<>(
        StyleableProperties.VBAR_POLICY,
        this,
        "vBarPolicy",
        ScrollBarPolicy.DEFAULT
    );

    private final StyleableObjectProperty<ScrollBarPolicy> hBarPolicy = new StyleableObjectProperty<>(
        StyleableProperties.HBAR_POLICY,
        this,
        "hBarPolicy",
        ScrollBarPolicy.DEFAULT
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

    private final StyleableBooleanProperty showButtons = new StyleableBooleanProperty(
        StyleableProperties.SHOW_BUTTONS,
        this,
        "showButtons",
        false
    );

    private final StyleableDoubleProperty buttonsGap = new StyleableDoubleProperty(
        StyleableProperties.BUTTONS_GAP,
        this,
        "buttonsGap",
        4.0
    );

    private final StyleableBooleanProperty smoothScroll = new StyleableBooleanProperty(
        StyleableProperties.SMOOTH_SCROLL,
        this,
        "smoothScroll",
        false
    );

    private final StyleableBooleanProperty trackSmoothScroll = new StyleableBooleanProperty(
        StyleableProperties.TRACK_SMOOTH_SCROLL,
        this,
        "trackSmoothScroll",
        false
    );

    private final StyleableBooleanProperty dragToScroll = new StyleableBooleanProperty(
        StyleableProperties.DRAG_TO_SCROLL,
        this,
        "dragToScroll",
        false
    ) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(DRAG_TO_SCROLL_PSEUDO_CLASS, get());
        }
    };

    private final StyleableBooleanProperty dragSmoothScroll = new StyleableBooleanProperty(
        StyleableProperties.DRAG_SMOOTH_SCROLL,
        this,
        "dragSmoothScroll",
        false
    );

    private final StyleableDoubleProperty clipBorderRadius = new StyleableDoubleProperty(
        StyleableProperties.CLIP_BORDER_RADIUS,
        this,
        "clipBorderRadius",
        0.0
    );

    public LayoutMode getLayoutMode() {
        return layoutMode.get();
    }

    public StyleableObjectProperty<LayoutMode> layoutModeProperty() {
        return layoutMode;
    }

    public void setLayoutMode(LayoutMode layoutMode) {
        this.layoutMode.set(layoutMode);
    }

    public Pos getAlignment() {
        return alignment.get();
    }

    /**
     * Allows to align the scroll pane's content within its viewport. The alignment is ignored if the content is larger
     * than the viewport.
     * <p>
     * This is also settable via CSS with the "-vfx-alignment" property.
     */
    public StyleableObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    public void setAlignment(Pos alignment) {
        this.alignment.set(alignment);
    }

    public Orientation getMainAxis() {
        return mainAxis.get();
    }

    /**
     * Specifies the main scroll axis.
     * <p>
     * This is used by the skin to determine the behavior of the scroll when the Shift button is
     * pressed.
     * By default, for:
     * <p> - VERTICAL orientation: if Shift is pressed the scroll will be horizontal
     * <p> - HORIZONTAL orientation: if Shift is pressed the scroll will be vertical
     * <p>
     * This is also settable via CSS with the "-vfx-main-axis" property.
     */
    public StyleableObjectProperty<Orientation> mainAxisProperty() {
        return mainAxis;
    }

    public void setMainAxis(Orientation mainAxis) {
        this.mainAxis.set(mainAxis);
    }

    public boolean isFitToWidth() {
        return fitToWidth.get();
    }

    /**
     * Makes the content be at least as wide as the scroll pane.
     * <p>
     * This is also settable via CSS with the "-vfx-fit-to-width" property.
     */
    public StyleableBooleanProperty fitToWidthProperty() {
        return fitToWidth;
    }

    public void setFitToWidth(boolean fitToWidth) {
        this.fitToWidth.set(fitToWidth);
    }

    public boolean isFitToHeight() {
        return fitToHeight.get();
    }

    /**
     * Makes the content be at least as tall as the scroll pane.
     * <p>
     * This is also settable via CSS with the "-vfx-fit-to-height" property.
     */
    public StyleableBooleanProperty fitToHeightProperty() {
        return fitToHeight;
    }

    public void setFitToHeight(boolean fitToHeight) {
        this.fitToHeight.set(fitToHeight);
    }

    public VBarPos getVBarPos() {
        return vBarPos.get();
    }

    /**
     * Specifies the position of the vertical scroll bar.
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-pos" property.
     */
    public StyleableObjectProperty<VBarPos> vBarPosProperty() {
        return vBarPos;
    }

    public void setVBarPos(VBarPos vBarPos) {
        this.vBarPos.set(vBarPos);
    }

    public HBarPos getHBarPos() {
        return hBarPos.get();
    }

    /**
     * Specifies the position of the horizontal scroll bar.
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-pos" property.
     */
    public StyleableObjectProperty<HBarPos> hBarPosProperty() {
        return hBarPos;
    }

    public void setHBarPos(HBarPos hBarPos) {
        this.hBarPos.set(hBarPos);
    }

    /**
     * Convenience method to combine {@link #setVBarPos(VBarPos)} and {@link #setHBarPos(HBarPos)}.
     * However, note that only four positions are allowed: {@link Pos#TOP_LEFT}, {@link Pos#TOP_RIGHT},
     * {@link Pos#BOTTOM_LEFT}, {@link Pos#BOTTOM_RIGHT} (default!).
     */
    public void setScrollBarsPos(Pos pos) {
        switch (pos) {
            case TOP_LEFT -> {
                setVBarPos(VBarPos.LEFT);
                setHBarPos(HBarPos.TOP);
            }
            case TOP_RIGHT -> {
                setVBarPos(VBarPos.RIGHT);
                setHBarPos(HBarPos.TOP);
            }
            case BOTTOM_LEFT -> {
                setVBarPos(VBarPos.LEFT);
                setHBarPos(HBarPos.BOTTOM);
            }
            default -> {
                // Default is BOTTOM_RIGHT
                setVBarPos(VBarPos.RIGHT);
                setHBarPos(HBarPos.BOTTOM);
            }
        }
    }

    public double getScrollBarsGap() {
        return scrollBarsGap.get();
    }

    /**
     * Determines how much space divides the two scroll bars. Imagine this as the corner UI element that was present in
     * the scroll panes back in the day.
     * <p>
     * This is also settable via CSS with the "-vfx-bars-gap" property.
     */
    public StyleableDoubleProperty scrollBarsGapProperty() {
        return scrollBarsGap;
    }

    public void setScrollBarsGap(double scrollBarsGap) {
        this.scrollBarsGap.set(scrollBarsGap);
    }

    public boolean isAutoHideBars() {
        return autoHideBars.get();
    }

    /**
     * Specifies whether to auto hide the scroll bars after a certain amount of time.
     * <p>
     * This is also settable via CSS with the "-vfx-auto-hide-bars" property.
     */
    public StyleableBooleanProperty autoHideBarsProperty() {
        return autoHideBars;
    }

    public void setAutoHideBars(boolean autoHideBars) {
        this.autoHideBars.set(autoHideBars);
    }

    public ScrollBarPolicy getVBarPolicy() {
        return vBarPolicy.get();
    }

    /**
     * Specifies the vertical scroll bar visibility policy.
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-policy" property.
     */
    public StyleableObjectProperty<ScrollBarPolicy> vBarPolicyProperty() {
        return vBarPolicy;
    }

    public void setVBarPolicy(ScrollBarPolicy vBarPolicy) {
        this.vBarPolicy.set(vBarPolicy);
    }

    public ScrollBarPolicy getHBarPolicy() {
        return hBarPolicy.get();
    }

    /**
     * Specifies the horizontal scroll bar visibility policy.
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-policy" property.
     */
    public StyleableObjectProperty<ScrollBarPolicy> hBarPolicyProperty() {
        return hBarPolicy;
    }

    public void setHBarPolicy(ScrollBarPolicy hBarPolicy) {
        this.hBarPolicy.set(hBarPolicy);
    }

    public double getVTrackIncrement() {
        return vTrackIncrement.get();
    }

    /**
     * Specifies the amount added/subtracted to the vertical scroll bar's value used by the
     * scroll bar's track.
     * <p>
     * This is also settable via CSS with the "-vfx-vtrack-increment" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-vunit-increment" property.
     */
    public StyleableDoubleProperty vUnitIncrementProperty() {
        return vUnitIncrement;
    }

    public void setVUnitIncrement(double unitIncrement) {
        this.vUnitIncrement.set(unitIncrement);
    }

    public double getHTrackIncrement() {
        return hTrackIncrement.get();
    }

    /**
     * Specifies the amount added/subtracted to the horizontal scroll bar's value used by the
     * scroll bar's track.
     * <p>
     * This is also settable via CSS with the "-vfx-htrack-increment" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-hunit-increment" property.
     */
    public StyleableDoubleProperty hUnitIncrementProperty() {
        return hUnitIncrement;
    }

    public void setHUnitIncrement(double hUnitIncrement) {
        this.hUnitIncrement.set(hUnitIncrement);
    }

    public boolean isShowButtons() {
        return showButtons.get();
    }

    /**
     * Specifies whether to show or not the scroll bars' buttons.
     * <p>
     * This is also settable via CSS with the "-vfx-show-buttons" property.
     */
    public StyleableBooleanProperty showButtonsProperty() {
        return showButtons;
    }

    public void setShowButtons(boolean showButtons) {
        this.showButtons.set(showButtons);
    }

    public double getButtonsGap() {
        return buttonsGap.get();
    }

    /**
     * Specifies the gap between the scroll bars' thumb and their buttons.
     * <p>
     * This is also settable via CSS with the "-vfx-buttons-gap" property.
     */
    public StyleableDoubleProperty buttonsGapProperty() {
        return buttonsGap;
    }

    public void setButtonsGap(double buttonsGap) {
        this.buttonsGap.set(buttonsGap);
    }


    public boolean isSmoothScroll() {
        return smoothScroll.get();
    }

    /**
     * Specifies whether the scrolling should be smooth, done by animations.
     * <p>
     * This is also settable via CSS with the "-vfx-smooth-scroll" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-track-smooth-scroll" property.
     */
    public StyleableBooleanProperty trackSmoothScrollProperty() {
        return trackSmoothScroll;
    }

    public void setTrackSmoothScroll(boolean trackSmoothScroll) {
        this.trackSmoothScroll.set(trackSmoothScroll);
    }

    public boolean isDragToScroll() {
        return dragToScroll.get();
    }

    /**
     * Specifies whether the content can be scrolled with mouse dragging.
     * <p>
     * This is also settable via CSS with the "-vfx-drag-to-scroll" property.
     */
    public StyleableBooleanProperty dragToScrollProperty() {
        return dragToScroll;
    }

    public void setDragToScroll(boolean dragToScroll) {
        this.dragToScroll.set(dragToScroll);
    }

    public boolean isDragSmoothScroll() {
        return dragSmoothScroll.get();
    }

    /**
     * Specifies whether to use animations for the {@link #dragToScrollProperty()} feature, making the scroll smooth.
     * <p>
     * This is also settable via CSS with the "-vfx-drag-smooth-scroll" property.
     */
    public StyleableBooleanProperty dragSmoothScrollProperty() {
        return dragSmoothScroll;
    }

    public void setDragSmoothScroll(boolean dragSmoothScroll) {
        this.dragSmoothScroll.set(dragSmoothScroll);
    }

    public double getClipBorderRadius() {
        return clipBorderRadius.get();
    }

    /**
     * Used by the viewport's clip to set its border radius.
     * This is useful when you want to make a rounded scroll pane and prevents the content from going outside the view.
     * <p></p>
     * <b>Side note:</b> the clip is a {@link Rectangle}, now for some
     * fucking reason the rectangle's arcWidth and arcHeight values used to make
     * it round do not act like the border-radius or background-radius properties,
     * instead their value is usually 2 / 2.5 times the latter.
     * So for a border radius of 5 you want this value to be at least 10/13.
     * <p>
     * This is also settable via CSS with the "-vfx-clip-border-radius" property.
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
        private static final StyleablePropertyFactory<VFXScrollPane> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXScrollPane, LayoutMode> LAYOUT_MODE =
            FACTORY.createEnumCssMetaData(
                LayoutMode.class,
                "-vfx-layout-mode",
                VFXScrollPane::layoutModeProperty,
                LayoutMode.DEFAULT
            );

        private static final CssMetaData<VFXScrollPane, Pos> ALIGNMENT =
            FACTORY.createEnumCssMetaData(
                Pos.class,
                "-vfx-alignment",
                VFXScrollPane::alignmentProperty,
                Pos.TOP_LEFT
            );

        private static final CssMetaData<VFXScrollPane, Orientation> MAIN_AXIS =
            FACTORY.createEnumCssMetaData(
                Orientation.class,
                "-vfx-main-axis",
                VFXScrollPane::mainAxisProperty,
                Orientation.VERTICAL
            );

        private static final CssMetaData<VFXScrollPane, Boolean> FIT_TO_WIDTH =
            FACTORY.createBooleanCssMetaData(
                "-vfx-fit-to-width",
                VFXScrollPane::fitToWidthProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, Boolean> FIT_TO_HEIGHT =
            FACTORY.createBooleanCssMetaData(
                "-vfx-fit-to-height",
                VFXScrollPane::fitToHeightProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, VBarPos> VBAR_POS =
            FACTORY.createEnumCssMetaData(
                ScrollPaneEnums.VBarPos.class,
                "-vfx-vbar-pos",
                VFXScrollPane::vBarPosProperty,
                ScrollPaneEnums.VBarPos.RIGHT
            );

        private static final CssMetaData<VFXScrollPane, HBarPos> HBAR_POS =
            FACTORY.createEnumCssMetaData(
                ScrollPaneEnums.HBarPos.class,
                "-vfx-hbar-pos",
                VFXScrollPane::hBarPosProperty,
                ScrollPaneEnums.HBarPos.BOTTOM
            );

        private static final CssMetaData<VFXScrollPane, Number> SCROLL_BARS_GAP =
            FACTORY.createSizeCssMetaData(
                "-vfx-bars-gap",
                VFXScrollPane::scrollBarsGapProperty,
                4.0
            );

        private static final CssMetaData<VFXScrollPane, Boolean> AUTO_HIDE_BARS =
            FACTORY.createBooleanCssMetaData(
                "-vfx-auto-hide-bars",
                VFXScrollPane::autoHideBarsProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, ScrollBarPolicy> VBAR_POLICY =
            FACTORY.createEnumCssMetaData(
                ScrollBarPolicy.class,
                "-vfx-vbar-policy",
                VFXScrollPane::vBarPolicyProperty,
                ScrollBarPolicy.DEFAULT
            );

        private static final CssMetaData<VFXScrollPane, ScrollBarPolicy> HBAR_POLICY =
            FACTORY.createEnumCssMetaData(
                ScrollBarPolicy.class,
                "-vfx-hbar-policy",
                VFXScrollPane::hBarPolicyProperty,
                ScrollBarPolicy.DEFAULT
            );

        private static final CssMetaData<VFXScrollPane, Number> V_TRACK_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-vtrack-increment",
                VFXScrollPane::vTrackIncrementProperty,
                0.1
            );

        private static final CssMetaData<VFXScrollPane, Number> V_UNIT_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-vunit-increment",
                VFXScrollPane::vUnitIncrementProperty,
                0.01
            );

        private static final CssMetaData<VFXScrollPane, Number> H_TRACK_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-htrack-increment",
                VFXScrollPane::hTrackIncrementProperty,
                0.1
            );

        private static final CssMetaData<VFXScrollPane, Number> H_UNIT_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-hunit-increment",
                VFXScrollPane::hUnitIncrementProperty,
                0.01
            );

        private static final CssMetaData<VFXScrollPane, Boolean> SHOW_BUTTONS =
            FACTORY.createBooleanCssMetaData(
                "-vfx-show-buttons",
                VFXScrollPane::showButtonsProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, Number> BUTTONS_GAP =
            FACTORY.createSizeCssMetaData(
                "-vfx-buttons-gap",
                VFXScrollPane::buttonsGapProperty,
                4.0
            );

        private static final CssMetaData<VFXScrollPane, Boolean> SMOOTH_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-smooth-scroll",
                VFXScrollPane::smoothScrollProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, Boolean> TRACK_SMOOTH_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-track-smooth-scroll",
                VFXScrollPane::trackSmoothScrollProperty,
                false
            );


        private static final CssMetaData<VFXScrollPane, Boolean> DRAG_TO_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-drag-to-scroll",
                VFXScrollPane::dragToScrollProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, Boolean> DRAG_SMOOTH_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-drag-smooth-scroll",
                VFXScrollPane::dragSmoothScrollProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, Number> CLIP_BORDER_RADIUS =
            FACTORY.createSizeCssMetaData(
                "-vfx-clip-border-radius",
                VFXScrollPane::clipBorderRadiusProperty,
                0.0
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                Control.getClassCssMetaData(),
                LAYOUT_MODE, ALIGNMENT, MAIN_AXIS,
                FIT_TO_WIDTH, FIT_TO_HEIGHT,
                VBAR_POS, HBAR_POS, SCROLL_BARS_GAP,
                AUTO_HIDE_BARS, VBAR_POLICY, HBAR_POLICY,
                V_TRACK_INCREMENT, V_UNIT_INCREMENT, H_TRACK_INCREMENT, H_UNIT_INCREMENT,
                SHOW_BUTTONS, BUTTONS_GAP,
                SMOOTH_SCROLL, TRACK_SMOOTH_SCROLL,
                DRAG_TO_SCROLL, DRAG_SMOOTH_SCROLL,
                CLIP_BORDER_RADIUS
            );
        }
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.cssMetaDataList;
    }

    @Override
    protected List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
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

    public double getVValue() {
        return vValue.get();
    }

    /**
     * Specifies the vertical scroll bar's value.
     */
    public DoubleProperty vValueProperty() {
        return vValue;
    }

    public void setVValue(double vValue) {
        this.vValue.set(vValue);
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

    public double getHValue() {
        return hValue.get();
    }

    /**
     * Specifies the horizontal scroll bar's value.
     */
    public DoubleProperty hValueProperty() {
        return hValue;
    }

    public void setHValue(double hValue) {
        this.hValue.set(hValue);
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

    public Function<VFXScrollBar, VFXScrollBarBehavior> getVBarBehavior() {
        return vBarBehavior.get();
    }

    /**
     * Specifies the function used to build the vertical scroll bar's behavior.
     */
    public FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> vBarBehaviorProperty() {
        return vBarBehavior;
    }

    public void setVBarBehavior(Function<VFXScrollBar, VFXScrollBarBehavior> vBarBehavior) {
        this.vBarBehavior.set(vBarBehavior);
    }

    public Function<VFXScrollBar, VFXScrollBarBehavior> getHBarBehavior() {
        return hBarBehavior.get();
    }

    /**
     * Specifies the function used to build the horizontal scroll bar's behavior.
     */
    public FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> hBarBehaviorProperty() {
        return hBarBehavior;
    }

    public void setHBarBehavior(Function<VFXScrollBar, VFXScrollBarBehavior> hBarBehavior) {
        this.hBarBehavior.set(hBarBehavior);
    }
}
