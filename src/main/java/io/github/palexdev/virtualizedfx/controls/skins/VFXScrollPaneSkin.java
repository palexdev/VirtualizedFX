package io.github.palexdev.virtualizedfx.controls.skins;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.github.palexdev.mfxcore.base.beans.Size;
import io.github.palexdev.mfxcore.base.beans.range.DoubleRange;
import io.github.palexdev.mfxcore.base.bindings.MappedBidirectionalBinding;
import io.github.palexdev.mfxcore.base.properties.SizeProperty;
import io.github.palexdev.mfxcore.builders.bindings.DoubleBindingBuilder;
import io.github.palexdev.mfxcore.controls.MFXSkinBase;
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
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.HBarPos;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarPolicy;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.ScrollBarsAlignment;
import io.github.palexdev.virtualizedfx.enums.ScrollPaneEnums.VBarPos;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
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

import static io.github.palexdev.mfxcore.base.beans.Size.zero;
import static io.github.palexdev.mfxcore.input.WhenEvent.intercept;
import static io.github.palexdev.mfxcore.observables.When.*;
import static io.github.palexdev.virtualizedfx.utils.Utils.mapOf;

/// Default skin implementation for [VFXScrollPane].
///
/// There are three components: the content container (viewport) and the two scroll bars.
///
/// The viewport fills the entire scroll pane area, and the bars "float" on top of it. This means that the size and
/// positioning should be handled by the CSS.
public class VFXScrollPaneSkin extends MFXSkinBase<VFXScrollPane> {
    //================================================================================
    // Properties
    //================================================================================
    protected final Pane viewport;
    protected final Rectangle clip;

    protected final VFXScrollBar vBar;
    protected final VFXScrollBar hBar;
    private BarsVisibilityProperty bvp;

    protected double DEFAULT_SIZE = 100.0;

    // Animations
    protected Duration SHOW_HIDE_DURATION = M3Motion.MEDIUM4;
    protected Duration HIDE_DELAY = M3Motion.LONG2;
    protected Interpolator SHOW_HIDE_CURVE = Interpolator.EASE_BOTH;
    private Animation showAnimation;
    private Animation hideAnimation;

    private final SizeProperty contentBounds = new SizeProperty(zero());

    // Bindings for virtualized content!
    private VirtualScrollBinding vBinding;
    private VirtualScrollBinding hBinding;
    private When<?> virtualBoundsListener;

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
        viewport.setManaged(false);
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

    /// Binds all the delegate properties declared in [VFXScrollPane] to the corresponding [VFXScrollBar].
    ///
    /// Additionally binds the scroll bars' visibility to a custom property implementation: [BarsVisibilityProperty].
    ///
    /// Adds the following listeners:
    ///
    /// - A listener to update the layout to the following properties:
    /// [VFXScrollPane#fitToWidthProperty()], [VFXScrollPane#fitToHeightProperty()],
    /// [VFXScrollPane#vBarPosProperty()], [VFXScrollPane#hBarPosProperty()],
    /// [VFXScrollPane#barsInsetsProperty()], [VFXScrollPane#barsAlignmentProperty()]
    ///
    /// - A listener on the [VFXScrollPane#contentProperty()] to update the viewport and call [#updateScrollBindings(Node, Node)]
    ///
    /// - A listener on the [VFXScrollPane#hoverProperty()] to hide/show the scroll bars according to the [VFXScrollPane#autoHideBarsProperty()]
    /// and a bunch of other conditions, see [#showBars(boolean)]
    ///
    /// - A listener on [VFXScrollPane#minBarsOpacityProperty()] and [VFXScrollPane#maxBarsOpacityProperty()]
    /// to call [#buildBarsAnimations()]
    private void addListeners() {
        VFXScrollPane pane = getSkinnable();

        // Bindings
        ((ObjectProperty<Size>) pane.contentBoundsProperty()).bind(contentBounds);
        bvp = new BarsVisibilityProperty();

        vBar.visibleProperty().bind(bvp.map(arr -> arr[0]));
        vBar.behaviorFactoryProperty().bind(pane.vBarBehaviorProperty().map(f -> () -> f.apply(vBar)));
        vBar.minProperty().bind(pane.vMinProperty());
        vBar.valueProperty().bindBidirectional(pane.vValueProperty());
        vBar.maxProperty().bind(pane.vMaxProperty());
        vBar.showButtonsProperty().bind(pane.showButtonsProperty());
        vBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        vBar.trackIncrementProperty().bind(pane.vTrackIncrementProperty());
        vBar.unitIncrementProperty().bind(pane.vUnitIncrementProperty());
        vBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        vBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());
        ((DoubleProperty) pane.verticalVisibleAmountProperty()).bind(vBar.visibleAmountProperty());

