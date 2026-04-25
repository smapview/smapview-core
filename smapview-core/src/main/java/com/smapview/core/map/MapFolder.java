package com.smapview.core.map;

import com.smapview.core.map.NodeType.FolderField;
import com.smapview.core.util.RegularPattern;

public class MapFolder extends SchemaElement {

	static final RegularPattern ROOT_FOLDER_NAME_PATTERN = 
			new RegularPattern("[a-z][a-zA-Z0-9]+"); 

	final String alias;
	
	final String contentType;
	
	final FolderField folderField;
	
	private MapFolder(MapSchema schema, String name, 
			String alias, String contentType, FolderField folderField) 
	{
		super(schema, name);
		this.alias = alias;
		this.contentType = contentType;
		this.folderField = folderField;
		if (isRootFolder()) ROOT_FOLDER_NAME_PATTERN.check(name);
		schema.declare(this);
	}

	/**
	 * Creates a root folder for a node type.
	 * 
	 * @param schema  The schema in which this folder is created.
	 * @param type    The content type for this folder.
	 */
	MapFolder(MapSchema schema, String alias, String name,  NodeType type) {
		this(schema, name, alias, type.name, (FolderField)null);
	}

	/**
	 * Creates a field folder for a folder field.
	 * 
	 * @param schema  The schema in which this folder is created.
	 * @param field   The folder field associated with this folder..
	 */
	MapFolder(MapSchema schema, String alias, FolderField field) {
		this(schema, field.name, alias, field.targetType, field);
	}

	boolean isRootFolder() {
		return folderField == null;
	}
	
	@Override
	String getTypeCN() {
		return "map folder";
	}

	@Override
	void compile() throws SchemaBuildException {
		schema.expectDeclaredType(contentType);
	}
	
}
