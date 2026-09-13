package com.smapview.view;

public class ViewUpdateException extends Exception {

	private static final long serialVersionUID = 1015050545126504032L;

	ViewUpdateException(String msg) {
		super(msg);
	}

	ViewUpdateException(Throwable cause) {
		super(cause);
	}

}
