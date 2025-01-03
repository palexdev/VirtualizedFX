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

package io.github.palexdev.virtualizedfx.controls;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.base.properties.functional.FunctionProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollPaneBehavior;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollPaneSkin;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.HBarPos;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.VBarPos;
import io.github.palexdev.virtualizedfx.utils.ScrollBounds;
import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;

/**
 * My personal custom implementation of a scroll pane from scratch, follows the MVC pattern as enforced by {@link Control}.
 * The default skin is {@link VFXScrollPaneSkin}. The default behavior is {@link VFXScrollPaneBehavior}. Also implements {@link VFXStyleable}.
 * <p></p>
 * <b>Note:</b> to make this component as generic as possible, it requires both the content and the view area bounds.
 * However, to make it easier to use, it tries to determine the bounds automatically whenever the content changes,
 * see {@link #onContentChanged()}.
 * <p>
 * Apart from this peculiarity, this is a normal scroll pane, <b>BUT</b>, since it's made from scratch it differs in many aspects from the JavaFX's one.
 * <p></p>
 * Since this uses the new {@link VFXScrollBar}s, this also expects values to be in pixels and not percentages.
 * <p>
 * Listing all the features of this scroll pane we have:
 * <p> - The possibility to change the layout strategy for the scroll bars, see {@link LayoutMode} and {@link VFXScrollPaneSkin}
 * <p> - The possibility of auto hide the scroll bars after a certain amount of time
 * <p> - The scroll bars policy, which <b>differs</b> from the JavaFX's one
 * <p> - The possibility to change the position of the scroll bars, see {@link HBarPos} and {@link VBarPos}
 * <p> - The possibility to specify extra padding for the scroll bars
 * <p> - The possibility to scroll with the mouse (not called pannable anymore but dragToScroll, {@link #dragToScrollProperty()})
 * <p> - Ports all the new features of {@link VFXScrollBar} which they'll be bound to, such as:
 * {@link VFXScrollBar#showButtonsProperty()}, {@link VFXScrollBar#buttonsGapProperty()},
 * {@link VFXScrollBar#trackIncrementProperty()}, {@link VFXScrollBar#unitIncrementProperty()},
 * {@link VFXScrollBar#smoothScrollProperty()} and {@link VFXScrollBar#trackSmoothScrollProperty()}.
 * <p></p>
 * This also offers two new PseudoClasses:
 * <p> - ":compact": active when the {@link #layoutModeProperty()} is set to {@link LayoutMode#COMPACT}
 * <p> - ":drag-to-scroll": active when the {@link #dragToScrollProperty()} is set to true
 * <p></p>
 * Removed features from the JavaFX's one are: fitToHeight and fitToWidth, the possibility to specify the viewport's bounds.
 * <p></p>
 * Last but not least, since this uses the new {@link VFXScrollBar}s, it also allows to change their behavior with
 * {@link #hBarBehaviorProperty()} and {@link #vBarBehaviorProperty()}.
 */
// TODO check fitTo features
public class VFXScrollPane extends Control<VFXScrollPaneBehavior> implements VFXStyleable {
    //================================================================================
    // PseudoClasses
    //================================================================================
    public static final PseudoClass COMPACT_MODE_PSEUDO_CLASS = PseudoClass.getPseudoClass("compact");
    public static final PseudoClass DRAG_TO_SCROLL_PSEUDO_CLASS = PseudoClass.getPseudoClass("drag-to-scroll");

    //================================================================================
    // Properties
    //================================================================================
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>() {
        @Override
        protected void invalidated() {
            onContentChanged();
        }
    };
    private final ObjectProperty<ScrollBounds> contentBounds = new SimpleObjectProperty<>(ScrollBounds.ZERO);
    private final SizeProperty viewportSize = new SizeProperty(Size.empty());

    //================================================================================
    // ScrollBars Properties & Config
    //================================================================================
    private final DoubleProperty hMin = new SimpleDoubleProperty(0.0);
    private final DoubleProperty hVal = new SimpleDoubleProperty(0.0) {
        @Override
        public void set(double newValue) {
            super.set(NumberUtils.clamp(
                newValue,
                Math.max(0.0, getHMin()),
                Math.min(getMaxScroll(Orientation.HORIZONTAL), getHMax())
            ));
        }
    };
    private final DoubleProperty hMax = new SimpleDoubleProperty(Double.MAX_VALUE) {
        @Override
        protected void invalidated() {
            invalidateValue(Orientation.HORIZONTAL);
        }
    };

