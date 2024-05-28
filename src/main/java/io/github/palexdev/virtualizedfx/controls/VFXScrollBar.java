package io.github.palexdev.virtualizedfx.controls;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.Control;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.ScrollUtils.ScrollDirection;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.VFXResources;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.base.VFXStyleable;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.skins.VFXScrollBarSkin;
import io.github.palexdev.virtualizedfx.utils.ScrollBounds;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;

import java.util.List;
import java.util.function.Supplier;

/**
 * My personal custom implementation of a scroll bar from scratch, follows the MVC pattern as enforced by {@link Control}.
 * The default skin is {@link VFXScrollBarSkin}. The default behavior is {@link VFXScrollBarBehavior}. Also implements {@link VFXStyleable}.
 * <p>
 * VirtualizedFX offers a default stylesheet for this control 'VFXScrollBar.css', but it's not added automatically.
 * You can load it by using {@link VFXResources#loadResource(String)}.
 * <p></p>
 * In addition to an appealing style, the component offers many new features compared to the boring standard
 * JavaFX' scroll bar, such as:
 * <p> - the possibility of showing/hiding the increase and decrease buttons, {@link #showButtonsProperty()}
 * <p> - the possibility of controlling the hap between the buttons and the thumb, {@link #buttonsGapProperty()}
 * <p> - inbuilt smooth scroll both for the thumb and the track, {@link #smoothScrollProperty()}, {@link #trackSmoothScrollProperty()}
 * <p> - the possibility of querying the scroll direction, {@link #scrollDirectionProperty()}
 * <p></p>
 * Three {@link PseudoClass} worth mentioning:
 * <p> - ":dragging": active when the thumb is being dragged
 * <p> - ":horizontal": active when the scroll bar's orientation is HORIZONTAL
 * <p> - ":vertical": active when the scroll bar's orientation is VERTICAL
 * <p></p>
 * <b>How VFXScrollBar is designed</b>
 * <p>
 * The following details are important to understand what to expect and how to correctly set up and use this component.
 * <p>
 * Just like the JavaFX's implementation, this component has three properties which determine the scroll value:
 * <p> - the {@link #minProperty()} can be used as the lower bound to limit the {@link #valueProperty()}
 * <p> - the {@link #maxProperty()} can be used as the upper bound to limit the {@link #valueProperty()}
 * <p> - the {@link #valueProperty()} specifies the scroll position
 * <p>
 * The main difference is that {@code VFXScrollBar} does not use percentage values but expects the actual number of pixels.
 * Also, for the same reason, there's another big difference here. This implementation expects you to specify both the
 * bounds of the content and the view area by setting the {@link #scrollBoundsProperty()}.
 * <p>
 * While it's true that this makes the component less 'modular', and it's debatable whether this is or it is not a
 * responsibility of the scroll bar; but it's also true that it makes the implementation much easier and straightforward.
 * <p>
 * <b>Examples</b>
 * <pre>
 * {@code
 * // Example with VFXContainer
 * VFXContainer<?> container = ...;
 * VFXScrollBar vBar = new VFXScrollBar();
 * vBar.bindTo(container, true); // Super easy
 * // Other options
 * // Same applied for hBar
 *
 *
 * // Example with generic Nodes
 * Node content = ...;
 * VBox box = new VBox(content);
 * // Limit the box to not grow infinitely
 * VFXScrollBar vBar = new VFXScrollBar();
 * vBar.scrollBoundsProperty().bind(Bindings.createObjectBinding(
 *     () -> new ScrollBounds(
 *             content.getLayoutBounds().getWidth(),
 *             content.getLayoutBounds().getHeight(),
 *             box.getWidth(),
 *             box.getHeight())
 * ), content.layoutBoundsProperty(), box.layoutBoundsProperty())
 * // Translate Y the content
 * // Other options
 * // Same applied for hBar
 * }
 * </pre>
 */
public class VFXScrollBar extends Control<VFXScrollBarBehavior> implements VFXStyleable {
	//================================================================================
	// PseudoClasses
	//================================================================================
	public static final PseudoClass DRAGGING_PSEUDO_CLASS = PseudoClass.getPseudoClass("dragging");
	public static final PseudoClass VERTICAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("vertical");
	public static final PseudoClass HORIZONTAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("horizontal");

