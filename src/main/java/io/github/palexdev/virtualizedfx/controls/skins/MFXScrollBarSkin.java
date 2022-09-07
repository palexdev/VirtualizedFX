package io.github.palexdev.virtualizedfx.controls.skins;

import io.github.palexdev.mfxcore.base.beans.PositionBean;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.builders.bindings.BooleanBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.nodes.IconWrapperBuilder;
import io.github.palexdev.mfxcore.controls.MFXIconWrapper;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxresources.builders.IconBuilder;
import io.github.palexdev.virtualizedfx.controls.MFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.behavior.MFXScrollBarBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.*;
import javafx.scene.control.SkinBase;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

/**
 * Default skin implementation for {@link MFXScrollBar}.
 * <p></p>
 * There are four components: the track, the thumb and the two buttons.
 */
public class MFXScrollBarSkin extends SkinBase<MFXScrollBar> {
	//================================================================================
	// Properties
	//================================================================================
	private final Region track;
	private final Region thumb;
	private final MFXIconWrapper decIcon;
	private final MFXIconWrapper incIcon;

	private final double DEFAULT_LENGTH = 100.0;
	private double trackLength;
	private double thumbLength;
	private DoubleBinding thumbPos;

	private boolean vamAdjust = false; // To signal listeners that the visible amount has been adjusted
	private boolean barPressed = false;

	private static final PseudoClass VERTICAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("vertical");
	private static final PseudoClass HORIZONTAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("horizontal");
	private static final PseudoClass FOCUS_WITHIN_PSEUDO_CLASS = PseudoClass.getPseudoClass("focus-within");

	//================================================================================
	// Constructors
	//================================================================================
	public MFXScrollBarSkin(MFXScrollBar scrollBar) {
		super(scrollBar);

		track = new Region();
		track.getStyleClass().add("track");
		thumb = new Region();
		thumb.getStyleClass().add("thumb");

		decIcon = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().get())
				.addRippleGenerator()
				.addStyleClasses("decrement-icon")
				.getNode();
		incIcon = IconWrapperBuilder.build()
				.setIcon(IconBuilder.build().get())
				.addRippleGenerator()
				.addStyleClasses("increment-icon")
				.getNode();

		thumbPos = DoubleBindingBuilder.build()
				.setMapper(() -> valToPos(scrollBar.getValue()))
				.addSources(scrollBar.valueProperty())
				.addSources(thumb.widthProperty(), thumb.heightProperty())
				.get();