        hBar.visibleProperty().bind(bvp.map(arr -> arr[1]));
        hBar.behaviorFactoryProperty().bind(pane.hBarBehaviorProperty().map(f -> () -> f.apply(hBar)));
        hBar.minProperty().bind(pane.hMinProperty());
        hBar.valueProperty().bindBidirectional(pane.hValueProperty());
        hBar.maxProperty().bind(pane.hMaxProperty());
        hBar.showButtonsProperty().bind(pane.showButtonsProperty());
        hBar.buttonsGapProperty().bind(pane.buttonsGapProperty());
        hBar.trackIncrementProperty().bind(pane.hTrackIncrementProperty());
        hBar.unitIncrementProperty().bind(pane.hUnitIncrementProperty());
        hBar.smoothScrollProperty().bind(pane.smoothScrollProperty());
        hBar.trackSmoothScrollProperty().bind(pane.trackSmoothScrollProperty());
        ((DoubleProperty) pane.horizontalVisibleAmountProperty()).bind(hBar.visibleAmountProperty());

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
            observe(
                pane::requestLayout,
                pane.fitToWidthProperty(), pane.fitToHeightProperty(),
                pane.barsInsetsProperty(), pane.barsAlignmentProperty(), pane.vBarPosProperty(), pane.hBarPosProperty()
            ),
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

    /// This core method is responsible for laying out the scroll pane's content in the viewport.
    ///
    /// Virtualized content ([VFXContainer]) always take all the available space and are positioned at [0,0].
    ///
    /// For standard content the layout depends on the following conditions:
    ///
    /// - [VFXScrollPane#alignmentProperty()] which determines where the content is in the viewport.
    /// [Pos] carries both the vertical and horizontal positions into single enum constants. Note that if the content
    /// is taller/wider than the viewport this gets ignored and the content positioned in the top-left corner.
    ///
    /// - If the [VFXScrollPane#fitToWidthProperty()] and [VFXScrollPane#fitToHeightProperty()] are active
    /// than the content will always take all the width/height available, regardless of its preferred sizes
    protected void layoutContent() {
        VFXScrollPane pane = getSkinnable();
        Node content = pane.getContent();
        if (content == null) {
            setContentBounds(zero());
            updateVisualAmount(null);
            return;
        }

        double w = viewport.getWidth();
        double h = viewport.getHeight();
        Size cs = computeContentBounds();

        if (content instanceof VFXContainer<?>) {
            // Virtualized containers always take up all the space and thus ignore the alignment too
            layoutInArea(content, 0, 0, w, h, 0, viewport.getPadding(), HPos.LEFT, VPos.TOP);
        } else {
            Pos alignment = pane.getAlignment();
            VPos vAlign = alignment.getVpos();
            HPos hAlign = alignment.getHpos();

            // If the content is larger than the viewport, then the alignment is ignored
            if (cs.width() > w) hAlign = HPos.LEFT;
            if (cs.height() > h) vAlign = VPos.TOP;

            content.resize(cs.width(), cs.height());
            positionInArea(content, 0, 0, w, h, 0, viewport.getPadding(), hAlign, vAlign);
        }

        setContentBounds(cs);
        updateVisualAmount(content);

        // Also update viewport size in behavior for features such as the drag to scroll.
        // Note: the visible size (padding excluded) is used, otherwise drag-to-scroll would be off when the
        // viewport has padding.
        ((SizeProperty) pane.viewportSizeProperty()).set(getViewportSize());
    }

