package com.smapview.core.map;

import java.util.ArrayList;
import java.util.List;

import com.smapview.core.map.MapRegistry.RegistryEntry;
import com.smapview.core.util.RegularPattern;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public class NodeType extends SchemaElement {

	enum ValueType {

		BOOLEAN,
		
		INTEGER,
		
		FLOAT,
		
		STRING,
		
		NODE,
		
		LIST;
		
	}
	
	enum LinkType {
		
		SINGLE_ALWAYS("1-1"),
		
		SINGLE_CASUAL("0-1"),
		
		MULTIPLE("0-N");
		
		final String abbrev;
		
		private LinkType(String abbrev) {
			this.abbrev = abbrev;
		}
		
		static LinkType fromAbbrev(String abbrev) {
			for (LinkType type : LinkType.values()) {
				if (type.abbrev.equals(abbrev)) return type;
			}
			return null;
		}

	}
	
	abstract static public class TypeField extends SchemaElement {
		
		final NodeType type;
		
		final String localName;
		
		public TypeField(NodeType type, String localName) {
			super(type.schema, type.name + "." + localName);
			this.type = type;
			this.localName = localName;
			FIELD_LOCAL_NAME_PATTERN.check(localName);
			type.fieldList.add(this);
			schema.declare(this);
		}

		public TypeField(NodeType type, JsonObject json) {
			this(type, json.getString("localName"));
		}

		@Override
		String getTypeCN() {
			return "type field";
		}

	}
	
	static public class ValueField extends TypeField {
		
		final ValueType baseType;
		
		final ValueType itemType;
		
		final int dataIndex;
		
		ValueField(NodeType type, String name, ValueType baseType, ValueType itemType) {
			super(type, name);
			if (baseType==null) throw new IllegalArgumentException();
			if (baseType==ValueType.LIST && itemType==null) throw new IllegalArgumentException();
			this.baseType = baseType;
			this.itemType = itemType;
			this.dataIndex = type.dataLength++;	
		}

		@Override
		void compile() throws SchemaBuildException {}

	}
	
	static public class LinkField extends TypeField {
		
		final int linkId;

		final String targetType;
		
		private LinkType linkType;
				
		LinkField(NodeType type, String name, int linkId, String targetType) {
			super(type, name);
			this.linkId = linkId;
			this.targetType = targetType;
		}
		
		void setLinkType(LinkType type) {
			if (type == null) throw new IllegalArgumentException();
			else if (linkType == type) ;
			else if (linkType == null) linkType = type;
			else switch (linkType) {
			case MULTIPLE:
				// multiple cannot be refined
				throw new IllegalArgumentException();
			case SINGLE_CASUAL:
				// can be refined to multiple
				if (type == LinkType.SINGLE_ALWAYS) {
					throw new IllegalArgumentException();
				}
			case SINGLE_ALWAYS:
				// can be refined to single-casual or multiple
				linkType = type;
				break;
			}
		}
		
		@Override
		void compile() throws SchemaBuildException {
			schema.expectDeclaredType(targetType);
		}

	}
	
	public class FolderField extends LinkField {
		
		final MapFolder folder;

		FolderField(NodeType type, String name, int linkId, String contentType, String folderAlias) {
			super(type, name, linkId, contentType);
			this.folder = new MapFolder(schema, folderAlias, this);
			schema.declare(folder);
		}

	}
	
	static final RegularPattern TYPE_NAME_PATTERN = new RegularPattern("[A-Z][a-zA-Z0-9]+"); 

	static final RegularPattern FIELD_LOCAL_NAME_PATTERN = new RegularPattern("[a-z][a-zA-Z0-9]+");

	static final String REGISTRY_KEY_PREFIX = "map.types.";
	
	final List<TypeField> fieldList = new ArrayList<>(12);

	final String folderField;
	
	MapFolder folder;
	
	int dataLength = 0;
	
	boolean modified = false; 
	
	private NodeType(MapSchema schema, String typeName, String folderField) {
		super(schema, typeName);
		this.folderField = folderField;
		TYPE_NAME_PATTERN.check(name);
		schema.declare(this);
	}
				
	/**
	 * Creates a primary node type with a root folder.
	 */
	NodeType(MapSchema schema, String typeName, String folderName, String folderAlias) {
		this(schema, typeName, (String)null);
		this.folder = new MapFolder(schema, folderAlias, folderName, this);
	}

	/**
	 * Creates a primary node type with a field folder.
	 */
	NodeType(MapSchema schema, String typeName, FolderField field) {
		this(schema, typeName, field.name);
	}

	/**
	 * Creates a secondary node type.
	 */
	NodeType(MapSchema schema, String typeName) {
		this(schema, typeName, (String)null);
	}
	
	NodeType(MapSchema schema, RegistryEntry entry) throws MapFileException {
		super(schema, entry.key.substring(REGISTRY_KEY_PREFIX.length()));
		JsonObject json = entry.getObject();
		boolean primary = json.getBoolean("primary");
		if (primary) {
			this.folderField = json.getString("folderField", null);
			if (folderField == null) {
				this.folder = new MapFolder(schema,
						json.getString("folderAlias"),
						json.getString("folderName"),
						this);
			}
		}
		else {
			this.folderField = null;
		}
		for (JsonObject fieldJson : json.getJsonArray("fields")
				.getValuesAs(JsonObject.class))
		{
			toField(fieldJson);
		}
	}

	void checkCanModify() {
		schema.checkCanModify();
		modified = true;
	}

	@Override
	void compile() throws SchemaBuildException {
		if (folder == null && folderField != null) {
			TypeField field = schema.expectDeclaredField(folderField);
			if (field instanceof FolderField ff) {
				folder = ff.folder;
			}
			else throw new SchemaBuildException("Not a folder field: "+field.name);
		}
	}
	
	public boolean isPrimary() {
		return folder != null || folderField != null;
	}

	RegistryEntry toRegistryEntry() {
		JsonArrayBuilder fields = Json.createArrayBuilder();
		fieldList.stream().forEach(f -> fields.add(toJson(f)));
		JsonObjectBuilder value = Json.createObjectBuilder();
		if (folder != null) {
			value.add("folderName", folder.name)
			.add("folderAlias", folder.alias);
		}
		value.add("fields", fields);
		return new RegistryEntry(REGISTRY_KEY_PREFIX+name, value.build()); 
	}
	
	JsonObject toJson(TypeField field) {
		String fieldType = field instanceof ValueField? "value"
				: field instanceof LinkField? "link"
						: field instanceof FolderField? "folder" : null;
		if (fieldType == null) throw new IllegalArgumentException();
		JsonObjectBuilder json = Json.createObjectBuilder()
				.add("name", field.name)
				.add("type", fieldType);
		if (field instanceof ValueField value) {
			json.add("baseType", value.baseType.name());
			json.add("itemType", value.itemType != null? 
					value.itemType.name() : null);						
		}
		else if (field instanceof LinkField link) {
			json.add("linkId", link.linkId);
			if (field instanceof FolderField) {
				json.add("contentType", link.targetType);
				json.add("folderAlias", folder.alias);
			}
			else {
				json.add("linkType", link.linkType.abbrev);
				json.add("targetType", link.targetType);
			}
		}
		return json.build();
	}
	
	TypeField toField(JsonObject json) throws MapFileException {
		String name = json.getString("name");
		String fieldType = json.getString("fieldType");
		if (fieldType.equals("value")) {
			String itemType = json.getString("baseType", null);
			return new ValueField(this, name,
					ValueType.valueOf(json.getString("baseType")),
					itemType != null? ValueType.valueOf(itemType) : null);			
		}
		else if (fieldType.equals("link")) {
			return new LinkField(this, name,
					json.getInt("linkId"), json.getString("targetType"));
		}
		else if (fieldType.equals("folder")) {
			return new FolderField(this, name,
					json.getInt("linkId"), json.getString("contentType"),
					json.getString("folderAlias"));			
		}
		else throw new MapFileException("Invalid field type: "+fieldType);
	}

	public TypeField getField(String fieldName) {
		return schema.getField(name + "." + fieldName);
	}

	public LinkField getLinkField(String fieldName) {
		TypeField field = getField(fieldName);
		if (field instanceof LinkField lf) {
			return lf;
		}
		return null;
	}

	public FolderField getFolderField(String fieldName) {
		TypeField field = getField(fieldName);
		if (field instanceof FolderField ff) {
			return ff;
		}
		return null;
	}

	@Override
	String getTypeCN() {
		return "node type";
	}
			
}
