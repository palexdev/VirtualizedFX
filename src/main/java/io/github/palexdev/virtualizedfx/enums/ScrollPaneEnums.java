package io.github.palexdev.virtualizedfx.enums;

import javafx.geometry.HPos;
import javafx.geometry.VPos;

/**
 * Convenience class that contains several enumerators for scroll panes.
 */
public class ScrollPaneEnums {

	private ScrollPaneEnums() {
	}

	/**
	 * Enumeration to define the layout strategy for a scroll pane.
	 */
	public enum LayoutMode {
		/**
		 * Extra space is reserved for both the vertical and horizontal scroll bars.
		 */
		DEFAULT,

		/**
		 * No extra space is reserved for the scroll bars, both are laid out
		 * on top of the content.
		 */
		COMPACT
	}

	/**
	 * Enumeration to define the visibility of a scroll pane's scroll bars.
	 */
	public enum ScrollBarPolicy {
		/**
		 * The scroll bars will be visible when needed.
		 */
		DEFAULT,

		/**
		 * The scroll bars will never be visible.
		 */
		NEVER
	}

	/**
	 * Enumeration to specify the position of the vertical scroll bar
	 * in a scroll pane.
	 */
	public enum VBarPos {
		RIGHT, LEFT;

		/**
		 * Converts this enumeration to an {@link HPos} object.
		 */
		public HPos toHPos() {
			return (this == LEFT) ? HPos.LEFT : HPos.RIGHT;
		}
	}

	/**
	 * Enumeration to specify the position of the horizontal scroll bar
	 * in a scroll pane.
	 */
	public enum HBarPos {
		BOTTOM, TOP;

		/**
		 * Converts this enumeration to a {@link VPos} object.
		 */
		public VPos toVPos() {
			return (this == TOP) ? VPos.TOP : VPos.BOTTOM;
		}
	}
}
