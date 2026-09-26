package com.smapview.view;

import jakarta.json.JsonArray;

public class ViewRequestException extends Exception {

	private static final long serialVersionUID = 1015050545126504032L;

	ViewRequestException(String msg) {
		super(msg);
	}

	ViewRequestException(Throwable cause) {
		super(cause);
	}

	ViewRequestException(JsonArray errors) {
		super(toExceptionMessage(errors));
	}
	
	static String toExceptionMessage(JsonArray errors) {
		try {
			return "Request returned an error: " + 
					errors.getJsonObject(0).getString("message");
		}
		catch (Exception e) {
			return "Request returned " + errors.size() + " error(s)";
		}
	}

}
