package io.github.palexdev.virtualizedfx.enums;

/**
 * Enumeration to set the buffer size of Virtualized containers. To avoid the used abusing the 'dynamic' buffer system, by
 * setting unreasonably high numbers or 0, an enumeration is used instead.
 * <p>
 * The size of the buffer is given by the constant's ordinal + 1, {@link #val()}.
 */
public enum BufferSize {
	SMALL,
	MEDIUM,
	BIG,
	;

	/**
	 * @return the constant's {@link #ordinal()} + 1
	 */
	public int val() {
		return ordinal() + 1;
	}

	/**
	 * @return the standard, recommended buffer size, which is {@link #MEDIUM} (2)
	 */
	public static BufferSize standard() {
		return MEDIUM;
	}
}