    private final DoubleProperty vMin = new SimpleDoubleProperty(0.0);
    private final DoubleProperty vVal = new SimpleDoubleProperty(0.0) {
        @Override
        public void set(double newValue) {
            super.set(NumberUtils.clamp(
                newValue,
                Math.max(0.0, getVMin()),
                Math.min(getMaxScroll(Orientation.VERTICAL), getVMax())
            ));
        }
    };
    private final DoubleProperty vMax = new SimpleDoubleProperty(Double.MAX_VALUE) {
        @Override
        protected void invalidated() {
            invalidateValue(Orientation.VERTICAL);
        }
    };

    private final ObjectProperty<Orientation> orientation = new SimpleObjectProperty<>(Orientation.VERTICAL);
    private final FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> hBarBehavior = PropUtils.function(VFXScrollBarBehavior::new);
    private final FunctionProperty<VFXScrollBar, VFXScrollBarBehavior> vBarBehavior = PropUtils.function(VFXScrollBarBehavior::new);

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollPane() {
        this(null);
    }

    public VFXScrollPane(Node content) {
        setContent(content);
        initialize();
    }

    //================================================================================
    // Methods
    //================================================================================
    private void initialize() {
        getStyleClass().setAll(defaultStyleClasses());
        setDefaultBehaviorProvider();
        getStylesheets().add(VFXResources.loadResource("VFXScrollPane.css"));
    }

    /**
     * Binds bidirectionally both the {@link #hValProperty()} and {@link #vValProperty()} to the properties of the given
     * {@link VFXContainer}: {@link VFXContainer#hPosProperty()} and {@link VFXContainer#vPosProperty()}.
     */
    public VFXScrollPane bindTo(VFXContainer<?> container) {
        container.hPosProperty().bindBidirectional(hValProperty());
        container.vPosProperty().bindBidirectional(vValProperty());
        return this;
    }

    /**
     * This is responsible for updating the {@link #contentBoundsProperty()} when the {@link #contentProperty()} changes.
     * To be precise the property is bound depending on the type of the content.
     * <p>
     * If the content implements {@link VFXContainer} then it uses both {@link VFXContainer#getVirtualMaxX()} and {@link VFXContainer#getVirtualMaxY()}
     * as the content bounds and {@link #viewportSizeProperty()} as the view area size.
     * <p></p>
     * If the content is just a generic node, then it uses {@link Node#layoutBoundsProperty()} as the content bounds and
     * again the {@link #viewportSizeProperty()} as the view area size.
     */
    protected void onContentChanged() {
        Node content = getContent();
        if (content == null) {
            contentBounds.unbind();
            setContentBounds(ScrollBounds.ZERO);
            return;
        }
        if (content instanceof VFXContainer<?> c) {
            contentBounds.bind(ObjectBindingBuilder.<ScrollBounds>build()
                .setMapper(() -> new ScrollBounds(
                    c.getVirtualMaxX(), c.getVirtualMaxY(),
                    viewportSize.getWidth(), viewportSize.getHeight()
                ))
                .addSources(c.virtualMaxXProperty(), c.virtualMaxYProperty())
                .addSources(viewportSize)
                .get()
            );
            return;
        }
        contentBounds.bind(ObjectBindingBuilder.<ScrollBounds>build()
            .setMapper(() -> new ScrollBounds(
                snapSizeX(content.prefWidth(-1)), snapSizeY(content.prefHeight(-1)),
                viewportSize.getWidth(), viewportSize.getHeight()
            ))
            .addSources(content.layoutBoundsProperty())
            .addSources(viewportSize)
            .get()
        );
    }

    /**
     * Shortcut for {@code setValue(getValue())}. This will cause the {@link #hValProperty()} or the {@link #vValProperty()}
     * (depending on the given orientation) to be withing the bounds specified by the respective min and max properties.
     */
    protected void invalidateValue(Orientation o) {
        if (o == Orientation.VERTICAL) {
            setVVal(getVVal());
        } else {
            setHVal(getHVal());
        }
    }

    //================================================================================
    // Delegate Methods
    //================================================================================

    /**
     * Delegate for {@link ScrollBounds#visibleAmount(Orientation)}
     *
     * @see ScrollBounds
     */
    public double getVisibleAmount(Orientation o) {
        return getContentBounds().visibleAmount(o);
    }