    /// Re-routes such events to the appropriate scroll bar, see [VFXScrollBarBehavior#scroll(ScrollEvent)].
    protected void onScrollEvent(ScrollEvent e) {
        VFXScrollPane pane = getSkinnable();
        Orientation o = pane.getMainAxis();
        VFXScrollBar target = switch (o) {
            case VERTICAL -> e.isShiftDown() ? hBar : vBar;
            case HORIZONTAL -> e.isShiftDown() ? vBar : hBar;
        };
        if (target.isVisible()) target.getBehavior().scroll(e);
        e.consume();
    }

    /// Computes the scrollable size of the given content node.
    ///
    /// For virtualized containers ([VFXContainer]) the size is given by their virtual max properties.
    /// For standard resizable nodes ([Region]) the size is computed using [Region#prefWidth(double)]
    /// and [Region#prefHeight(double)] with content bias awareness, considering the
    /// [VFXScrollPane#fitToWidthProperty()] and [VFXScrollPane#fitToHeightProperty()] settings.
    /// For non-resizable nodes the size is derived from [Node#getLayoutBounds()].
    protected Size computeContentBounds() {
        VFXScrollPane pane = getSkinnable();
        Node content = pane.getContent();
        if (content == null) return zero();
        return switch (content) {
            case VFXTable<?> t -> Size.size(
                snapSizeX(t.getVirtualMaxX()),
                snapSizeY(t.getVirtualMaxY() + t.getColumnsSize().height())
            );
            case VFXContainer<?> c -> Size.size(
                snapSizeX(c.getVirtualMaxX()),
                snapSizeY(c.getVirtualMaxY())
            );
            default -> {
                if (content instanceof Region r && r.isResizable()) {
                    Orientation bias = r.getContentBias();
                    yield ContentBiasHandler.computeSize(bias, pane, getViewportSize());
                }
                Bounds b = content.getLayoutBounds();
                yield Size.size(
                    snapSizeX(b.getWidth()),
                    snapSizeY(b.getHeight())
                );
            }
        };
    }

