package com.smapview.view;

enum FieldTag {

	/**
	 * Marks a node identifier field (GraphQL ID type).
	 */
	ID(0x0001),
	
	/**
	 * Marks a string field (GraphQL String type).
	 */
	STRING(0x0002),
	
	/**
	 * Marks a date-and-time field (GraphQL DateTime type).
	 */
	DATE_TIME(0x0004),
	
	/**
	 * Marks a list field (GraphQL list type).
	 */
	LIST(0x0010),
	
	/**
	 * Marks a mandatory field (GraphQL non-null type).
	 */
	MANDATORY(0x0020),
	
	/**
	 * Marks a field to be used as path to other nodes in view graphs.
	 */
	PATH(0x0100),
	
	/**
	 * Marks a link field used in a link strategy.
	 */
	LINK(0x0200),
	
	/**
	 * Marks a reverse link or path field.
	 */
	REVERSE(0x0400),
		
	/**
	 * Marks a node pointer field.
	 * <p>
	 * Pointer values are provided in node data during graph updates to uniquely identify 
	 * child nodes from their parent node and path field.   
	 */
	POINTER(0x1000),

	/**
	 * Marks a node timestamp field.
	 * <p>
	 * Node timestamps are provided in node data during graph updates to indicate when
	 * the node data was changed in the data source.
	 */
	TIMESTAMP(0x2000);
	
	final int value;
	
	FieldTag(int value) {
		this.value = value;
	}

}
