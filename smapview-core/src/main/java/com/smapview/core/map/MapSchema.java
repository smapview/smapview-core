package com.smapview.core.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.smapview.core.map.MapRegistry.RegistryEntry;
import com.smapview.core.map.MapRegistry.RegistryReader;
import com.smapview.core.map.NodeType.TypeField;

public class MapSchema {
		
	static final Pattern FIELD_NAME_FORMAT = Pattern.compile("[a-z][a-zA-Z0-9]+"); 

	final Map<String,SchemaElement> map = new HashMap<>();
	
	SchemaBuilder builder;

	MapSchema(RegistryReader reader) throws MapFileException {
		this.builder = null;
		for (RegistryEntry e : reader.scanKeys(NodeType.REGISTRY_KEY_PREFIX)) {
			new NodeType(this, e);
		}
		rebuild();
	}
	
	MapSchema(MapSchema base, NodePool buildPool) {
		this.builder = new SchemaBuilder(buildPool, this);
		listNodeTypes().forEach(t -> {
			try {
				new NodeType(this, t.toRegistryEntry());
			} catch (MapFileException e) {
				// above should not give such an exception
				// as we simply recreate each type
				// from itself for the new schema
				throw new Error(e);
			}
		});
		rebuild();
	}
		
	void declare(SchemaElement element, String key, String keyTypeCN) {
		if (map.containsKey(key)) {
			throw new IllegalArgumentException(String.format(
					"Already exists %s with %s %s",
					element.getTypeCN(), keyTypeCN, key));
		}
		map.put(key, element);				
	}
	
	void declare(MapFolder folder) {
		if (folder.isRootFolder()) {
			declare(folder, folder.name, "name");
		}
		declare(folder, "/" + folder.alias, "path alias");
	}
	
	void declare(NodeType type) {
		declare(type, type.name, "name");
	}

	void declare(TypeField field) {
		declare(field, field.name, "name");
	}

	void build() throws SchemaBuildException {
		HashSet<SchemaElement> elements = new HashSet<>();
		map.values().forEach(e -> elements.add(e));
		for (SchemaElement e : elements) e.compile();
	}
	
	void rebuild() {
		try {
			build();
		}
		catch (SchemaBuildException e) {
			throw new RuntimeException(e);
		}
	}

	NodeType getType(String typeName) {
		Object result = map.get(typeName);
		if (result != null && result instanceof NodeType type) {
			return type;
		}
		else return null;
	}

	NodeType safeGetType(String typeName) {
		NodeType result = getType(typeName);
		if (result != null) return result; 
		else throw new IllegalArgumentException("Unknown type: "+typeName);
	}

	NodeType expectDeclaredType(String typeName) throws SchemaBuildException {
		NodeType result = getType(typeName);
		if (result != null) return result; 
		else throw new SchemaBuildException("Unknown type: "+typeName);
	}

	TypeField getField(String fieldName) {
		Object result = map.get(fieldName);
		if (result != null && result instanceof TypeField field) {
			return field;
		}
		else return null;
	}

	TypeField expectDeclaredField(String fieldName) throws SchemaBuildException {
		TypeField result = getField(fieldName);
		if (result != null) return result; 
		else throw new SchemaBuildException("Unknown field: "+fieldName);
	}

	static void checkFormat(String str, Pattern format) {
		if (!format.matcher(str).matches()) throw new IllegalArgumentException();
	}
	
	List<NodeType> listNodeTypes() {
		List<NodeType> result = new ArrayList<>();
		map.values().stream()
		.filter(o -> o instanceof NodeType)
		.forEach(o -> result.add((NodeType)o));
		return result;
	}

	void checkCanModify() {
		if (builder == null) throw new UnsupportedOperationException();
	}
			
	public SchemaBuilder builder() {
		return builder;
	}
		
}
