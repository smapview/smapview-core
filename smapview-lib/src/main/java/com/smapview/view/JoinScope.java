package com.smapview.view;

public enum JoinScope {

	/** 
	 * Joins nodes within the same view graph (same root). 
	 */
	GRAPH,
	
	/**
	 * Joins nodes (in any graph) based on their named link spaces.
	 */
	SPACES;

}
