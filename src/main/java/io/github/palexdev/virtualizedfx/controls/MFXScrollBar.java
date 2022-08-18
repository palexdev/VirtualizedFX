package io.github.palexdev.virtualizedfx.controls;

import io.github.palexdev.mfxcore.base.properties.styleable.StyleableBooleanProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableDoubleProperty;
import io.github.palexdev.mfxcore.base.properties.styleable.StyleableObjectProperty;
import io.github.palexdev.mfxcore.utils.fx.PropUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import io.github.palexdev.virtualizedfx.ResourceManager;
import io.github.palexdev.virtualizedfx.controls.behavior.MFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behavior.base.BehaviorBase;
import io.github.palexdev.virtualizedfx.controls.behavior.base.MFXBehavioral;
import io.github.palexdev.virtualizedfx.controls.skins.MFXScrollBarSkin;
import javafx.beans.property.*;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

import java.util.List;

/**
 * Implementation of a scroll bar following material design guidelines in JavaFX.
 * <p>
 * In addition to an appealing style, {@code MFXScrollBar} offers many new features compared
 * to the boring standard JavaFX' scroll bar, such as:
 * <p> - The ability to show/hide the increase/decrease buttons through a property
 * <p> - The ability to specify the gap between the buttons and the thumb
 * <p> - Inbuilt smooth scroll for the thumb and the track
 * <p> All of the above feature (and also every other not mentioned here) can also be changed trough CSS.
 * <p></p>
 * {@code MFXScrollBar} also offers 4 new PseudoClasses for CSS:
 * <p> - ":dragging": active when the thumb is being dragged
 * <p> - ":horizontal": active when the scroll bar's orientation is HORIZONTAL
 * <p> - ":vertical": active when the scroll bar's orientation is VERTICAL
 * <p> - ":focus-within": active when any of the internal components of the scroll bar is focused
 * <p></p>
 * {@code MFXScrollBar} is also the first control to use the new behavior API, so you can easily change
 * the behavior of the control by creating your own behavior and set it trough {@link #behaviorProperty()}.
 * <p></p>
 * <b>NOTE:</b> there is an important difference between this and the standard JavaFX' scroll bar.
 * Both allow to set a minimum and a maximum value, <b>BUT</b>, for {@code MFXScrollBar} these values
 * are capped to 0.0 (min) and 1.0 (max).
 * <p>
 * The idea here is to make the scroll bar work on "percentage values". 0.0 the thumb is all the way up, 1.0 the thumb
 * is all the way down, as simple as that. It would make little to no sense to make a scroll bar work on pixels
 * value, since the scroll bar alone is a pretty useless control. Once you combine it with other controls (for example
 * in a scroll pane) it's your job to tweak the scrolling. In such cases you typically want to scroll by a certain amount
 * of pixels which means that the {@link #unitIncrementProperty()} and {@link #trackIncrementProperty()} must be determined
 * in percentage values.
 * <p>
 * A simple example: if I have a content of 12000px and I want to scroll 20px (for unit increment) and 80px (for track increment),
 * in a viewport of 1000px, we must compute both the increments as follows:
 * <p> ScrollableAmount = ContentLength - ViewportLength = 11000px;
 * <p> UnitIncrement = 20px / ScrollableAmount = 0.001818182;
 * <p> TrackIncrement = 80px / ScrollableAmount = 0.007272727;
 */
public class MFXScrollBar extends Control implements MFXBehavioral<MFXScrollBar, MFXScrollBarBehavior> {
	//================================================================================
	// Properties
	//================================================================================
	private final String STYLE_CLASS = "mfx-scroll-bar";
	private final String STYLESHEET = ResourceManager.loadResource("MFXScrollBar.css");

	private final DoubleProperty min = PropUtils.clampedDoubleProperty(() -> 0.0, this::getMax);
	private final DoubleProperty value = PropUtils.clampedDoubleProperty(this::getMin, this::getMax);
	private final DoubleProperty max = PropUtils.clampedDoubleProperty(this::getMin, () -> 1.0);
	private final DoubleProperty visibleAmount = new SimpleDoubleProperty();
	private final DoubleProperty thumbPos = new SimpleDoubleProperty();
	private final DoubleProperty trackLength = new SimpleDoubleProperty();

	private static final PseudoClass DRAGGING_PSEUDO_CLASS = PseudoClass.getPseudoClass("dragging");
	private final BooleanProperty dragging = new SimpleBooleanProperty() {
		@Override
		protected void invalidated() {
			pseudoClassStateChanged(DRAGGING_PSEUDO_CLASS, get());
		}
	};

	private final ObjectProperty<MFXScrollBarBehavior> behavior = PropUtils.mappedObjectProperty(val -> {
		BehaviorBase<MFXScrollBar> old = getBehavior();
		if (old != null) old.dispose();
		return val;
	});

	//================================================================================
	// Constructors
	//================================================================================
	public MFXScrollBar() {
		this(Orientation.VERTICAL);
	}

	public MFXScrollBar(Orientation orientation) {
		setOrientation(orientation);
		initialize();
	}

