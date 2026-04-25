package com.smapview.core.map;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Provides GraphQL access to a map.
 */
public class MapGraph {
	
	final String schemaDoc;
	
	final TypeDefinitionRegistry types;

	MapGraph(MapSchema schema) {
		this.schemaDoc = buildSchemaDoc(schema);
		this.types = new SchemaParser().parse(schemaDoc);
	}
		
	String buildSchemaDoc(MapSchema schema) {
		// TODO complete this (build GraphQL schema based on node types)
		return null;
	}

	public String getSchemaDoc() {
		return schemaDoc;
	}
	
}