		onOrientationChange();
		updateChildren();
		scrollBar.setValue(scrollBar.getValue()); // Adjusts the value if min and max are not 0.0/1.0
		addListeners();
		initBehavior();
	}

	//================================================================================
	// Methods
	//================================================================================

	/**
	 * Adds the following listeners:
	 * <p> - A listener to update the layout to the following properties: {@link MFXScrollBar#minProperty()}, {@link MFXScrollBar#maxProperty()},
	 * {@link MFXScrollBar#visibleAmountProperty()}, {@link MFXScrollBar#buttonsGapProperty()}
	 * <p> - A listener to the {@link MFXScrollBar#buttonsVisibleProperty()} to call {@link #updateChildren()} and update the layout
	 * <p> - A listener to the {@link MFXScrollBar#orientationProperty()} to call {@link #onOrientationChange()} and update the layout
	 * <p> - A listener to the {@link MFXScrollBar#behaviorProperty()} to call {@link #initBehavior()}
	 * <p> - Two listeners to activate the ":focus-within" PseudoClass when the increase/decrease buttons are focused
	 * <p></p>
	 * Also adds the needed bindings for {@link MFXScrollBar#thumbPosProperty()} and {@link MFXScrollBar#trackLengthProperty()},
	 * as well as a binding responsible for hiding the scroll bar when the visibleAmount is greater or equal to 1.0.
	 * <p></p>
	 * Also adds a one-shot listener by using a {@link When} construct to initialize the thumb position once the skin has been initialized
	 * (to be more precise, one both width and height are greater than 0).
	 */
	private void addListeners() {
		MFXScrollBar sb = getSkinnable();
		InvalidationListener reLayout = invalidated -> {
			if (vamAdjust) {
				vamAdjust = false; // TODO check effectiveness
				return;
			}
			sb.requestLayout();
		};

		// TODO should adjust value too?
		sb.minProperty().addListener(reLayout);
		sb.maxProperty().addListener(reLayout);
		sb.visibleAmountProperty().addListener(reLayout);
		((DoubleProperty) sb.thumbPosProperty()).bind(thumbPos);
		((DoubleProperty) sb.trackLengthProperty()).bind(
				DoubleBindingBuilder.build()
						.setMapper(() -> (sb.getOrientation() == Orientation.VERTICAL) ? track.getHeight() : track.getWidth())
						.addSources(track.widthProperty(), track.heightProperty())
						.addSources(sb.orientationProperty())
						.get()
		);

		sb.visibleProperty().bind(BooleanBindingBuilder.build()
				.setMapper(() -> sb.getVisibleAmount() < 1.0)
				.addSources(sb.visibleAmountProperty())
				.get()
		);

		sb.buttonsVisibleProperty().addListener(invalidated -> {
			updateChildren();
			thumbPos.invalidate();
			sb.requestLayout();
		});
		sb.buttonsGapProperty().addListener(reLayout);

		sb.orientationProperty().addListener(invalidated -> {
			onOrientationChange();
			sb.requestLayout();
		});

		sb.behaviorProperty().addListener(invalidated -> initBehavior());

		decIcon.focusedProperty().addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(FOCUS_WITHIN_PSEUDO_CLASS, newValue));
		incIcon.focusedProperty().addListener((observable, oldValue, newValue) -> pseudoClassStateChanged(FOCUS_WITHIN_PSEUDO_CLASS, newValue));

		When.onChanged(track.layoutBoundsProperty())
				.condition((oldValue, newValue) -> newValue.getWidth() > 0 && newValue.getHeight() > 0)
				.then((oldValue, newValue) -> thumbPos.invalidate())
				.oneShot()
				.listen();
	}

	/**
	 * This is the bridge between this skin and the scroll bar's behavior.
	 * It is responsible for registering all the handlers for the skin's components
	 * into the behavior object, this includes:
	 * <p> - The behavior for {@link ScrollEvent#SCROLL} events
	 * <p> - The behavior for {@link MouseEvent#MOUSE_PRESSED} and {@link MouseEvent#MOUSE_RELEASED} events
	 * <p> - The behavior for {@link MouseEvent#MOUSE_PRESSED}, {@link MouseEvent#MOUSE_DRAGGED} and {@link MouseEvent#MOUSE_RELEASED}
	 * events on the thumb
	 * <p> - The behavior for {@link MouseEvent#MOUSE_PRESSED} and {@link MouseEvent#MOUSE_RELEASED} events on the track
	 * <p> - The behavior for {@link MouseEvent#MOUSE_PRESSED} and {@link MouseEvent#MOUSE_RELEASED} events on the buttons
	 * <p></p>
	 * This is called every time the behavior is changed.
	 */
	private void initBehavior() {
		MFXScrollBar sb = getSkinnable();
		MFXScrollBarBehavior behavior = sb.getBehavior();
		if (behavior == null) return;

		behavior.handler(sb, ScrollEvent.SCROLL, behavior::scroll);
		behavior.handler(sb, MouseEvent.MOUSE_PRESSED, me -> {
			// If there are gaps between the track and the scroll bar
			// the event can be captured by the scroll bar
			// Detect such cases and send a fake event to the track.
			// Before doing so though it's necessary to convert the mouse
			// coordinates from the scroll bar to the track.
			if (me.getPickResult().getIntersectedNode() == sb) {
				barPressed = true;
				Point2D toTrack = track.parentToLocal(me.getX(), me.getY());
				MouseEvent nme = new MouseEvent(
						null, track, MouseEvent.MOUSE_PRESSED,
						toTrack.getX(), toTrack.getY(), 0, 0,
						MouseButton.PRIMARY, 1,
						false, false, false, false, true,
						false, false, false, false, false, null
				);
				Event.fireEvent(track, nme);
				me.consume();
			}
		});
		behavior.handler(sb, MouseEvent.MOUSE_RELEASED, me -> {
			if (barPressed) {
				behavior.trackReleased(me);
				barPressed = false;
			}
		});

		behavior.handler(thumb, MouseEvent.MOUSE_PRESSED, behavior::thumbPressed);
		behavior.handler(thumb, MouseEvent.MOUSE_DRAGGED, behavior::thumbDragged);
		behavior.handler(thumb, MouseEvent.MOUSE_RELEASED, me -> behavior.thumbReleased());

		behavior.handler(track, MouseEvent.MOUSE_PRESSED, behavior::trackPressed);
		behavior.handler(track, MouseEvent.MOUSE_RELEASED, behavior::trackReleased);

		behavior.handler(decIcon, MouseEvent.MOUSE_PRESSED, me -> {
			decIcon.requestFocus();
			behavior.decPressed();
		});
		behavior.handler(decIcon, MouseEvent.MOUSE_RELEASED, me -> behavior.stopAnimations());

		behavior.handler(incIcon, MouseEvent.MOUSE_PRESSED, me -> {
			incIcon.requestFocus();
			behavior.incPressed();
		});
		behavior.handler(incIcon, MouseEvent.MOUSE_RELEASED, me -> behavior.stopAnimations());
	}

	/**
	 * This is responsible for updating the children list according to the {@link MFXScrollBar#buttonsVisibleProperty()}.
	 */
	private void updateChildren() {
		MFXScrollBar sb = getSkinnable();
		boolean buttonsVisible = sb.isButtonsVisible();
		if (buttonsVisible) {
			getChildren().setAll(track, thumb, decIcon, incIcon);
		} else {
			getChildren().setAll(track, thumb);
		}
	}

	/**
	 * This is responsible for updating the ":vertical" and ":horizontal" PseudoClass states,
	 * as well as create the right binding for the thumb position every time the scroll bar's orientation
	 * change.
	 */
	private void onOrientationChange() {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		if (o == Orientation.VERTICAL) {
			pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, false);
			pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, true);
			thumb.translateXProperty().unbind();
			thumb.translateYProperty().bind(thumbPos);
		} else {
			pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, false);
			pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, true);
			thumb.translateYProperty().unbind();
			thumb.translateXProperty().bind(thumbPos);
		}
	}

	/**
	 * This core method is responsible for converting the {@link MFXScrollBar#valueProperty()} to the coordinate
	 * (x or y depending on the orientation) at which the thumb must be positioned.
	 */
	private double valToPos(double val) {
		MFXScrollBar sb = getSkinnable();
		double min = sb.getMin();
		double max = sb.getMax();
		assert val >= min && val <= max;
		return NumberUtils.mapOneRangeToAnother(
				val,
				DoubleRange.of(min, max),
				DoubleRange.of(0.0, trackLength - thumbLength)
		);
	}

	/**
	 * Responsible for computing the scroll bar's length (width for VERTICAL, height for HORIZONTAL).
	 */
	private double getLength(Orientation orientation) {
		MFXScrollBar sb = getSkinnable();
		double length;
		double padding;
		if (orientation == Orientation.VERTICAL) {
			padding = snappedLeftInset() + snappedRightInset();
			length = LayoutUtils.boundWidth(thumb) + padding;
		} else {
			padding = snappedTopInset() + snappedBottomInset();
			length = LayoutUtils.boundHeight(thumb) + padding;
		}

		if (sb.isButtonsVisible())
			length = Math.max(length, Math.max(decIcon.getSize(), incIcon.getSize()) + padding);
		return length;
	}

	/**
	 * @return the minimum allowed track length, 2 * {@link #getLength(Orientation)}
	 */
	private double minTrackLength(Orientation orientation) {
		return getLength(orientation) * 2.0;
	}

	/**
	 * @return the minimum allowed thumb length, 1.5 * {@link #getLength(Orientation)}
	 */
	private double minThumbLength(Orientation orientation) {
		return getLength(orientation) * 1.5;
	}

	/**
	 * @return the visible portion as {@link MFXScrollBar} / (max - min)
	 */
	private double visiblePortion() {
		MFXScrollBar sb = getSkinnable();
		return sb.getVisibleAmount() / (sb.getMax() - sb.getMin());
	}

	private DoubleProperty visibleAmountProperty() {
		return ((DoubleProperty) getSkinnable().visibleAmountProperty());
	}

	//================================================================================
	// Overridden Methods
	//================================================================================
	@Override
	protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		boolean buttonsVisible = sb.isButtonsVisible();
		double buttonsGap = buttonsVisible ? sb.getButtonsGap() : 0.0;
		return (o == Orientation.VERTICAL) ?
				getLength(Orientation.VERTICAL) :
				leftInset + decIcon.getSize() + minTrackLength(Orientation.HORIZONTAL) + incIcon.getSize() + rightInset + (buttonsGap * 2);
	}

	@Override
	protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		boolean buttonsVisible = sb.isButtonsVisible();
		double buttonsGap = buttonsVisible ? sb.getButtonsGap() : 0.0;
		return (o == Orientation.VERTICAL) ?
				topInset + decIcon.getSize() + minTrackLength(Orientation.VERTICAL) + incIcon.getSize() + bottomInset + (buttonsGap * 2) :
				getLength(Orientation.HORIZONTAL);
	}

	@Override
	protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		boolean buttonsVisible = sb.isButtonsVisible();
		double buttonsGap = buttonsVisible ? sb.getButtonsGap() : 0.0;
		return (o == Orientation.VERTICAL) ?
				getLength(Orientation.VERTICAL) :
				leftInset + DEFAULT_LENGTH + rightInset + (buttonsGap * 2);
	}

	@Override
	protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		boolean buttonsVisible = sb.isButtonsVisible();
		double buttonsGap = buttonsVisible ? sb.getButtonsGap() : 0.0;
		return (o == Orientation.VERTICAL) ?
				topInset + DEFAULT_LENGTH + bottomInset + (buttonsGap * 2) :
				getLength(Orientation.HORIZONTAL);
	}

	@Override
	protected double computeMaxWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar scrollBar = getSkinnable();
		return (scrollBar.getOrientation() == Orientation.VERTICAL) ?
				scrollBar.prefWidth(-1) :
				Double.MAX_VALUE;
	}

	@Override
	protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
		MFXScrollBar scrollBar = getSkinnable();
		return (scrollBar.getOrientation() == Orientation.VERTICAL) ?
				Double.MAX_VALUE :
				scrollBar.prefHeight(-1);
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		MFXScrollBar sb = getSkinnable();
		Orientation o = sb.getOrientation();
		boolean buttonsVisible = sb.isButtonsVisible();
		double buttonsGap = buttonsVisible ? sb.getButtonsGap() : 0.0;

		double decSize = buttonsVisible ? decIcon.getSize() + buttonsGap : 0.0;
		double incSize = buttonsVisible ? incIcon.getSize() + buttonsGap : 0.0;

		if (buttonsVisible) {
			HPos decHPos = (o == Orientation.VERTICAL) ? HPos.CENTER : HPos.LEFT;
			HPos incHPos = (o == Orientation.VERTICAL) ? HPos.CENTER : HPos.RIGHT;
			VPos decVPos = (o == Orientation.VERTICAL) ? VPos.TOP : VPos.CENTER;
			VPos incVPos = (o == Orientation.VERTICAL) ? VPos.BOTTOM : VPos.CENTER;
			layoutInArea(decIcon, x, y, w, h, 0, decHPos, decVPos);
			layoutInArea(incIcon, x, y, w, h, 0, incHPos, incVPos);
		}

		double visiblePortion = visiblePortion();
		if (o == Orientation.VERTICAL) {
			trackLength = snapSizeY(h - (decSize + incSize));
			layoutInArea(track, x, y + decSize, w, trackLength, 0, HPos.CENTER, VPos.TOP);

			thumbLength = snapSizeY(NumberUtils.clamp(trackLength * visiblePortion, minThumbLength(Orientation.VERTICAL), trackLength));
			double visibleAmount = (thumbLength * (sb.getMax() - sb.getMin())) / trackLength;
			if (sb.getVisibleAmount() != visibleAmount && !visibleAmountProperty().isBound()) {
				vamAdjust = true;
				visibleAmountProperty().set(visibleAmount);
			}

			PositionBean position = LayoutUtils.computePosition(
					sb, thumb,
					x, y + decSize, w, thumbLength, 0,
					Insets.EMPTY, HPos.CENTER, VPos.TOP,
					false, true
			);
			double thumbW = LayoutUtils.boundWidth(thumb);
			thumb.resizeRelocate(position.getX(), position.getY(), thumbW, thumbLength);
		} else {
			trackLength = snapSizeX(w - (decSize + incSize));
			layoutInArea(track, x + decSize, y, trackLength, h, 0, HPos.LEFT, VPos.CENTER);

			thumbLength = snapSizeX(NumberUtils.clamp(trackLength * visiblePortion, minThumbLength(Orientation.HORIZONTAL), trackLength));
			double visibleAmount = (thumbLength * (sb.getMax() - sb.getMin())) / trackLength;
			if (sb.getVisibleAmount() != visibleAmount && !visibleAmountProperty().isBound()) {
				vamAdjust = true;
				visibleAmountProperty().set(visibleAmount);
			}

			PositionBean position = LayoutUtils.computePosition(
					sb, thumb,
					x + decSize, y, thumbLength, h, 0,
					Insets.EMPTY, HPos.LEFT, VPos.CENTER,
					false, true
			);
			double thumbH = LayoutUtils.boundWidth(thumb);
			thumb.resizeRelocate(position.getX(), position.getY(), thumbLength, thumbH);
		}
	}

	@Override
	public void dispose() {
		MFXScrollBarBehavior behavior = getSkinnable().getBehavior();
		if (behavior != null) behavior.dispose();

		thumb.translateXProperty().unbind();
		thumb.translateYProperty().unbind();
		thumbPos = null;

		super.dispose();
	}
}