	//================================================================================
	// Methods
	//================================================================================
	private void initialize() {
		getStyleClass().add(STYLE_CLASS);
		setBehavior(defaultBehavior());
		setMin(0.0);
		setMax(1.0);
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	public MFXScrollBarBehavior defaultBehavior() {
		return new MFXScrollBarBehavior(this);
	}

	@Override
	public MFXScrollBarBehavior getBehavior() {
		return behavior.get();
	}

	@Override
	public ObjectProperty<MFXScrollBarBehavior> behaviorProperty() {
		return behavior;
	}

	@Override
	public void setBehavior(MFXScrollBarBehavior behavior) {
		this.behavior.set(behavior);
	}

	@Override
	public String getUserAgentStylesheet() {
		return STYLESHEET;
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		return new MFXScrollBarSkin(this);
	}

	//================================================================================
	// Styleable Properties
	//================================================================================
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

	public boolean isButtonsVisible() {
		return buttonsVisible.get();
	}

	/**
	 * Specifies whether the increase/decrease buttons are visible.
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
	 * Specifies the gap between the increase/decrease buttons and the thumb.
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

	//================================================================================
	// CssMetaData
	//================================================================================
	private static class StyleableProperties {
		private static final StyleablePropertyFactory<MFXScrollBar> FACTORY = new StyleablePropertyFactory<>(Control.getClassCssMetaData());
		private static final List<CssMetaData<? extends Styleable, ?>> cssMetaDataList;

		private static final CssMetaData<MFXScrollBar, Boolean> BUTTONS_VISIBLE =
				FACTORY.createBooleanCssMetaData(
						"-mfx-buttons-visible",
						MFXScrollBar::buttonsVisibleProperty,
						false
				);

		private static final CssMetaData<MFXScrollBar, Number> BUTTONS_GAP =
				FACTORY.createSizeCssMetaData(
						"-mfx-buttons-gap",
						MFXScrollBar::buttonsGapProperty,
						3.0
				);

		private static final CssMetaData<MFXScrollBar, Orientation> ORIENTATION =
				FACTORY.createEnumCssMetaData(
						Orientation.class,
						"-mfx-orientation",
						MFXScrollBar::orientationProperty,
						Orientation.VERTICAL
				);

		private static final CssMetaData<MFXScrollBar, Number> TRACK_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-mfx-track-increment",
						MFXScrollBar::trackIncrementProperty,
						0.1
				);

		private static final CssMetaData<MFXScrollBar, Number> UNIT_INCREMENT =
				FACTORY.createSizeCssMetaData(
						"-mfx-unit-increment",
						MFXScrollBar::unitIncrementProperty,
						0.01
				);

		private static final CssMetaData<MFXScrollBar, Boolean> SMOOTH_SCROLL =
				FACTORY.createBooleanCssMetaData(
						"-mfx-smooth-scroll",
						MFXScrollBar::smoothScrollProperty,
						false
				);

		private static final CssMetaData<MFXScrollBar, Boolean> TRACK_SMOOTH_SCROLL =
				FACTORY.createBooleanCssMetaData(
						"-mfx-track-smooth-scroll",
						MFXScrollBar::trackSmoothScrollProperty,
						false
				);

		static {
			cssMetaDataList = StyleUtils.cssMetaDataList(
					Control.getClassCssMetaData(),
					BUTTONS_VISIBLE, BUTTONS_GAP,
					ORIENTATION,
					TRACK_INCREMENT, UNIT_INCREMENT,
					SMOOTH_SCROLL, TRACK_SMOOTH_SCROLL
			);
		}
	}

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return StyleableProperties.cssMetaDataList;
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public double getMin() {
		return min.get();
	}

	/**
	 * The scroll bar's minimum value, clamped between 0.0 and the {@link #maxProperty()}.
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
	 * The current scroll bar's value, clamped between {@link #minProperty()} and {@link #maxProperty()}.
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
	 * The scroll bar's maximum value, clamped between {@link #minProperty()} and 1.0.
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
	 * Visible amount of the scroll bar's range.
	 * <p>
	 * The default skin computes this value as: {@code (thumbLength * (max - min)) / trackLength}.
	 * <p>
	 * This value is responsible for the length of the thumb, check the skin source code to understand
	 * better how it works.
	 */
	public ReadOnlyDoubleProperty visibleAmountProperty() {
		return visibleAmount;
	}

	public double getThumbPos() {
		return thumbPos.get();
	}

	/**
	 * This property tracks the position of the thumb along the track.
	 */
	public ReadOnlyDoubleProperty thumbPosProperty() {
		return thumbPos;
	}

	public double getTrackLength() {
		return trackLength.get();
	}

	/**
	 * This property tracks the length of the scroll bar's track.
	 */
	public ReadOnlyDoubleProperty trackLengthProperty() {
		return trackLength;
	}

	public boolean isDragging() {
		return dragging.get();
	}

	/**
	 * Specifies whether the scroll bar's thumb is being dragged.
	 */
	public ReadOnlyBooleanProperty draggingProperty() {
		return dragging;
	}
}
