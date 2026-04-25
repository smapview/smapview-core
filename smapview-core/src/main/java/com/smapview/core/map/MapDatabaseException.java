package com.smapview.core.map;

public class MapDatabaseException extends Exception {

	private static final long serialVersionUID = -2374650739154181272L;

	public MapDatabaseException(String message) {
		super(message);
	}

	public MapDatabaseException(Throwable cause) {
		super(cause);
	}

	public MapDatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

}
