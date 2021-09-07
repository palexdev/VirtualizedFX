package io.github.palexdev.virtualizedfx.skin;

/*
public class SimpleVirtualFlowSkin<T, C extends ISimpleCell> extends SkinBase<SimpleVirtualFlow<T, C>> {
    private final ScrollBar vBar;
    private final SimpleVirtualFlowContainer<T, C> container;

    public SimpleVirtualFlowSkin(SimpleVirtualFlow<T, C> virtualFlow) {
        super(virtualFlow);
        vBar = new ScrollBar();
        container = new SimpleVirtualFlowContainer<>(virtualFlow);
        container.setManaged(false);

        setUpScrollBars();
        EventDispatcher original = container.getEventDispatcher();
        container.setEventDispatcher((event, tail) -> {
            tail.prepend(vBar.getEventDispatcher());
            return original.dispatchEvent(event, tail);
        });

        getChildren().setAll(container, vBar);
    }

    private void setUpScrollBars() {
        SimpleVirtualFlow<T, C> virtualFlow = getSkinnable();
        vBar.getStyleClass().add("vbar");
        vBar.setManaged(false);
        vBar.setOrientation(Orientation.VERTICAL);
        vBar.setUnitIncrement(15);
        vBar.maxProperty().bind(Bindings.createDoubleBinding(
                () -> snapSpaceY(container.getTotalHeight() - virtualFlow.getHeight()),
                container.totalHeightProperty(), virtualFlow.heightProperty()
        ));
        vBar.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> container.getTotalHeight() > virtualFlow.getHeight(),
                container.totalHeightProperty(), virtualFlow.heightProperty()
        ));
        virtualFlow.vValueProperty().bind(vBar.valueProperty());
        virtualFlow.vValueProperty().addListener((observable, oldValue, newValue) -> container.setLayoutY(-newValue.doubleValue()));
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);
        container.resizeRelocate(0, 0, w, -1);
        vBar.resizeRelocate(
                w - 10,
                0,
                10,
                h
        );
    }
}
*/
