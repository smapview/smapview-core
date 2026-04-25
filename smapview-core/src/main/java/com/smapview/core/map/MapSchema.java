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
import com.smapview.core.util.RegularPattern;

public class MapSchema {
		
	static class FieldKey {
		
		final NodeType nodeType;
		
		final String fieldName;
		
		public FieldKey(NodeType nodeType, String fieldName) {
			super();
			this.nodeType = nodeType;
			this.fieldName = fieldName;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof FieldKey key) {
				return key.nodeType == this.nodeType
						&& key.fieldName.equals(this.fieldName);
			}
			else return false;
		}
		
		@Override
		public int hashCode() {
			int hash = 7;
		    hash = 31 * hash + nodeType.hashCode();
		    hash = 31 * hash + fieldName.hashCode();
		    return hash; 
		}

	}
	
	static final RegularPattern FIELD_QUALIFIED_NAME_PATTERN = 
			new RegularPattern("([A-Z][a-zA-Z0-9]+)\\.([a-z][a-zA-Z0-9]+)");

	final Map<Object,SchemaElement> map = new HashMap<>();
	
	private SchemaBuilder builder;
	
	private MapGraph graph;

	MapSchema(RegistryReader reader) throws MapDatabaseException {
		this.builder = null;
		for (RegistryEntry e : reader.scanKeys(NodeType.REGISTRY_KEY_PREFIX)) {
			new NodeType(this, e);
		}
		rebuild();
	}
	
	MapSchema(MapSchema base, NodePool buildPool) {
		this.builder = new SchemaBuilder(buildPool, this);
		listTypes().forEach(t -> {
			try {
				new NodeType(this, t.toRegistryEntry());
			} catch (MapDatabaseException e) {
				// above should not give such an exception
				// as we simply recreate each type
				// from itself for the new schema
				throw new Error(e);
			}
		});
		rebuild();
	}
	
	SchemaBuilder getBuilder() {
		return builder;
	}
		
	void declare(SchemaElement element, Object key, String keyType) {
		if (map.containsKey(key)) {
			throw new IllegalArgumentException(String.format(
					"Already exists %s with %s %s",
					element.getTypeCN(), keyType, key));
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
		declare(field, field.type.name + "." + field.name, "qualified name");
		declare(field, new FieldKey(field.type, field.name), "field key");
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

	public NodeType getType(String typeName) {
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

	TypeField getField(NodeType nodeType, String fieldName) {
		Object result = map.get(new FieldKey(nodeType, fieldName));
		if (result != null && result instanceof TypeField field) {
			return field;
		}
		else return null;
	}

	TypeField getField(String qualifiedName) {
		Object result = map.get(qualifiedName);
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
	
	public List<NodeType> listTypes() {
		List<NodeType> result = new ArrayList<>();
		map.values().stream()
		.filter(o -> o instanceof NodeType)
		.forEach(o -> result.add((NodeType)o));
		return result;
	}

	void checkCanModify() {
		if (builder == null) throw new UnsupportedOperationException();
	}
			
	String toShortPath(String fullPath) {
		// TODO complete this
		return null;
	}
	
	String toFullPath(String shortPath) {
		// TODO complete this
		return null;
	}
	
	/**
	 * Gets the node type of a given node short path.
	 * 
	 * @param shortPath  The node short path.
	 * 
	 * @return The node type of the node with the specified short path. 
	 */
	NodeType getShortPathType(String shortPath) {
		// TODO complete this
		return null;
	}
	
	void publish() {
		builder = null;
		graph = new MapGraph(this);
	}
	
	public MapGraph getGraph() {
		return graph;
	}
		
}
