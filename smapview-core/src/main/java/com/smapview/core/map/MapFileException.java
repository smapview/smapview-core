package com.smapview.core.map;

public class MapFileException extends Exception {

	private static final long serialVersionUID = -2374650739154181272L;

	public MapFileException(String message) {
		super(message);
	}

	public MapFileException(Throwable cause) {
		super(cause);
	}

	public MapFileException(String message, Throwable cause) {
		super(message, cause);
	}

}
