package io.github.palexdev.virtualizedfx.controls.skins;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.base.bindings.*;
import io.github.palexdev.mfxcore.builders.InsetsBuilder;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.builders.bindings.ObjectBindingBuilder;
import io.github.palexdev.mfxcore.controls.SkinBase;
import io.github.palexdev.mfxcore.observables.When;
import io.github.palexdev.mfxcore.utils.NumberUtils;
import io.github.palexdev.mfxcore.utils.fx.LayoutUtils;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.mfxeffects.animations.Animations.KeyFrames;
import io.github.palexdev.mfxeffects.animations.Animations.TimelineBuilder;
import io.github.palexdev.mfxeffects.animations.motion.M3Motion;
import io.github.palexdev.virtualizedfx.base.VFXContainer;
import io.github.palexdev.virtualizedfx.controls.VFXScrollBar;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollBarBehavior;
import io.github.palexdev.virtualizedfx.controls.behaviors.VFXScrollPaneBehavior;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.LayoutMode;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static io.github.palexdev.mfxcore.events.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.OnInvalidated.withListener;
import static io.github.palexdev.mfxcore.observables.When.onChanged;
import static io.github.palexdev.mfxcore.observables.When.onInvalidated;

/**
 * Default skin implementation for {@link VFXScrollPane}.
 * <p></p>
 * There are three components: the content container also referred to as the "viewport", and the two scroll bars.
 * <p></p>
 * Considering all the properties/features, all the possible combinations between them, and in general the flexibility my
 * scroll pane offers, the layout strategy is quite complex, too much to be described into details in the documentation.
 * That said, I'll try to document the most important aspects of it, but note that the source code also contains many comments.
 * <p>
 * Because of this complexity I decided to split the layout strategy by defining a common interface and four concrete
 * implementations that correspond to the four possible combinations between these two properties:
 * {@link VFXScrollPane#vBarPosProperty()} and {@link VFXScrollPane#hBarPosProperty()}.
 * <p>
 * Many other properties and combinations determine the layout but those two are the main ones.
 */
public class VFXScrollPaneSkin extends SkinBase<VFXScrollPane, VFXScrollPaneBehavior> {
    //================================================================================
    // Properties
    //================================================================================
    private final Pane viewport;
    private final Rectangle clip;

    private final VFXScrollBar vBar;
    private final VFXScrollBar hBar;
    private BarsVisibilityProperty bvp;

    protected double DEFAULT_SIZE = 100.0;

    // Animations
    protected Duration SHOW_HIDE_DURATION = M3Motion.MEDIUM4;
    protected Duration HIDE_DELAY = M3Motion.LONG2;
    protected Interpolator SHOW_HIDE_CURVE = Interpolator.EASE_BOTH;
    private Animation showAnimation;
    private Animation hideAnimation;

    // Bindings for virtualized content!
    private VirtualScrollBinding vBinding;
    private VirtualScrollBinding hBinding;

    //================================================================================
    // Constructors
    //================================================================================
    public VFXScrollPaneSkin(VFXScrollPane pane) {
        super(pane);

        // Init viewport & clip
        viewport = new Pane() {
            @Override
            protected void layoutChildren() {
                layoutContent();
            }
        };
        viewport.getStyleClass().add("viewport");

        clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        clip.arcWidthProperty().bind(pane.clipBorderRadiusProperty());
        clip.arcHeightProperty().bind(pane.clipBorderRadiusProperty());
        viewport.setClip(clip);

        // Init scroll bars
        vBar = new VFXScrollBar(Orientation.VERTICAL);
        hBar = new VFXScrollBar(Orientation.HORIZONTAL);

        // Finalize init
        addListeners();
        getChildren().setAll(viewport, vBar, hBar);
    }

    //================================================================================
    // Methods
    //================================================================================