	//================================================================================
	// Properties
	//================================================================================
	private final DoubleProperty min = new SimpleDoubleProperty(0.0) {
		@Override
		protected void invalidated() {
			invalidateValue();
		}
	};
	private final DoubleProperty value = new SimpleDoubleProperty() {
		@Override
		public void set(double newValue) {
			super.set(NumberUtils.clamp(
				newValue,
				Math.max(0.0, getMin()),
				Math.min(getMaxScroll(), getMax())
			));
		}
	};
	private final DoubleProperty max = new SimpleDoubleProperty(Double.MAX_VALUE) {
		@Override
		protected void invalidated() {
			invalidateValue();
		}
	};
	private final ObjectProperty<ScrollBounds> scrollBounds = new SimpleObjectProperty<>(ScrollBounds.ZERO) {
		@Override
		public void set(ScrollBounds newValue) {
			if (newValue == null) newValue = ScrollBounds.ZERO;
			super.set(newValue);
		}
	};
	private final ObjectProperty<ScrollDirection> scrollDirection = new SimpleObjectProperty<>();

	//================================================================================
	// Constructors
	//================================================================================
	public VFXScrollBar() {
		this(Orientation.VERTICAL);
	}

	public VFXScrollBar(Orientation orientation) {
		setOrientation(orientation);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().setAll(defaultStyleClasses());
		setDefaultBehaviorProvider();
	}

	/**
	 * Binds the {@link #scrollBoundsProperty()} to the given node implementing {@link VFXContainer} by using:
	 * {@link VFXContainer#virtualMaxXProperty()} and {@link VFXContainer#virtualMaxXProperty()} as the content bounds;
	 * {@link Bounds#getWidth()} and {@link Bounds#getHeight()} as the view bounds.
	 * <p>
	 * If the {@code bindPos} parameter is true then it also bidirectionally binds the properties:
	 * {@link #valueProperty()} and {@link VFXContainer#vPosProperty()} or {@link VFXContainer#hPosProperty()} depending
	 * on the {@link #orientationProperty()}.
	 *
	 * @param <N> any {@link Node} which implements {@link VFXContainer}
	 */
	public <N extends Node & VFXContainer<?>> VFXScrollBar bindTo(N container, boolean bindPos) {
		scrollBounds.bind(ObjectBindingBuilder.<ScrollBounds>build()
			.setMapper(() -> new ScrollBounds(
				container.getVirtualMaxX(), container.getVirtualMaxY(),
				container.getLayoutBounds().getWidth(), container.getLayoutBounds().getHeight()
			))
			.addSources(container.virtualMaxXProperty(), container.virtualMaxYProperty())
			.addSources(container.layoutBoundsProperty())
			.get()
		);
		if (bindPos) {
			DoubleProperty prop = (getOrientation() == Orientation.VERTICAL) ? container.vPosProperty() : container.hPosProperty();
			valueProperty().bindBidirectional(prop);
		}
		return this;
	}

	/**
	 * @return whether the thumb is being dragged
	 */
	public boolean isDragging() {
		return getPseudoClassStates().contains(DRAGGING_PSEUDO_CLASS);
	}

	/**
	 * Shortcut for {@code setValue(getValue())}. This will cause the {@link #valueProperty()} to be withing the bounds
	 * specified by the {@link #minProperty()} and the {@link #maxProperty()}.
	 */
	protected void invalidateValue() {
		setValue(getValue());
	}

	//================================================================================
	// Delegate Methods
	//================================================================================

	/**
	 * Delegate for {@link ScrollBounds#visibleAmount(Orientation)}
	 *
	 * @see ScrollBounds
	 */
	public double getVisibleAmount() {
		return getScrollBounds().visibleAmount(getOrientation());
	}

