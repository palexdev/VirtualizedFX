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

import java.util.List;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils.ScrollDirection;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollBarSkin;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;

/**
 * My personal custom implementation of a scroll bar from scratch, follows the MVC pattern as enforced by {@link Control}.
 * The default skin is {@link VFXScrollBarSkin}. The default behavior is {@link VFXScrollBarBehavior}. Also implements {@link VFXStyleable}.
 * <p></p>
 * In addition to an appealing style, the component offers many new features compared to the boring standard
 * JavaFX' scroll bar, such as:
 * <p> - standard or slim appearance (depends on the stylesheet!), {@link #layoutModeProperty()}
 * <p> - the possibility of showing/hiding the increase and decrease buttons, {@link #showButtonsProperty()}
 * <p> - the possibility of controlling the gap between the buttons and the thumb, {@link #buttonsGapProperty()}
 * <p> - inbuilt smooth scroll both for the thumb and the track, {@link #smoothScrollProperty()}, {@link #trackSmoothScrollProperty()}
 * <p> - the possibility of querying the scroll direction, {@link #scrollDirectionProperty()}
 * <p></p>
 * Three {@link PseudoClass} worth mentioning:
 * <p> - ":compact": active when the {@link #layoutModeProperty()} is set to {@link LayoutMode#COMPACT}
 * <p> - ":buttons": active when the increment and decrement buttons are visible, {@link #showButtonsProperty()}
 * <p> - ":dragging": active when the thumb is being dragged
 * <p> - ":horizontal": active when the scroll bar's orientation is HORIZONTAL
 * <p> - ":vertical": active when the scroll bar's orientation is VERTICAL
 * <p></p>
 * For the sake of simplicity and abstraction, the bar's values are expressed as percentages. The minimum and maximum values
 * can be set by using {@link #setMin(double)} and {@link #setMax(double)} but cannot go below {@code 0.0}
 * and above {@code 1.0} respectively. The {@link #valueProperty()} is automatically clamped between the min and max values.
 */
public class VFXScrollBar extends Control<VFXScrollBarBehavior> implements VFXStyleable {
    //================================================================================
    // Static Properties
    //================================================================================
    public static final PseudoClass DRAGGING_PSEUDO_CLASS = PseudoClass.getPseudoClass("dragging");
    public static final PseudoClass VERTICAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("vertical");
    public static final PseudoClass HORIZONTAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("horizontal");
    public static final PseudoClass BUTTONS_PSEUDO_CLASS = PseudoClass.getPseudoClass("buttons");

    //================================================================================
    // Properties
    //================================================================================
    private final DoubleProperty min = PropUtils.clampedDoubleProperty(
        () -> 0.0,
        this::getMax
    );
    private final DoubleProperty value = PropUtils.clampedDoubleProperty(
        this::getMin,
        this::getMax
    );
    private final DoubleProperty max = PropUtils.clampedDoubleProperty(
        this::getMin,
        () -> 1.0
    );
    private final DoubleProperty visibleAmount = PropUtils.clampedDoubleProperty(
        () -> 0.0,
        () -> 1.0
    );

    private final ObjectProperty<ScrollDirection> scrollDirection = new SimpleObjectProperty<>();

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollBar() {
        this(Orientation.VERTICAL);
    }

    public VFXScrollBar(Orientation orientation) {
        setOrientation(orientation);
        init();
    }

