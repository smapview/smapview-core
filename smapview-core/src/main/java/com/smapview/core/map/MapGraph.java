package com.smapview.core.map;

import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

/**
 * Provides GraphQL access to a map.
 */
public class MapGraph {
	
	final MapView view;
		
	final String schemaDocument;
	
	final TypeDefinitionRegistry types;

	MapGraph(MapView view) {
		this.view = view;
		this.schemaDocument = buildSchemaDocument();
		this.types = new SchemaParser().parse(buildSchemaDocument());
	}
		
	String buildSchemaDocument() {
		// TODO complete this (build GraphQL schema based on node types)
		return null;
	}

	public String getSchemaDocument() {
		return schemaDocument;
	}
	
}
