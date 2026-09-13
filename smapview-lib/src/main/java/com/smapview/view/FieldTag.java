package com.smapview.view;

enum FieldTag {

	/**
	 * Marks a node identifier field.
	 * <p>
	 * Node identifier fields are automatically tagged based on GraphQL introspection.
	 * They must be of GraphQL ID type and declared as non-null in the GraphQL schema.
	 */
	ID,
	
	/**
	 * Marks a field to be used as path to other nodes in view graphs.
	 * 
	 * @see View#tagPathField(String, String)
	 */
	PATH,
	
	/**
	 * Marks a join field used in a link strategy.
	 * 
	 * Fields get automatically marked with this tag through calls to 
	 * {@link View#addLinkStrategy(String, String, String, String, LinkScope)
	 */
	JOIN,
	
	/**
	 * Marks a link field used in a link strategy.
	 * 
	 * Fields get automatically marked with this tag through calls to 
	 * {@link View#addLinkStrategy(String, String, String, String, LinkScope)
	 */
	LINK,
	
	/**
	 * Marks a node pointer field.
	 * <p>
	 * Pointer values are provided in node data during graph updates to uniquely identify 
	 * child nodes from their parent node and path field.   
	 *  
	 * @see View#tagPointerField(String)
	 */
	POINTER,

	/**
	 * Marks a node timestamp field.
	 * <p>
	 * Node timestamps are provided in node data during graph updates to indicate when
	 * the node data was changed in the data source.
	 *  
	 * @see View#tagTimestampField(String)
	 */
	TIMESTAMP

}