    /// To avoid overcomplicating things by translating the viewport, because then we'd have to translate its clip too,
    /// this implementation translates the content directly.
    ///
    /// This core method is responsible for re-creating the bindings that make the content scroll when it changes.
    ///
    /// Virtualized content ([VFXContainer]) and standard content are treated differently! While the latter is
    /// translated for real by using the translateX/Y properties, the virtualized content is not moved but its
    /// [VFXContainer#vPosProperty()] and [VFXContainer#hPosProperty()] are bound appropriately.
    protected void updateScrollBindings(Node oldContent, Node newContent) {
        if (oldContent != null) {
            oldContent.translateXProperty().unbind();
            oldContent.translateYProperty().unbind();
        }
        if (newContent == null) return;

        VFXScrollPane pane = getSkinnable();
        if (!(newContent instanceof VFXContainer<?> c)) {
            newContent.translateXProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double cw = getContentBounds().width();
                    double vw = getViewportSize().width();
                    double maxScroll = Math.max(0, cw - vw);
                    return -maxScroll * hBar.getValue();
                })
                .addSources(newContent.layoutBoundsProperty(), viewport.widthProperty(), viewport.paddingProperty())
                .addSources(hBar.valueProperty())
                .get()
            );
            newContent.translateYProperty().bind(DoubleBindingBuilder.build()
                .setMapper(() -> {
                    double ch = getContentBounds().height();
                    double vh = getViewportSize().height();
                    double maxScroll = Math.max(0, ch - vh);
                    return -maxScroll * vBar.getValue();
                })
                .addSources(newContent.layoutBoundsProperty(), viewport.heightProperty(), viewport.paddingProperty())
                .addSources(vBar.valueProperty())
                .get()
            );
        } else {
            if (vBinding != null) vBinding.dispose();
            if (hBinding != null) hBinding.dispose();
            if (virtualBoundsListener != null) virtualBoundsListener.dispose();
            vBinding = new VirtualScrollBinding(Orientation.VERTICAL, c).bind();
            hBinding = new VirtualScrollBinding(Orientation.HORIZONTAL, c).bind();
            virtualBoundsListener = observe(pane::requestLayout, c.maxHScrollProperty(), c.maxVScrollProperty()).listen();
        }
    }

    /// This method is responsible for updating the [VFXScrollBar#visibleAmountProperty()] properties of both the
    /// scroll bars according to the sizes of the content and the viewport.
    ///
    /// Trivially, the values are given by this formula `viewportSize / contentSize` and determines how much content
    /// is visible as a percentage (0.0 to 1.0)
    ///
    /// For virtualized content ([VFXContainer]) the content's sizes are given by:
    /// [VFXContainer#virtualMaxXProperty()] and [VFXContainer#virtualMaxYProperty()]
    protected void updateVisualAmount(Node content) {
        if (content == null) {
            vBar.setVisibleAmount(0.0);
            hBar.setVisibleAmount(0.0);
            return;
        }

        Size contentSize = getContentBounds();
        Size viewportSize = getViewportSize();
        vBar.setVisibleAmount(viewportSize.height() / contentSize.height());
        hBar.setVisibleAmount(viewportSize.width() / contentSize.width());
    }

    /// This method animates the scroll bars' opacity according to the `show` parameter.
    ///
    /// Both the animations last [#SHOW_HIDE_DURATION] and use [#SHOW_HIDE_CURVE] as the interpolator.
    /// The hide animation is delayed by [#HIDE_DELAY].
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

    /// Responsible for building the show and hide animations for the scroll bars according to the parameters specified
    /// by [VFXScrollPane#minBarsOpacityProperty()] and [VFXScrollPane#maxBarsOpacityProperty()].
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

    protected Size getContentBounds() {
        return contentBounds.get();
    }

    protected SizeProperty contentBoundsProperty() {
        return contentBounds;
    }

    protected void setContentBounds(Size contentBounds) {
        this.contentBounds.set(contentBounds);
    }

    /// @return the viewport's actual visible size, which is its size minus its padding.
    ///
    /// All scroll-related computations that need the size of the visible area (visible amount, scroll bindings for
    /// standard content, content-bias fit sizing, drag-to-scroll) **must** use this instead of the raw
    /// [Region#getWidth()]/[Region#getHeight()]. Otherwise any padding set on the viewport (e.g. to prevent the clip
    /// from cutting the content's effects like drop shadows) would not be tracked and would lead to incorrect scrolling.
    protected Size getViewportSize() {
        Insets padding = viewport.getPadding();
        return Size.size(
            Math.max(0, viewport.getWidth() - padding.getLeft() - padding.getRight()),
            Math.max(0, viewport.getHeight() - padding.getTop() - padding.getBottom())
        );
    }

    //================================================================================
    // Overridden Methods
    //================================================================================

    /// Initializes the behavior by calling [VFXScrollPaneBehavior#init()] and by registering the following handlers:
    ///
    /// - intercepts events of type [MouseEvent#MOUSE_PRESSED] to call [VFXScrollBarBehavior#mousePressed(MouseEvent)]
    ///
    /// - intercepts events of type [MouseEvent#MOUSE_DRAGGED] to call [VFXScrollBarBehavior#mouseDragged(MouseEvent)]
    ///
    /// - intercepts events of type [MouseEvent#MOUSE_RELEASED] to call [VFXScrollBarBehavior#mouseReleased(MouseEvent)]
    ///
    /// - intercepts events of type [ScrollEvent#SCROLL] to call [#onScrollEvent(ScrollEvent)]
    ///
    /// - intercepts events of type [KeyEvent#KEY_PRESSED] to call [VFXScrollBarBehavior#keyPressed(KeyEvent)]
    @Override
    protected void registerBehavior() {
        super.registerBehavior();
        VFXScrollPane pane = getSkinnable();
        VFXScrollPaneBehavior behavior = getBehavior();
        events(
            intercept(viewport, MouseEvent.MOUSE_PRESSED)
                .asFilter()
                .handle(behavior::mousePressed),
            intercept(viewport, MouseEvent.MOUSE_DRAGGED)
                .asFilter()
                .handle(behavior::mouseDragged),
            intercept(viewport, MouseEvent.MOUSE_RELEASED)
                .asFilter()
                .handle(behavior::mouseReleased),
            intercept(viewport, ScrollEvent.SCROLL)
                .handle(e -> behavior.scroll(e, () -> onScrollEvent(e))),
            intercept(pane, KeyEvent.KEY_PRESSED)
                .handle(behavior::keyPressed)
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

        // viewport
        layoutInArea(viewport, x, y, w, h, 0, HPos.LEFT, VPos.TOP);

        // bars
        Insets barsInsets = pane.getBarsInsets();
        VBarPos vBarPos = pane.getVBarPos();
        HBarPos hBarPos = pane.getHBarPos();
        ScrollBarsAlignment barsAlignment = pane.getBarsAlignment();

        Size vSize = Size.size(
            LayoutUtils.snappedBoundWidth(vBar),
            pane.getHeight() - barsInsets.getTop() - barsInsets.getBottom()
        );
        Size hSize = Size.size(
            pane.getWidth() - barsInsets.getLeft() - barsInsets.getRight(),
            LayoutUtils.snappedBoundHeight(hBar)
        );

        vBar.resize(vSize.width(), vSize.height() - hSize.height());
        VPos vBarAlign = barsAlignment == ScrollBarsAlignment.DEFAULT ? VPos.TOP : VPos.CENTER;
        positionInArea(vBar, 0, 0, pane.getWidth(), pane.getHeight(), 0, vBarPos.toHPos(), vBarAlign);

        hBar.resize(hSize.width() - vSize.width(), hSize.height());
        HPos hBarAlign = barsAlignment == ScrollBarsAlignment.DEFAULT ? HPos.LEFT : HPos.CENTER;
        positionInArea(hBar, 0, 0, pane.getWidth(), pane.getHeight(), 0, hBarAlign, hBarPos.toVPos());
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
        if (virtualBoundsListener != null) {
            virtualBoundsListener.dispose();
            virtualBoundsListener = null;
        }
        super.dispose();
    }

    @Override
    protected VFXScrollPaneBehavior getBehavior() {
        return (VFXScrollPaneBehavior) super.getBehavior();
    }

    //================================================================================
    // Inner Classes
    //================================================================================

    /// Convenient custom property implementation that determines the visibility of the two scroll bars as an array of two
    /// boolean values arranged as: `[vBarVisibility, hBarVisibility]`.
    ///
    /// The bars are visible only if their [VFXScrollBar#visibleAmountProperty()] is lesser than `1.0` and the
    /// corresponding policy ([VFXScrollPane#vBarPolicyProperty()] and [VFXScrollPane#hBarPolicyProperty()])
    /// is not set to [ScrollBarPolicy#NEVER].
    ///
    /// The convenience of this is not only in having a single property carrying two pieces of information but also that we
    /// can perform common actions in a single place when any of the aforementioned dependencies change.
    ///
    /// The [#invalidated()] method is overridden to:
    ///
    /// - Call [VFXScrollPaneBehavior#setCanVScroll(boolean)] and [VFXScrollPaneBehavior#setCanHScroll(boolean)]
    ///
    /// - Request a layout computation because the bars' visibility can also determine the size and position of the viewport
    ///
    /// **Note:**
    ///
    /// We are using a property rather than a binding because the binding would become invalid regardless of the computed
    /// value. A slight change in the visible amount properties causes the value to be re-computed and if the result is
    /// the same as the old one it would still fire a change event. This causes the layout to be computed when needed
    /// therefore wasting performance.
    protected class BarsVisibilityProperty extends SimpleObjectProperty<boolean[]> {
        private When<?> listener;

        {
            VFXScrollPane pane = getSkinnable();
            listener = observe(
                this::update,
                vBar.visibleAmountProperty(), pane.vBarPolicyProperty(),
                hBar.visibleAmountProperty(), pane.hBarPolicyProperty()
            ).listen();
        }

        @Override
        public void set(boolean[] newValue) {
            boolean[] oldValue = get();
            if (Arrays.equals(oldValue, newValue)) return;
            super.set(newValue);
        }

        @Override
        protected void invalidated() {
            // layout is not needed because the bars always "float" on top of the content
            Optional.ofNullable(getBehavior()).ifPresent(b -> {
                b.setCanVScroll(get()[0]);
                b.setCanHScroll(get()[1]);
            });
        }

        protected void update() {
            VFXScrollPane pane = getSkinnable();
            boolean vBarVisible = computeVisibility(vBar, pane.getVBarPolicy());
            boolean hBarVisible = computeVisibility(hBar, pane.getHBarPolicy());
            setValue(new boolean[]{vBarVisible, hBarVisible});
        }

        protected boolean computeVisibility(VFXScrollBar bar, ScrollBarPolicy policy) {
            if (policy == ScrollBarPolicy.NEVER) return false;
            double va = bar.getVisibleAmount();
            // If va is 0 or 1.0 there's no content or it's fully visible, therefore hide the bar
            return va > 0.0 && va < 1.0;
        }

        public void dispose() {
            if (listener != null) {
                listener.dispose();
                listener = null;
            }
        }
    }

    protected class VirtualScrollBinding {
        private MappedBidirectionalBinding<Number, Number> binding;

        public VirtualScrollBinding(Orientation orientation, VFXContainer<?> container) {
            VFXScrollPane pane = getSkinnable();

            ObservableValue<Number> first;
            Function<Number, Number> firstMapper;

            ObservableValue<Number> second;
            Function<Number, Number> secondMapper;

            Observable dependency;

            if (orientation == Orientation.VERTICAL) {
                first = container.vPosProperty();
                firstMapper = pos ->
                    NumberUtils.mapOneRangeToAnother(
                        pos.doubleValue(),
                        DoubleRange.of(0.0, container.getMaxVScroll()),
                        DoubleRange.of(0.0, 1.0)
                    );

                second = pane.vValueProperty();
                secondMapper = val -> val.doubleValue() * container.getMaxVScroll();

                dependency = container.maxVScrollProperty();
            } else {
                first = container.hPosProperty();
                firstMapper = pos ->
                    NumberUtils.mapOneRangeToAnother(
                        pos.doubleValue(),
                        DoubleRange.of(0.0, container.getMaxHScroll()),
                        DoubleRange.of(0.0, 1.0)
                    );

                second = pane.hValueProperty();
                secondMapper = val -> val.doubleValue() * container.getMaxHScroll();

                dependency = container.maxHScrollProperty();
            }

            binding = new MappedBidirectionalBinding<>(first, second)
                .setFirstToSecondMapper(firstMapper)
                .setSecondToFirstMapper(secondMapper)
                .addDependenciesFor(MappedBidirectionalBinding.Target.SECOND, dependency);
            /*
             * Setting the dependency with Target.SECOND makes the viewport stay in position when the bounds change
             */
        }

        public VirtualScrollBinding bind() {
            if (binding.isActive()) return null;
            binding.bind();
            return this;
        }

        public void dispose() {
            binding.dispose();
            binding = null;
        }
    }

    @FunctionalInterface
    protected interface ContentBiasHandler {
        Map<Orientation, ContentBiasHandler> HANDLERS = mapOf(
            null, (ContentBiasHandler) (vsp, vs, c) -> Size.size(
                boundedSize(vsp.isFitToWidth() ? vs.width() : c.prefWidth(-1), c.minWidth(-1), c.maxWidth(-1)),
                boundedSize(vsp.isFitToHeight() ? vs.height() : c.prefHeight(-1), c.minHeight(-1), c.maxHeight(-1))
            ),
            Orientation.HORIZONTAL, (ContentBiasHandler) (vsp, vs, c) -> {
                double cw = boundedSize(vsp.isFitToWidth() ? vs.width() : c.prefWidth(-1), c.minWidth(-1), c.maxWidth(-1));
                double ch = boundedSize(vsp.isFitToHeight() ? vs.height() : c.prefHeight(cw), c.minHeight(cw), c.maxHeight(cw));
                return Size.size(cw, ch);
            },
            Orientation.VERTICAL, (ContentBiasHandler) (vsp, vs, c) -> {
                double ch = boundedSize(vsp.isFitToHeight() ? vs.height() : c.prefHeight(-1), c.minHeight(-1), c.maxHeight(-1));
                double cw = boundedSize(vsp.isFitToWidth() ? vs.width() : c.prefWidth(ch), c.minWidth(ch), c.maxWidth(ch));
                return Size.size(cw, ch);
            }
        );

        static Size computeSize(Orientation bias, VFXScrollPane vsp, Size viewportSize) {
            return HANDLERS.get(bias).computeSize(vsp, viewportSize, vsp.getContent());
        }

        Size computeSize(VFXScrollPane vsp, Size viewportSize, Node content);

        private static double boundedSize(double value, double min, double max) {
            return Math.min(Math.max(value, min), Math.max(min, max));
        }
    }
}
