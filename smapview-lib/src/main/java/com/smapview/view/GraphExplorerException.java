package com.smapview.view;

public class GraphExplorerException extends ViewUpdateException {

	private static final long serialVersionUID = 2709167987486307409L;

	public GraphExplorerException() {
		super("Failed to explore view graph");
	}

}
