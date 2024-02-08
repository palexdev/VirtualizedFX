package utils;

import javafx.animation.PauseTransition;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.function.BiConsumer;

public class MouseHoldHandler<T extends Node> {
	//================================================================================
	// Properties
	//================================================================================
	protected T node;

	private EventHandler<MouseEvent> pressedHandler = this::handlePressed;
	private EventHandler<MouseEvent> movedHandler = this::handleMoved;
	private EventHandler<MouseEvent> releasedHandler = this::handleReleased;
	private EventHandler<MouseEvent> exitHandler = this::handleExit;

	public static final double DEFAULT_DELAY = 150.0;
	protected PauseTransition pause;
	private BiConsumer<MouseEvent, T> action;

	private MouseEvent event;

	//================================================================================
	// Constructors
	//================================================================================
	public MouseHoldHandler(T node) {
		this(node, (e, n) -> {});
	}

	public MouseHoldHandler(T node, BiConsumer<MouseEvent, T> action) {
		this.node = node;
		this.action = action;

		this.pause = new PauseTransition();
		serDelay(DEFAULT_DELAY);
		pause.setOnFinished(e -> handle());
	}

	public static <T extends Node> MouseHoldHandler<T> install(T node) {
		return new MouseHoldHandler<>(node).install();
	}

	//================================================================================
	// Methods
	//================================================================================
	public MouseHoldHandler<T> install() {
		node.addEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
		node.addEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
		node.addEventFilter(MouseEvent.MOUSE_RELEASED, releasedHandler);
		node.addEventFilter(MouseEvent.MOUSE_EXITED, exitHandler);
		return this;
	}

	public void uninstall() {
		node.removeEventFilter(MouseEvent.MOUSE_PRESSED, pressedHandler);
		node.removeEventFilter(MouseEvent.MOUSE_MOVED, movedHandler);
		node.removeEventFilter(MouseEvent.MOUSE_RELEASED, releasedHandler);
		node.removeEventFilter(MouseEvent.MOUSE_EXITED, exitHandler);
	}

	public void dispose() {
		pause.stop();
		uninstall();
		pressedHandler = null;
		movedHandler = null;
		releasedHandler = null;
		exitHandler = null;
		action = null;
		node = null;
	}

	protected void handle() {
		action.accept(event, node);
		event.consume();
		event = null;
	}

	protected void handlePressed(MouseEvent event) {
		if (Duration.ZERO.equals(pause.getDuration())) return;
		this.event = event;
		pause.playFromStart();
	}

	protected void handleMoved(MouseEvent event) {}

	protected void handleReleased(MouseEvent event) {
		pause.stop();
	}

	protected void handleExit(MouseEvent event) {
		pause.stop();
	}

	//================================================================================
	// Getters/Setters
	//================================================================================
	public MouseHoldHandler<T> setDelay(Duration delay) {
		pause.setDuration(delay);
		return this;
	}

	public MouseHoldHandler<T> serDelay(double millis) {
		pause.setDuration(Duration.millis(Math.max(0, millis)));
		return this;
	}

	public BiConsumer<MouseEvent, T> getAction() {
		return action;
	}

	public MouseHoldHandler<T> setAction(BiConsumer<MouseEvent, T> action) {
		this.action = action;
		return this;
	}
}