    /**
     * Delegate for {@link ScrollBounds#maxScroll(Orientation)}
     *
     * @see ScrollBounds
     */
    public double getMaxScroll(Orientation o) {
        return getContentBounds().maxScroll(o);
    }


    //================================================================================
    // Overridden Methods
    //================================================================================
    @Override
    public Supplier<VFXScrollPaneBehavior> defaultBehaviorProvider() {
        return () -> new VFXScrollPaneBehavior(this);
    }

    @Override
    protected SkinBase<?, ?> buildSkin() {
        return new VFXScrollPaneSkin(this);
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

    private final StyleableObjectProperty<ScrollPaneEnums.HBarPos> hBarPos = new StyleableObjectProperty<>(
        StyleableProperties.HBAR_POS,
        this,
        "hBarPos",
        ScrollPaneEnums.HBarPos.BOTTOM
    );

    private final StyleableObjectProperty<ScrollPaneEnums.VBarPos> vBarPos = new StyleableObjectProperty<>(
        StyleableProperties.VBAR_POS,
        this,
        "vBarPos",
        ScrollPaneEnums.VBarPos.RIGHT
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

    private final StyleableDoubleProperty hBarOffset = new StyleableDoubleProperty(
        StyleableProperties.HBAR_OFFSET,
        this,
        "hBarOffset",
        0.0
    );

    private final StyleableDoubleProperty vBarOffset = new StyleableDoubleProperty(
        StyleableProperties.VBAR_OFFSET,
        this,
        "vBarOffset",
        0.0
    );

    private final StyleableBooleanProperty dragToScroll = new StyleableBooleanProperty(
        StyleableProperties.DRAG_TO_SCROLL,
        this,
        "dragToScroll",
        false
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
        1.0
    );

    private final StyleableDoubleProperty hTrackIncrement = new StyleableDoubleProperty(
        StyleableProperties.H_TRACK_INCREMENT,
        this,
        "hTrackIncrement",
        25.0
    );

    private final StyleableDoubleProperty hUnitIncrement = new StyleableDoubleProperty(
        StyleableProperties.H_UNIT_INCREMENT,
        this,
        "hUnitIncrement",
        10.0
    );

    private final StyleableDoubleProperty vTrackIncrement = new StyleableDoubleProperty(
        StyleableProperties.V_TRACK_INCREMENT,
        this,
        "vTrackIncrement",
        25.0
    );

    private final StyleableDoubleProperty vUnitIncrement = new StyleableDoubleProperty(
        StyleableProperties.V_UNIT_INCREMENT,
        this,
        "vUnitIncrement",
        10.0
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

    /**
     * Specifies the layout strategy for the scroll bars, see {@link LayoutMode} or {@link VFXScrollPaneSkin}
     * for an explanation.
     * <p>
     * This is also settable via CSS with the "-vfx-layout-mode" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-autohide-bars" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-policy" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-policy" property.
     */
    public StyleableObjectProperty<ScrollBarPolicy> vBarPolicyProperty() {
        return vBarPolicy;
    }

    public void setVBarPolicy(ScrollBarPolicy vBarPolicy) {
        this.vBarPolicy.set(vBarPolicy);
    }

    public ScrollPaneEnums.HBarPos getHBarPos() {
        return hBarPos.get();
    }

    /**
     * Specifies the position of the horizontal scroll bar.
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-pos" property.
     */
    public StyleableObjectProperty<ScrollPaneEnums.HBarPos> hBarPosProperty() {
        return hBarPos;
    }

    public void setHBarPos(ScrollPaneEnums.HBarPos hBarPos) {
        this.hBarPos.set(hBarPos);
    }

    public ScrollPaneEnums.VBarPos getVBarPos() {
        return vBarPos.get();
    }

    /**
     * Specifies the position of the vertical scroll bar.
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-pos" property.
     */
    public StyleableObjectProperty<ScrollPaneEnums.VBarPos> vBarPosProperty() {
        return vBarPos;
    }

    public void setVBarPos(ScrollPaneEnums.VBarPos vBarPos) {
        this.vBarPos.set(vBarPos);
    }

    public Insets getHBarPadding() {
        return hBarPadding.get();
    }

    /**
     * Specifies the extra padding for the horizontal scroll bar.
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-padding" property.
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
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-padding" property.
     */
    public StyleableObjectProperty<Insets> vBarPaddingProperty() {
        return vBarPadding;
    }

    public void setVBarPadding(Insets vBarPadding) {
        this.vBarPadding.set(vBarPadding);
    }

    public double getHBarOffset() {
        return hBarOffset.get();
    }

    /**
     * Specifies a value by which the horizontal scroll bar will be "shifted" on the x-axis.
     * <p>
     * Note that as a consequence, this will also reduce the width of the bar.
     * <p>
     * This can be useful if wrapping content which has headers or extra nodes of some sort and the
     * bar ends up covering them.
     * <p>
     * This is also settable via CSS with the "-vfx-hbar-offset" property.
     */
    public StyleableDoubleProperty hBarOffsetProperty() {
        return hBarOffset;
    }

    public void setHBarOffset(double hBarOffset) {
        this.hBarOffset.set(hBarOffset);
    }

    public double getVBarOffset() {
        return vBarOffset.get();
    }

    /**
     * Specifies a value by which the vertical scroll bar will be "shifted" on the y-axis.
     * <p>
     * Note that as a consequence, this will also reduce the height of the bar.
     * <p>
     * This can be useful if wrapping content which has headers or extra nodes of some sort and the
     * bar ends up covering them.
     * <p>
     * This is also settable via CSS with the "-vfx-vbar-offset" property.
     */
    public StyleableDoubleProperty vBarOffsetProperty() {
        return vBarOffset;
    }

    public void setVBarOffset(double vBarOffset) {
        this.vBarOffset.set(vBarOffset);
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

    public boolean isDragSmoothScroll() {
        return dragSmoothScroll.get();
    }

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
     * This is useful when you want to make a rounded scroll pane, this
     * prevents the content from going outside the view.
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

        private static final CssMetaData<VFXScrollPane, Boolean> AUTO_HIDE_BARS =
            FACTORY.createBooleanCssMetaData(
                "-vfx-autohide-bars",
                VFXScrollPane::autoHideBarsProperty,
                false
            );

        private static final CssMetaData<VFXScrollPane, ScrollBarPolicy> HBAR_POLICY =
            FACTORY.createEnumCssMetaData(
                ScrollBarPolicy.class,
                "-vfx-hbar-policy",
                VFXScrollPane::hBarPolicyProperty,
                ScrollBarPolicy.DEFAULT
            );

        private static final CssMetaData<VFXScrollPane, ScrollBarPolicy> VBAR_POLICY =
            FACTORY.createEnumCssMetaData(
                ScrollBarPolicy.class,
                "-vfx-vbar-policy",
                VFXScrollPane::vBarPolicyProperty,
                ScrollBarPolicy.DEFAULT
            );

        private static final CssMetaData<VFXScrollPane, HBarPos> HBAR_POS =
            FACTORY.createEnumCssMetaData(
                ScrollPaneEnums.HBarPos.class,
                "-vfx-hbar-pos",
                VFXScrollPane::hBarPosProperty,
                ScrollPaneEnums.HBarPos.BOTTOM
            );

        private static final CssMetaData<VFXScrollPane, VBarPos> VBAR_POS =
            FACTORY.createEnumCssMetaData(
                ScrollPaneEnums.VBarPos.class,
                "-vfx-vbar-pos",
                VFXScrollPane::vBarPosProperty,
                ScrollPaneEnums.VBarPos.RIGHT
            );

        private static final CssMetaData<VFXScrollPane, Insets> HBAR_PADDING =
            FACTORY.createInsetsCssMetaData(
                "-vfx-hbar-padding",
                VFXScrollPane::hBarPaddingProperty,
                Insets.EMPTY
            );

        private static final CssMetaData<VFXScrollPane, Insets> VBAR_PADDING =
            FACTORY.createInsetsCssMetaData(
                "-vfx-vbar-padding",
                VFXScrollPane::vBarPaddingProperty,
                Insets.EMPTY
            );

        private static final CssMetaData<VFXScrollPane, Number> HBAR_OFFSET =
            FACTORY.createSizeCssMetaData(
                "-vfx-hbar-offset",
                VFXScrollPane::hBarOffsetProperty,
                0.0
            );

        private static final CssMetaData<VFXScrollPane, Number> VBAR_OFFSET =
            FACTORY.createSizeCssMetaData(
                "-vfx-vbar-offset",
                VFXScrollPane::vBarOffsetProperty,
                0.0
            );

        private static final CssMetaData<VFXScrollPane, Boolean> DRAG_TO_SCROLL =
            FACTORY.createBooleanCssMetaData(
                "-vfx-drag-to-scroll",
                VFXScrollPane::dragToScrollProperty,
                false
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
                1.0
            );

        private static final CssMetaData<VFXScrollPane, Number> H_TRACK_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-htrack-increment",
                VFXScrollPane::hTrackIncrementProperty,
                25.0
            );

        private static final CssMetaData<VFXScrollPane, Number> H_UNIT_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-hunit-increment",
                VFXScrollPane::hUnitIncrementProperty,
                10.0
            );

        private static final CssMetaData<VFXScrollPane, Number> V_TRACK_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-vtrack-increment",
                VFXScrollPane::vTrackIncrementProperty,
                25.0
            );

        private static final CssMetaData<VFXScrollPane, Number> V_UNIT_INCREMENT =
            FACTORY.createSizeCssMetaData(
                "-vfx-vunit-increment",
                VFXScrollPane::vUnitIncrementProperty,
                10.0
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
                LAYOUT_MODE, HBAR_POLICY, VBAR_POLICY, HBAR_POS, VBAR_POS,
                HBAR_PADDING, VBAR_PADDING, HBAR_OFFSET, VBAR_OFFSET,
                AUTO_HIDE_BARS, DRAG_TO_SCROLL,
                SHOW_BUTTONS, BUTTONS_GAP,
                H_TRACK_INCREMENT, H_UNIT_INCREMENT, V_TRACK_INCREMENT, V_UNIT_INCREMENT,
                SMOOTH_SCROLL, TRACK_SMOOTH_SCROLL, DRAG_SMOOTH_SCROLL,
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

    public ScrollBounds getContentBounds() {
        return contentBounds.get();
    }

    /**
     * Specifies the content's bounds; this information is crucial for the scroll pane to work properly.
     * <p>
     * The property is bound automatically by the {@link #onContentChanged()} method, you need to override it if you need
     * a custom binding.
     */
    public ObjectProperty<ScrollBounds> contentBoundsProperty() {
        return contentBounds;
    }

    public void setContentBounds(ScrollBounds contentBounds) {
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

    /**
     * Increments the {@link #hValProperty()} by the amount specified by the {@link #hUnitIncrementProperty()}.
     */
    public void huIncrement() {
        setHVal(getHVal() + getHUnitIncrement());
    }

    /**
     * Decrements the {@link #hValProperty()} by the amount specified by the {@link #hUnitIncrementProperty()}.
     */
    public void huDecrement() {
        setHVal(getHVal() - getHUnitIncrement());
    }

    /**
     * Increments the {@link #hValProperty()} by the amount specified by the {@link #hTrackIncrementProperty()}.
     */
    public void htIncrement() {
        setHVal(getHVal() + getHTrackIncrement());
    }

    /**
     * Decrements the {@link #hValProperty()} by the amount specified by the {@link #hTrackIncrementProperty()}.
     */
    public void htDecrement() {
        setHVal(getHVal() - getHTrackIncrement());
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

    /**
     * Increments the {@link #vValProperty()} by the amount specified by the {@link #vUnitIncrementProperty()}.
     */
    public void vuIncrement() {
        setVVal(getVVal() + getVUnitIncrement());
    }

    /**
     * Decrements the {@link #vValProperty()} by the amount specified by the {@link #vUnitIncrementProperty()}.
     */
    public void vuDecrement() {
        setVVal(getVVal() - getVUnitIncrement());
    }

    /**
     * Increments the {@link #vValProperty()} by the amount specified by the {@link #vTrackIncrementProperty()}.
     */
    public void vtIncrement() {
        setVVal(getVVal() + getVTrackIncrement());
    }

    /**
     * Decrements the {@link #vValProperty()} by the amount specified by the {@link #vTrackIncrementProperty()}.
     */
    public void vtDecrement() {
        setHVal(getVVal() - getVTrackIncrement());
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

    public Size getViewportSize() {
        return viewportSize.get();
    }

    /**
     * Specifies the scroll pane's view area size, which may or may not include the scroll bars' size.
     * Depends on the skin implementation.
     *
     * @see VFXScrollPaneSkin
     */
    public ReadOnlyObjectProperty<Size> viewportSizeProperty() {
        return viewportSize;
    }
}