	/**
	 * Delegate for {@link ScrollBounds#maxScroll(Orientation)}
	 *
	 * @see ScrollBounds
	 */
	public double getMaxScroll() {
		return getScrollBounds().maxScroll(getOrientation());
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public Supplier<VFXScrollBarBehavior> defaultBehaviorProvider() {
		return () -> new VFXScrollBarBehavior(this);
	}

	@Override
	protected SkinBase<?, ?> buildSkin() {
		return new VFXScrollBarSkin(this);
	}

	@Override
	public List<String> defaultStyleClasses() {
		return List.of("vfx-scroll-bar");
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
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

	private final StyleableObjectProperty<Orientation> orientation = new StyleableObjectProperty<>(
		StyleableProperties.ORIENTATION,
		this,
		"orientation",
		Orientation.VERTICAL
	);

	private final StyleableDoubleProperty trackIncrement = new StyleableDoubleProperty(
		StyleableProperties.TRACK_INCREMENT,
		this,
		"trackIncrement",
		25.0
	);

	private final StyleableDoubleProperty unitIncrement = new StyleableDoubleProperty(
		StyleableProperties.UNIT_INCREMENT,
		this,
		"unitIncrement",
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
	 * Specifies the gap between the increase/decrease buttons and the thumb.
	 * <p>
	 * This is also settable via CSS with the "-vfx-buttons-gap" property.
	 */
	public StyleableDoubleProperty buttonsGapProperty() {
		return buttonsGap;
	}

	public void setButtonsGap(double buttonsGap) {
		this.buttonsGap.set(buttonsGap);
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

		private static final CssMetaData<VFXScrollBar, Boolean> SHOW_BUTTONS =
			FACTORY.createBooleanCssMetaData(
				"-vfx-show-buttons",
				VFXScrollBar::showButtonsProperty,
				false
			);

		private static final CssMetaData<VFXScrollBar, Number> BUTTONS_GAP =
			FACTORY.createSizeCssMetaData(
				"-mfx-buttons-gap",
				VFXScrollBar::buttonsGapProperty,
				1.0
			);

		private static final CssMetaData<VFXScrollBar, Orientation> ORIENTATION =
			FACTORY.createEnumCssMetaData(
				Orientation.class,
				"-mfx-orientation",
				VFXScrollBar::orientationProperty,
				Orientation.VERTICAL
			);

		private static final CssMetaData<VFXScrollBar, Number> TRACK_INCREMENT =
			FACTORY.createSizeCssMetaData(
				"-mfx-track-increment",
				VFXScrollBar::trackIncrementProperty,
				25.0
			);

		private static final CssMetaData<VFXScrollBar, Number> UNIT_INCREMENT =
			FACTORY.createSizeCssMetaData(
				"-mfx-unit-increment",
				VFXScrollBar::unitIncrementProperty,
				10.0
			);

		private static final CssMetaData<VFXScrollBar, Boolean> SMOOTH_SCROLL =
			FACTORY.createBooleanCssMetaData(
				"-mfx-smooth-scroll",
				VFXScrollBar::smoothScrollProperty,
				false
			);

		private static final CssMetaData<VFXScrollBar, Boolean> TRACK_SMOOTH_SCROLL =
			FACTORY.createBooleanCssMetaData(
				"-mfx-track-smooth-scroll",
				VFXScrollBar::trackSmoothScrollProperty,
				false
			);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
				Control.getClassCssMetaData(),
				SHOW_BUTTONS, BUTTONS_GAP,
				ORIENTATION,
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

	/**
	 * Increments the {@link #valueProperty()} by the amount specified by the {@link #unitIncrementProperty()}.
	 */
	public void uIncrement() {
		setValue(getValue() + getUnitIncrement());
	}

	/**
	 * Decrements the {@link #valueProperty()} by the amount specified by the {@link #unitIncrementProperty()}.
	 */
	public void uDecrement() {
		setValue(getValue() - getUnitIncrement());
	}

	/**
	 * Increments the {@link #valueProperty()} by the amount specified by the {@link #trackIncrementProperty()}.
	 */
	public void tIncrement() {
		setValue(getValue() + getTrackIncrement());
	}

	/**
	 * Decrements the {@link #valueProperty()} by the amount specified by the {@link #trackIncrementProperty()}.
	 */
	public void tDecrement() {
		setValue(getValue() - getTrackIncrement());
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

	public ScrollBounds getScrollBounds() {
		return scrollBounds.get();
	}

	/**
	 * Specifies both the content's bounds and the view area's bounds.
	 */
	public ObjectProperty<ScrollBounds> scrollBoundsProperty() {
		return scrollBounds;
	}

	public void setScrollBounds(ScrollBounds scrollBounds) {
		this.scrollBounds.set(scrollBounds);
	}

	public ScrollDirection getScrollDirection() {
		return scrollDirection.get();
	}

	/**
	 * Specifies the current scroll direction. The default behavior implementation manages this.
	 *
	 * @see VFXScrollBarBehavior
	 */
	public ObjectProperty<ScrollDirection> scrollDirectionProperty() {
		return scrollDirection;
	}

	public void setScrollDirection(ScrollDirection scrollDirection) {
		this.scrollDirection.setValue(scrollDirection);
	}
}

