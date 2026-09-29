package com.smapview.script;

public class ScriptException extends Exception {

	private static final long serialVersionUID = -1461927512779338315L;

	public ScriptException(String message) {
		super(message);
	}

	public ScriptException(Throwable cause) {
		super(cause);
	}

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