    //================================================================================
    // Methods
    //================================================================================
    private void init() {
        setDefaultStyleClasses();
        getStylesheets().add(VFXResources.loadResource("VFXScrollBar.css"));
        setDefaultBehaviorProvider();

        setMin(0.0);
        setMax(1.0);
    }

    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXScrollBarSkin(this);
    }

    @Override
    public Supplier<VFXScrollBarBehavior> defaultBehaviorProvider() {
        return () -> new VFXScrollBarBehavior(this);
    }

    @Override
    public List<String> defaultStyleClasses() {
        return List.of("vfx-scroll-bar");
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

    private final StyleableObjectProperty<Orientation> orientation = new StyleableObjectProperty<>(
        StyleableProperties.ORIENTATION,
        this,
        "orientation",
        Orientation.VERTICAL
    );

    private final StyleableBooleanProperty showButtons = new StyleableBooleanProperty(
        StyleableProperties.SHOW_BUTTONS,
        this,
        "showButtons",
        false
    ) {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(BUTTONS_PSEUDO_CLASS, get());
        }
    };

    private final StyleableDoubleProperty buttonsGap = new StyleableDoubleProperty(
        StyleableProperties.BUTTONS_GAP,
        this,
        "buttonsGap",
        4.0
    );

    private final StyleableDoubleProperty trackIncrement = new StyleableDoubleProperty(
        StyleableProperties.TRACK_INCREMENT,
        this,
        "trackIncrement",
        0.1
    );

    private final StyleableDoubleProperty unitIncrement = new StyleableDoubleProperty(
        StyleableProperties.UNIT_INCREMENT,
        this,
        "unitIncrement",
        0.01
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

    public LayoutMode getLayoutMode() {
        return layoutMode.get();
    }

    /**
     * Specifies the scroll bar's appearance. Setting this to {@link LayoutMode#COMPACT} will activate the extra PseudoClass
     * ':compact' on the control.
     * <p>
     * Note however that this depends entirely on the stylesheet used.
     * <p>
     * This is also settable via CSS with the "-vfx-layout-mode" property.
     */
    public StyleableObjectProperty<LayoutMode> layoutModeProperty() {
        return layoutMode;
    }

    public void setLayoutMode(LayoutMode layoutMode) {
        this.layoutMode.set(layoutMode);
    }

    public Orientation getOrientation() {
        return orientation.get();
    }

    /**
     * Specifies the scroll bar's orientation.
     * <p>
     * This is also settable via CSS with the "-vfx-orientation" property.
     */
    public StyleableObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation.set(orientation);
    }

    public boolean isShowButtons() {
        return showButtons.get();
    }

    /**
     * Specifies whether the increase/decrease buttons are visible.
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
     * Specifies the gap between the increase/decrease buttons and the track.
     * <p>
     * This is also settable via CSS with the "-vfx-buttons-gap" property.
     */
    public StyleableDoubleProperty buttonsGapProperty() {
        return buttonsGap;
    }

    public void setButtonsGap(double buttonsGap) {
        this.buttonsGap.set(buttonsGap);
    }

    public double getTrackIncrement() {
        return trackIncrement.get();
    }

    /**
     * Specifies the amount added/subtracted to the {@link #valueProperty()} used by the
     * scroll bar's track.
     * <p>
     * This is also settable via CSS with the "-vfx-track-increment" property.
     */
    public StyleableDoubleProperty trackIncrementProperty() {
        return trackIncrement;
    }

    public void setTrackIncrement(double trackIncrement) {
        this.trackIncrement.set(trackIncrement);
    }

    public double getUnitIncrement() {
        return unitIncrement.get();
    }

    /**
     * Specifies the amount added/subtracted to the {@link #valueProperty()} used by the
     * increment/decrement buttons and by scrolling.
     * <p>
     * This is also settable via CSS with the "-vfx-unit-increment" property.
     */
    public StyleableDoubleProperty unitIncrementProperty() {
        return unitIncrement;
    }

    public void setUnitIncrement(double unitIncrement) {
        this.unitIncrement.set(unitIncrement);
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

    //================================================================================
    // CssMetaData
    //================================================================================
    private static class StyleableProperties {
        private static final StyleablePropertyFactory<VFXScrollBar> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
        private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

        private static final CssMetaData<VFXScrollBar, LayoutMode> LAYOUT_MODE =
            FACTORY.createEnumCssMetaData(
                LayoutMode.class,
                "-vfx-layout-mode",
                VFXScrollBar::layoutModeProperty,
                LayoutMode.DEFAULT
            );

        private static final CssMetaData<VFXScrollBar, Orientation> ORIENTATION =
            FACTORY.createEnumCssMetaData(
                Orientation.class,
                "-vfx-orientation",
                VFXScrollBar::orientationProperty,
                Orientation.VERTICAL
            );

        private static final CssMetaData<VFXScrollBar, Boolean> SHOW_BUTTONS =
            FACTORY.createBooleanCssMetaData(
                "-vfx-show-buttons",
                VFXScrollBar::showButtonsProperty,
                false
            );

        private static final CssMetaData<VFXScrollBar, Number> BUTTONS_GAP =
            FACTORY.createSizeCssMetaData(
                "-vfx-buttons-gap",
                VFXScrollBar::buttonsGapProperty,
                4.0
            );

        private static final CssMetaData<VFXScrollBar, Number> TRACK_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-track-increment",
                VFXScrollBar::trackIncrementProperty,
                0.1
            );

        private static final CssMetaData<VFXScrollBar, Number> UNIT_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-unit-increment",
                VFXScrollBar::unitIncrementProperty,
                0.01
            );

        private static final CssMetaData<VFXScrollBar, Boolean> SMOOTH_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-smooth-scroll",
                VFXScrollBar::smoothScrollProperty,
                false
            );

        private static final CssMetaData<VFXScrollBar, Boolean> TRACK_SMOOTH_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-track-smooth-scroll",
                VFXScrollBar::trackSmoothScrollProperty,
                false
            );

        static {
            cssMetaDataList = StyleUtils.cssMetaDataList(
                Control.getClassCssMetaData(),
                LAYOUT_MODE, ORIENTATION,
                SHOW_BUTTONS, BUTTONS_GAP,
                TRACK_INCREMENT, UNIT_INCREMENT,
                SMOOTH_SCROLL, TRACK_SMOOTH_SCROLL
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
    public double getMin() {
        return min.get();
    }

    /**
     * Specifies the minimum possible value for {@link #valueProperty()}.
     */
    public DoubleProperty minProperty() {
        return min;
    }

    public void setMin(double min) {
        this.min.set(min);
    }

    public double getValue() {
        return value.get();
    }

    /**
     * Specifies the scroll value, clamped between {@link #minProperty()} and {@link #maxProperty()}.
     */
    public DoubleProperty valueProperty() {
        return value;
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public double getMax() {
        return max.get();
    }

    /**
     * Specifies the maximum possible value for {@link #valueProperty()}.
     */
    public DoubleProperty maxProperty() {
        return max;
    }

    public void setMax(double max) {
        this.max.set(max);
    }

    public double getVisibleAmount() {
        return visibleAmount.get();
    }

    /**
     * Specifies how much content is visible. Depends on the components using the scroll bar and determines the length
     * of the scroll bar's thumb relative to the track.
     * <p>
     * Not setting this property does not compromise the component's functionality but as a consequence the bar does not
     * indicate clearly to the user how much content is hidden.
     */
    public DoubleProperty visibleAmountProperty() {
        return visibleAmount;
    }

    public void setVisibleAmount(double visibleAmount) {
        this.visibleAmount.set(visibleAmount);
    }

    public ScrollDirection getScrollDirection() {
        return scrollDirection.get();
    }

    /**
     * Specifies the current scroll direction. The default behavior implementation manages this.
     *
     * @see VFXScrollBarBehavior
     */
    public ReadOnlyObjectProperty<ScrollDirection> scrollDirectionProperty() {
        return scrollDirection;
    }
}