    /**
     * Binds all the delegate properties declared in {@link VFXScrollPane} to the corresponding {@link VFXScrollBar}.
     * <p>
     * Additionally binds the scroll bars' visibility to a custom property implementation: {@link BarsVisibilityProperty}.
     * <p></p>
     * <p>
     * Adds the following listeners:
     * <p> - A listener to update the layout to the following properties:
     * {@link VFXScrollPane#fitToWidthProperty()}, {@link VFXScrollPane#fitToHeightProperty()},
     * {@link VFXScrollPane#vBarPosProperty()}, {@link VFXScrollPane#hBarPosProperty()}, {@link VFXScrollPane#scrollBarsGapProperty()}
     * <p> - A listener on the {@link VFXScrollPane#contentProperty()} to update the viewport and call {@link #updateScrollBindings(Node, Node)}
     * <p> - A listener on the {@link VFXScrollPane#hoverProperty()} to hide/show the scroll bars according to the {@link VFXScrollPane#autoHideBarsProperty()}
     * and a bunch of other conditions, see {@link #showBars(boolean)}
     * <p> - A listener on {@link VFXScrollPane#minBarsOpacityProperty()} and {@link VFXScrollPane#maxBarsOpacityProperty()}
     * to call {@link #buildBarsAnimations()}
     */
    private void addListeners() {
        VFXScrollPane pane = getSkinnable();
        InvalidationListener ll = l -> pane.requestLayout();

        // Bindings
        bvp = new BarsVisibilityProperty();

        vBar.layoutModeProperty().bind(pane.layoutModeProperty());
        vBar.visibleProperty().bind(bvp.map(arr -> arr[0]));
        vBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
            .setMapper(() -> () -> pane.getVBarBehavior().apply(vBar))
            .addSources(pane.vBarBehaviorProperty())
            .get()
        );
        vBar.minProperty().bind(pane.vMinProperty());
        vBar.valueProperty().bindBidirectional(pane.vValueProperty());
        vBar.maxProperty().bind(pane.vMaxProperty());
        vBar.showButtonsProperty().bind(pane.showButtonsProperty());
        vBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        vBar.trackIncrementProperty().bind(pane.vTrackIncrementProperty());
        vBar.unitIncrementProperty().bind(pane.vUnitIncrementProperty());
        vBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        vBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

