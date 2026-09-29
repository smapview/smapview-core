package com.smapview.view;

enum FieldTag {

	/**
	 * Marks a node identifier field (GraphQL ID type).
	 */
	ID(0x00001),
	
	/**
	 * Marks a string field (GraphQL String type).
	 */
	ENUM(0x00002),
	
	/**
	 * Marks a date-and-time field (GraphQL DateTime type).
	 */
	STRING(0x00004),

	/**
	 * Marks a date-and-time field (GraphQL DateTime type).
	 */
	DATE_TIME(0x00008),

	/**
	 * Marks a list field (GraphQL list type).
	 */
	LIST(0x00100),
	
	/**
	 * Marks a mandatory field (GraphQL non-null type).
	 */
	MANDATORY(0x00200),
	
	/**
	 * Marks a field to be used as path to other nodes in view graphs.
	 */
	PATH(0x01000),
	
	/**
	 * Marks a link field used in a link strategy.
	 */
	LINK(0x02000),
	
	/**
	 * Marks a reverse link or path field.
	 */
	REVERSE(0x04000),
		
	/**
	 * Marks a node pointer field.
	 * <p>
	 * Pointer values are provided in node data during graph updates to uniquely identify 
	 * child nodes from their parent node and path field.   
	 */
	POINTER(0x10000),

	/**
	 * Marks a node timestamp field.
	 * <p>
	 * Node timestamps are provided in node data during graph updates to indicate when
	 * the node data was changed in the data source.
	 */
	TIMESTAMP(0x20000);
	
	final int value;
	
	FieldTag(int value) {
		this.value = value;
	}

}
