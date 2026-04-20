package com.smapview.core.map;

public abstract class SchemaElement {

	final MapSchema schema;
	
	final String name;
	
	SchemaElement(MapSchema schema, String name) {
		this.schema = schema;
		this.name = name;
	}
	
	abstract String getTypeCN();
	
	abstract void compile() throws SchemaBuildException;

}