        hBar.layoutModeProperty().bind(pane.layoutModeProperty());
        hBar.visibleProperty().bind(bvp.map(arr -> arr[1]));
        hBar.behaviorProviderProperty().bind(ObjectBindingBuilder.<Supplier<VFXScrollBarBehavior>>build()
            .setMapper(() -> () -> pane.getHBarBehavior().apply(hBar))
            .addSources(pane.hBarBehaviorProperty())
            .get()
        );
        hBar.minProperty().bind(pane.hMinProperty());
        hBar.valueProperty().bindBidirectional(pane.hValueProperty());
        hBar.maxProperty().bind(pane.hMaxProperty());
        hBar.showButtonsProperty().bind(pane.showButtonsProperty());
        hBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        hBar.trackIncrementProperty().bind(pane.hTrackIncrementProperty());
        hBar.unitIncrementProperty().bind(pane.hUnitIncrementProperty());
        hBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        hBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());

        // Listeners
        listeners(
            // Base
            onChanged(pane.contentProperty())
                .then((oc, nc) -> {
                    if (nc != null) {
                        viewport.getChildren().setAll(nc);
                    } else {
                        viewport.getChildren().clear();
                    }
                    updateScrollBindings(oc, nc);
                })
                .executeNow(() -> pane.getContent() != null),

            // Layout
            withListener(pane.fitToWidthProperty(), ll),
            withListener(pane.fitToHeightProperty(), ll),
            withListener(pane.vBarPosProperty(), ll),
            withListener(pane.hBarPosProperty(), ll),
            withListener(pane.scrollBarsGapProperty(), ll),
            // Animations
            onInvalidated(pane.minBarsOpacityProperty())
                .then(v -> buildBarsAnimations())
                .invalidating(pane.maxBarsOpacityProperty()),
            onInvalidated(pane.hoverProperty())
                .condition(h -> !(vBar.isPressed() || vBar.isPressed()))
                .then(this::showBars)
                .invalidating(pane.autoHideBarsProperty())
                .invalidating(vBar.pressedProperty())
                .invalidating(hBar.pressedProperty())
                .executeNow()
        );
    }

    /**
     * This core method is responsible for laying out the scroll pane's content in the viewport.
     * <p>
     * Virtualized content ({@link VFXContainer}) always take all the available space and are positioned at [0, 0].
     * <p>
     * For standard content the layout depends on the following conditions:
     * <p> - {@link VFXScrollPane#alignmentProperty()} which determines where the content is in the viewport.
     * {@link Pos} carries both the vertical and horizontal positions into single enum constants. Note that if the content
     * is taller/wider than the viewport, then the corresponding position is ignored!
     * <p> - If the {@link VFXScrollPane#fitToWidthProperty()} and {@link VFXScrollPane#fitToWidthProperty()} are active
     * than the content will always take all the width/height available, regardless of its preferred sizes
     * <p></p>
     * Last but not least, this method calls {@link #updateVisualAmount(Node)} and {@link VFXScrollPaneBehavior#setViewportSize(Size)}
     * at the end.
     */
    protected void layoutContent() {
        VFXScrollPane pane = getSkinnable();
        Node content = pane.getContent();
        if (content == null) {
            updateVisualAmount(null);
            return;
        }

        double w = viewport.getWidth();
        double h = viewport.getHeight();
        if (content instanceof VFXContainer<?>) {
            // Virtualized containers always take up all the space and thus ignore the alignment too
            content.resizeRelocate(0, 0, w, h);
        } else {
            Pos alignment = pane.getAlignment();
            VPos vAlign = alignment.getVpos();
            HPos hAlign = alignment.getHpos();
            double cw = LayoutUtils.snappedBoundWidth(content);
            double ch = LayoutUtils.snappedBoundHeight(content);
            cw = pane.isFitToWidth()
                ? Math.max(w, cw)
                : cw;
            ch = pane.isFitToHeight()
                ? Math.max(h, ch)
                : ch;

            // If the content is larger than the viewport then the alignment is ignored
            if (ch > h) vAlign = VPos.TOP;
            if (cw > w) hAlign = HPos.LEFT;

            content.resize(cw, ch);
            positionInArea(content, 0, 0, w, h, 0, hAlign, vAlign);
        }

        updateVisualAmount(content);

        // Also update viewport size in behavior for features such as the drag to scroll
        Optional.ofNullable(getBehavior()).ifPresent(b -> b.setViewportSize(Size.of(w, h)));
    }

    /**
     * To avoid over-complicating things by translating the viewport, because then we'd have to translate its clip too,
     * this implementation translates the content directly.
     * <p>
     * This core method is responsible for re-creating the bindings that make the content scroll when it changes.
     * <p>
     * Virtualized content ({@link VFXContainer}) and standard content are treated differently! While the latter is
     * translated for real by using the translateX/Y properties, the virtualized content is not moved but its
     * {@link VFXContainer#vPosProperty()} and {@link VFXContainer#hPosProperty()} are bound appropriately.
     */
    protected void updateScrollBindings(Node oldContent, Node newContent) {
        if (oldContent != null) {
            oldContent.translateXProperty().unbind();
            oldContent.translateYProperty().unbind();
        }
        if (newContent == null) return;

        if (!(newContent instanceof VFXContainer<?> c)) {
            newContent.translateXProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double cw = newContent.getLayoutBounds().getWidth();
                    double vw = viewport.getWidth();
                    double maxScroll = Math.max(0, cw - vw);
                    return -maxScroll * hBar.getValue();
                })
                .addSources(newContent.layoutBoundsProperty(), viewport.widthProperty())
                .addSources(hBar.valueProperty())
                .get()
            );
            newContent.translateYProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double ch = newContent.getLayoutBounds().getHeight();
                    double vh = viewport.getHeight();
                    double maxScroll = Math.max(0, ch - vh);
                    return -maxScroll * vBar.getValue();
                })
                .addSources(newContent.layoutBoundsProperty(), viewport.heightProperty())
                .addSources(vBar.valueProperty())
                .get()
            );
        } else {
            if (vBinding != null) vBinding.dispose();
            if (hBinding != null) hBinding.dispose();
            vBinding = new VirtualScrollBinding(Orientation.VERTICAL, c).bind();
            hBinding = new VirtualScrollBinding(Orientation.HORIZONTAL, c).bind();
        }
    }

    /**
     * This method is responsible for updating the {@link VFXScrollBar#visibleAmountProperty()} properties of both the
     * scroll bars according to the sizes of the content and the viewport.
     * <p>
     * Trivially, the values are given by this formula {@code viewportSize / contentSize} and determines how much content
     * is visible as a percentage (0.0 to 1.0)
     * <p></p>
     * For virtualized content ({@link VFXContainer}) the content's sizes are given by:
     * {@link VFXContainer#virtualMaxXProperty()} and {@link VFXContainer#virtualMaxYProperty()}
     */
    protected void updateVisualAmount(Node content) {
        if (content == null) {
            vBar.setVisibleAmount(0.0);
            hBar.setVisibleAmount(0.0);
            return;
        }

        VFXScrollPane pane = getSkinnable();
        Size contentSize = pane.getContentBounds();
        Size viewportSize = Size.of(
            viewport.getWidth(),
            viewport.getHeight()
        );
        vBar.setVisibleAmount(viewportSize.getHeight() / contentSize.getHeight());
        hBar.setVisibleAmount(viewportSize.getWidth() / contentSize.getWidth());
    }

    /**
     * This method animates the scroll bars' opacity according to the {@code show} parameter.
     * <p>
     * Both the animations last {@link #SHOW_HIDE_DURATION} and use {@link #SHOW_HIDE_CURVE} as the interpolator.
     * The hide animation is delayed by {@link #HIDE_DELAY}.
     */
    protected void showBars(boolean show) {
        VFXScrollPane pane = getSkinnable();

        if (!pane.isAutoHideBars()) {
            if (Animations.isPlaying(hideAnimation)) hideAnimation.stop();
            if (Animations.isPlaying(showAnimation)) showAnimation.stop();
            vBar.setOpacity(1.0);
            hBar.setOpacity(1.0);
            return;
        }

        if (showAnimation == null || hideAnimation == null)
            buildBarsAnimations();

        showAnimation.setOnFinished(null);
        if (show) {
            hideAnimation.stop();
            showAnimation.playFromStart();
        } else {
            // The hide animation is played only after the show animation ended
            // This is very important to prevent "glitchy", undesired behaviors
            if (Animations.isPlaying(showAnimation)) {
                showAnimation.setOnFinished(e -> hideAnimation.playFromStart());
            } else {
                hideAnimation.playFromStart();
            }
        }
    }

    /**
     * Responsible for building the show and hide animations for the scroll bars according to the parameters specified
     * by [VFXScrollPane#minBarsOpacityProperty()] and [VFXScrollPane#maxBarsOpacityProperty()].
     */
    private void buildBarsAnimations() {
        VFXScrollPane pane = getSkinnable();
        double min = NumberUtils.clamp(pane.getMinBarsOpacity(), 0.0, 1.0);
        double max = NumberUtils.clamp(pane.getMaxBarsOpacity(), 0.0, 1.0);
        hideAnimation = TimelineBuilder.build()
            .setDelay(HIDE_DELAY)
            .add(KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), min, SHOW_HIDE_CURVE))
            .add(KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), min, SHOW_HIDE_CURVE))
            .getAnimation();
        showAnimation = TimelineBuilder.build()
            .add(KeyFrames.of(SHOW_HIDE_DURATION, vBar.opacityProperty(), max, SHOW_HIDE_CURVE))
            .add(KeyFrames.of(SHOW_HIDE_DURATION, hBar.opacityProperty(), max, SHOW_HIDE_CURVE))
            .getAnimation();
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /**
     * Initializes the behavior by calling {@link VFXScrollPaneBehavior#init()} and by registering the following handlers:
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_PRESSED} to call {@link VFXScrollBarBehavior#mousePressed(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_DRAGGED} to call {@link VFXScrollBarBehavior#mouseDragged(MouseEvent)}
     * <p> - intercepts events of type {@link MouseEvent#MOUSE_RELEASED} to call {@link VFXScrollBarBehavior#mouseReleased(MouseEvent)}
     * <p> - intercepts events of type {@link ScrollEvent#SCROLL} and re-routes such events to the appropriate scroll bar,
     * see {@link VFXScrollBarBehavior#scroll(ScrollEvent)}
     * <p> - intercepts events of type {@link KeyEvent#KEY_PRESSED} to call {@link VFXScrollBarBehavior#keyPressed(KeyEvent)}
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
            intercept(viewport, MouseEvent.MOUSE_RELEASED)
                .asFilter()
                .process(behavior::mouseReleased),
            intercept(viewport, ScrollEvent.SCROLL)
                .process(e -> behavior.scroll(e, c -> {
                    // Re-route scroll events to the appropriate scroll bar's behavior
                    Orientation o = pane.getMainAxis();
                    VFXScrollBar target = switch (o) {
                        case VERTICAL -> c.isShiftDown() ? hBar : vBar;
                        case HORIZONTAL -> c.isShiftDown() ? vBar : hBar;
                    };
                    if (target.isVisible()) target.getBehavior().scroll(c);
                })),
            intercept(pane, KeyEvent.KEY_PRESSED)
                .process(behavior::keyPressed)
        );
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return leftInset + DEFAULT_SIZE + LayoutUtils.snappedBoundWidth(vBar) + rightInset;
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
        return topInset + DEFAULT_SIZE + LayoutUtils.snappedBoundHeight(hBar) + bottomInset;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        VFXScrollPane pane = getSkinnable();
        LayoutHandler layoutHandler = LayoutHandler.getHandler(pane);
        layoutHandler.layout(pane, viewport, vBar, hBar);
    }

    @Override
    public void dispose() {
        if (bvp != null) {
            bvp.dispose();
            bvp = null;
        }
        if (vBinding != null) {
            vBinding.dispose();
            vBinding = null;
        }
        if (hBinding != null) {
            hBinding.dispose();
            hBinding = null;
        }
        super.dispose();
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /**
     * Simplifies the layout strategy by dividing it into four base cases which depend on the combination between the
     * {@link VFXScrollPane#vBarPosProperty()} and the {@link VFXScrollPane#hBarPosProperty()}; and so this has four
     * concrete implementations for the following positions: {@code TOP_LEFT}, {@code TOP_RIGHT}, {@code BOTTOM_RIGHT},
     * {@code BOTTOM_LEFT}.
     * <p>
     * It also defines common computations that depend on other properties and simpler conditions.
     * <p></p>
     * In general the scroll pane's layout is quite complex. For the sake of simplicity, and in some cases as design
     * choices some properties may be ignored under specific conditions. See the methods docs.
     */
    protected interface LayoutHandler {
        Map<PosPair, LayoutHandler> HANDLERS = Map.of(
            PosPair.of(VPos.TOP, HPos.LEFT), new TopLeftHandler(),
            PosPair.of(VPos.TOP, HPos.RIGHT), new TopRightHandler(),
            PosPair.of(VPos.BOTTOM, HPos.LEFT), new BottomLeftHandler(),
            PosPair.of(VPos.BOTTOM, HPos.RIGHT), new BottomRightHandler()
        );

        /**
         * @return the appropriate {@code LayoutHandler} implementation for the given scroll pane considering its
         * {@link VFXScrollPane#vBarPosProperty()} and {@link VFXScrollPane#hBarPosProperty()}
         */
        static LayoutHandler getHandler(VFXScrollPane pane) {
            HPos vBarPos = pane.getVBarPos().toHPos();
            VPos hBarPos = pane.getHBarPos().toVPos();
            return HANDLERS.get(PosPair.of(hBarPos, vBarPos));
        }

        void layout(VFXScrollPane pane, Pane viewport, VFXScrollBar vBar, VFXScrollBar hBar);

        /**
         * @return the values specified by the {@link VFXScrollPane#scrollBarsGapProperty()} or {@code 0.0} if either
         * the {@code vBar} or {@code hBar} are hidden.
         * @see BarsVisibilityProperty
         */
        default double getBarsGap(VFXScrollPane pane, VFXScrollBar vBar, VFXScrollBar hBar) {
            return (!vBar.isVisible() || !hBar.isVisible())
                ? 0.0
                : pane.getScrollBarsGap();
        }

        /**
         * If the {@link VFXScrollPane#layoutModeProperty()} is set to {@link LayoutMode#COMPACT} basically returns
         * {@link VFXScrollPane#insetsProperty()}, otherwise returns {@link Insets#EMPTY}.
         * <p></p>
         * In other words: in compact mode the padding is used to space the scroll bars from the edges; in standard
         * mode the bars cannot go on top of the viewport but instead the padding is used as the distance between the two.
         * <p>
         * Which values of the {@link Insets} object are used depend on the handler's implementation!!
         */
        default Insets getBarsPadding(VFXScrollPane pane) {
            return (pane.getLayoutMode() == LayoutMode.COMPACT)
                ? InsetsBuilder.of(pane.snappedTopInset(), pane.snappedRightInset(), pane.snappedBottomInset(), pane.snappedLeftInset())
                : Insets.EMPTY;
        }

        /**
         * Determines the viewport's size and position.
         * <p>
         * When the {@link VFXScrollPane#layoutModeProperty()} is set to {@link LayoutMode#COMPACT} the viewport
         * always takes all the available space and is positioned at [0, 0].
         * <p></p>
         * In standard mode things are much more complicated. In simple terms, the offset parameters determine the distance
         * from the origin ([0, 0]) and the end (which would be the scroll pane's [maxX, maxY]).
         * <p>
         * The reason for that is quite simple. This basically ensures that the viewport never covers the scroll bars and
         * that there is the right distance between the three.
         * <p>
         * The offsets highly depend on the scroll bars positions as specified by the
         * {@link VFXScrollPane#vBarPosProperty()} and {@link VFXScrollPane#hBarPosProperty()}.
         * <p></p>
         * Last but not least, note that the x and y offsets are ignored if either the vertical or horizontal scroll bar
         * are hidden!!
         * <p>
         * For example, if the vertical bar is not visible, then we don't want to take it into
         * account when laying out the viewport. We want the latter to take all the horizontal space.
         * When the bar is on the right side, then we simply stretch the viewport's width to the end of the scroll pane.
         * When the bar is on the left side, things are a bit more complex, because we want the width to be the same as
         * the scroll pane, but we also need to position the viewport at {@code x = 0}
         */
        default LayoutInfo computeViewportLayout(
            VFXScrollPane pane, VFXScrollBar vBar, VFXScrollBar hBar,
            double startXOffset, double endXOffset, double startYOffset, double endYOffset
        ) {

            /*
             * In `COMPACT` mode the viewport takes all the available space.
             *
             * In the `DEFAULT` mode the viewport layout depends on the visibility of the bars.
             *
             * The padding is ignored on the opposite sides of the scroll bars.
             * In other words currently the padding just determines the gap between the bars and the viewport when they
             * are visible.
             */

            if (pane.getLayoutMode() == LayoutMode.COMPACT) {
                return LayoutInfo.of(0, 0, pane.getWidth(), pane.getHeight());
            }

            if (pane.getVBarPolicy() == ScrollBarPolicy.NEVER || !vBar.isVisible()) {
                startXOffset = 0.0;
                endXOffset = 0.0;
            }
            if (pane.getHBarPolicy() == ScrollBarPolicy.NEVER || !hBar.isVisible()) {
                startYOffset = 0.0;
                endYOffset = 0.0;
            }
            return LayoutInfo.of(
                startXOffset,
                startYOffset,
                pane.getWidth() + endXOffset,
                pane.getHeight() + endYOffset
            );
        }
    }

    protected static class TopLeftHandler implements LayoutHandler {
        @Override
        public void layout(VFXScrollPane pane, Pane viewport, VFXScrollBar vBar, VFXScrollBar hBar) {
            double w = pane.getWidth();
            double h = pane.getHeight();
            double barsGap = getBarsGap(pane, vBar, hBar);
            Insets bPadding = getBarsPadding(pane);

            double vBarW = LayoutUtils.snappedBoundWidth(vBar);
            double hBarH = LayoutUtils.snappedBoundHeight(hBar);

            vBar.resize(
                vBarW,
                Math.max(0, h - hBarH - barsGap - bPadding.getTop() - bPadding.getBottom())
            );
            Region.positionInArea(
                vBar,
                bPadding.getLeft(), hBarH + barsGap + bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.TOP,
                pane.isSnapToPixel()
            );

            hBar.resize(
                Math.max(0, w - vBarW - barsGap - bPadding.getLeft() - bPadding.getRight()),
                hBarH
            );
            Region.positionInArea(
                hBar,
                vBarW + barsGap + bPadding.getLeft(), bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.TOP,
                pane.isSnapToPixel()
            );

            LayoutInfo li = computeViewportLayout(
                pane, vBar, hBar,
                vBar.getWidth() + pane.snappedLeftInset(), -(vBar.getWidth() + pane.snappedLeftInset()),
                hBar.getHeight() + pane.snappedTopInset(), -(hBar.getHeight() + pane.snappedTopInset())
            );
            li.layout(viewport);
        }
    }

    protected static class TopRightHandler implements LayoutHandler {
        @Override
        public void layout(VFXScrollPane pane, Pane viewport, VFXScrollBar vBar, VFXScrollBar hBar) {
            double w = pane.getWidth();
            double h = pane.getHeight();
            double barsGap = getBarsGap(pane, vBar, hBar);
            Insets bPadding = getBarsPadding(pane);

            double vBarW = LayoutUtils.snappedBoundWidth(vBar);
            double hBarH = LayoutUtils.snappedBoundHeight(hBar);

            vBar.resize(
                vBarW,
                Math.max(0, h - hBarH - barsGap - bPadding.getTop() - bPadding.getBottom())
            );
            Region.positionInArea(
                vBar,
                -bPadding.getRight(), hBarH + barsGap + bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.RIGHT, VPos.TOP,
                pane.isSnapToPixel()
            );

            hBar.resize(
                Math.max(0, w - vBarW - barsGap - bPadding.getLeft() - bPadding.getRight()),
                hBarH
            );
            Region.positionInArea(
                hBar,
                bPadding.getLeft(), bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.TOP,
                pane.isSnapToPixel()
            );

            LayoutInfo li = computeViewportLayout(
                pane, vBar, hBar,
                0.0, -(vBar.getWidth() + pane.snappedRightInset()),
                hBar.getHeight() + pane.snappedTopInset(), -(hBar.getHeight() + pane.snappedTopInset())
            );
            li.layout(viewport);
        }
    }

    protected static class BottomRightHandler implements LayoutHandler {
        @Override
        public void layout(VFXScrollPane pane, Pane viewport, VFXScrollBar vBar, VFXScrollBar hBar) {
            double w = pane.getWidth();
            double h = pane.getHeight();
            double barsGap = getBarsGap(pane, vBar, hBar);
            Insets bPadding = getBarsPadding(pane);

            double vBarW = LayoutUtils.snappedBoundWidth(vBar);
            double hBarH = LayoutUtils.snappedBoundHeight(hBar);

            vBar.resize(
                vBarW,
                Math.max(0, h - hBarH - barsGap - bPadding.getTop() - bPadding.getBottom())
            );
            Region.positionInArea(
                vBar,
                -bPadding.getRight(), bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.RIGHT, VPos.TOP,
                pane.isSnapToPixel()
            );

            hBar.resize(
                Math.max(0, w - vBarW - barsGap - bPadding.getLeft() - bPadding.getRight()),
                hBarH
            );
            Region.positionInArea(
                hBar,
                bPadding.getLeft(), -bPadding.getBottom(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.BOTTOM,
                pane.isSnapToPixel()
            );

            LayoutInfo li = computeViewportLayout(
                pane, vBar, hBar,
                0, -(vBar.getWidth() + pane.snappedRightInset()),
                0, -(hBar.getHeight() + pane.snappedBottomInset())
            );
            li.layout(viewport);
        }
    }

    protected static class BottomLeftHandler implements LayoutHandler {
        @Override
        public void layout(VFXScrollPane pane, Pane viewport, VFXScrollBar vBar, VFXScrollBar hBar) {
            double w = pane.getWidth();
            double h = pane.getHeight();
            double barsGap = getBarsGap(pane, vBar, hBar);
            Insets bPadding = getBarsPadding(pane);

            double vBarW = LayoutUtils.snappedBoundWidth(vBar);
            double hBarH = LayoutUtils.snappedBoundHeight(hBar);

            vBar.resize(
                vBarW,
                Math.max(0, h - hBarH - barsGap - bPadding.getTop() - bPadding.getBottom())
            );
            Region.positionInArea(
                vBar,
                bPadding.getLeft(), bPadding.getTop(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.TOP,
                pane.isSnapToPixel()
            );

            hBar.resize(
                Math.max(0, w - vBarW - barsGap - bPadding.getLeft() - bPadding.getRight()),
                hBarH
            );
            Region.positionInArea(
                hBar,
                vBarW + barsGap + bPadding.getLeft(), -bPadding.getBottom(), w, h, 0,
                Insets.EMPTY, HPos.LEFT, VPos.BOTTOM,
                pane.isSnapToPixel()
            );

            LayoutInfo li = computeViewportLayout(
                pane, vBar, hBar,
                vBar.getWidth() + pane.snappedLeftInset(), -(vBar.getWidth() + pane.snappedLeftInset()),
                0, -(hBar.getHeight() + pane.snappedBottomInset())
            );
            li.layout(viewport);
        }
    }

    /**
     * Utility record which carries a vertical and a horizontal alignment as {@link VPos} and {@link HPos} respectively.
     * <p>
     * (The {@link Pos} enumerator unfortunately does not offer an easy way to get one of its constants from a vPos and a hPos)
     */
    protected record PosPair(VPos vPos, HPos hPos) {
        public static PosPair of(VPos vPos, HPos hPos) {
            return new PosPair(vPos, hPos);
        }
    }

    /**
     * Utility record which carries the positions and sizes for a certain node.
     * <p>
     * It's also possible to quickly lay out that node by calling {@link #layout(Node)}.
     */
    protected record LayoutInfo(double x, double y, double w, double h) {
        public static LayoutInfo of(double x, double y, double w, double h) {
            return new LayoutInfo(x, y, w, h);
        }

        public void layout(Node node) {
            node.resizeRelocate(x, y, w, h);
        }
    }

    /**
     * Convenient custom property implementation that determines the visibility of the two scroll bars as an array of two
     * boolean values arranged as: {@code [vBarVisibility, hBarVisibility]}.
     * <p>
     * The bars are visible only if their {@link VFXScrollBar#visibleAmountProperty()} is lesser than {@code 1.0} and the
     * corresponding policy ({@link VFXScrollPane#vBarPolicyProperty()} and {@link VFXScrollPane#hBarPolicyProperty()})
     * is not set to {@link ScrollBarPolicy#NEVER}.
     * <p></p>
     * The convenience of this is not only in having a single property carrying two pieces of information but also that we
     * can perform common actions in a single place when any of the aforementioned dependencies change.
     * <p>
     * The {@link #invalidated()} method is overridden to:
     * <p> - Call {@link VFXScrollPaneBehavior#setCanVScroll(boolean)} and {@link VFXScrollPaneBehavior#setCanHScroll(boolean)}
     * <p> - Request a layout computation because the bars' visibility can also determine the size and position of the viewport
     * <p></p>
     * <b>Note:</b>
     * <p>
     * We are using a property rather than a binding because the binding would become invalid regardless of the computed
     * value. A slight change in the visible amount properties causes the value to be re-computed and if the result is
     * the same as the old one it would still fire a change event. This causes the layout to be computed when needed
     * therefore wasting performance.
     */
    protected class BarsVisibilityProperty extends SimpleObjectProperty<boolean[]> {
        private When<?> listener;

        {
            VFXScrollPane pane = getSkinnable();
            listener = onInvalidated(vBar.visibleAmountProperty())
                .then(v -> update())
                .invalidating(hBar.visibleAmountProperty())
                .invalidating(pane.vBarPolicyProperty())
                .invalidating(pane.hBarPolicyProperty())
                .listen();
        }

        @Override
        public void set(boolean[] newValue) {
            boolean[] oldValue = get();
            if (Arrays.equals(oldValue, newValue)) return;
            super.set(newValue);
        }

        @Override
        protected void invalidated() {
            VFXScrollPane pane = getSkinnable();
            Optional.ofNullable(getBehavior()).ifPresent(b -> {
                b.setCanVScroll(get()[0]);
                b.setCanHScroll(get()[1]);
            });

            // BUG for some fucking reason JavaFX ignores this layout request if not wrapped in a runLater call
            //  Even though the request is already on the UI thread
            Platform.runLater(pane::requestLayout);
        }

        protected void update() {
            VFXScrollPane pane = getSkinnable();
            boolean vBarVisible = vBar.getVisibleAmount() < 1.0 && pane.getVBarPolicy() != ScrollBarPolicy.NEVER;
            boolean hBarVisible = hBar.getVisibleAmount() < 1.0 && pane.getHBarPolicy() != ScrollBarPolicy.NEVER;
            setValue(new boolean[]{vBarVisible, hBarVisible});
        }

        public void dispose() {
            if (listener != null) {
                listener.dispose();
                listener = null;
            }
        }
    }

    protected class VirtualScrollBinding {
        private Boolean active = false;

        private ObservableValue<Number> target;
        private Function<Number, Number> targetMapper; // val percent -> pixels pos
        private BiConsumer<Number, Number> targetUpdater;

        private ObservableValue<Number> source;
        private Function<Number, Number> sourceMapper; // pixels pos -> val percent
        private BiConsumer<Number, Number> sourceUpdater;

        private final List<Consumer<AbstractBinding<?>>> invalidating = new ArrayList<>();

        public VirtualScrollBinding(Orientation orientation, VFXContainer<?> container) {
            VFXScrollPane pane = getSkinnable();
            if (orientation == Orientation.VERTICAL) {
                this.target = container.vPosProperty();
                this.targetMapper = val -> val.doubleValue() * container.getMaxVScroll();
                this.targetUpdater = (o, n) -> container.setVPos(n.doubleValue());

                this.source = pane.vValueProperty();
                this.sourceMapper = pos ->
                    NumberUtils.mapOneRangeToAnother(
                        pos.doubleValue(),
                        DoubleRange.of(0.0, container.getMaxVScroll()),
                        DoubleRange.of(0.0, 1.0)
                    );
                this.sourceUpdater = (o, n) -> pane.setVValue(n.doubleValue());

                invalidating.add(b -> b.addSourcesInvalidatingSource(container.maxVScrollProperty()));
            } else {
                this.target = container.hPosProperty();
                this.targetMapper = val -> val.doubleValue() * container.getMaxHScroll();
                this.targetUpdater = (o, n) -> container.setHPos(n.doubleValue());

                this.source = pane.hValueProperty();
                this.sourceMapper = pos ->
                    NumberUtils.mapOneRangeToAnother(
                        pos.doubleValue(),
                        DoubleRange.of(0.0, container.getMaxHScroll()),
                        DoubleRange.of(0.0, 1.0)
                    );
                this.sourceUpdater = (o, n) -> pane.setHValue(n.doubleValue());

                invalidating.add(b -> b.addSourcesInvalidatingSource(container.maxHScrollProperty()));
            }
        }

        public VirtualScrollBinding bind() {
            if (active == null) return null; // Disposed!
            if (!active) {
                MFXBindings bindings = MFXBindings.instance();
                BidirectionalBinding<Number> binding = bindings.bindBidirectional(target).addSources(
                    new MappingSource.Builder<Number, Number>()
                        .observable(source)
                        .targetUpdater(new MappedUpdater<>(
                            Mapper.of(targetMapper),
                            targetUpdater::accept
                        ))
                        .sourceUpdater(new MappedUpdater<>(
                            Mapper.of(sourceMapper),
                            sourceUpdater::accept
                        ))
                        .get()
                );

                invalidating.forEach(c -> c.accept(binding));

                binding.get();
            }
            active = true;
            return this;
        }

        public void dispose() {
            active = null;
            MFXBindings.instance().unbindBidirectional(target);

            target = null;
            targetMapper = null;
            targetUpdater = null;

            source = null;
            sourceMapper = null;
            sourceUpdater = null;

            invalidating.clear();
        }
    }
}
