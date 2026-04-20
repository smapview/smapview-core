package com.smapview.core.map;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

class MapRegistry {
	
	class RegistryWriter {
		
		final MapFileConnection mfc;
		
		RegistryWriter(MapFileConnection mfc) {
			this.mfc = mfc;
		}
		
		void insert(RegistryEntry entry) throws MapFileException {
			// TODO complete this
		}

		void update(RegistryEntry entry) throws MapFileException {
			// TODO complete this
		}
		
		void put(String key, String value) throws MapFileException {
			// TODO complete this
		}

		public void setSchema(MapSchema newSchema) throws MapFileException {
			for (NodeType type : newSchema.listNodeTypes()) {
				// case of a new type
				if (schema.getType(type.name) == null) {
					insert(type.toRegistryEntry());
				}
				// case of a modified type
				else if (type.modified) update(type.toRegistryEntry());
			}
			schema = newSchema;
		}

	}
	
	class RegistryReader {
		
		final MapFileConnection mfc;
		
		RegistryReader(MapFileConnection mfc) {
			this.mfc = mfc;
		}

		RegistryEntry selectKey(String key) throws MapFileException {
			try (PreparedStatement pst = mfc.prepare(
					"select EVAL from REGISTRY where EKEY = ?",
					key)) 
			{
				pst.execute();
				ResultSet set = pst.getResultSet();
				while (set.next()) {
					return new RegistryEntry(key, toValue(set.getString(1)));
				}
				return null;
			} catch (SQLException e) {
				throw new MapFileException(e);
			}
		}

		List<RegistryEntry> scanKeys(String keyPrefix) throws MapFileException {
			List<RegistryEntry> result = new ArrayList<>();
			try (PreparedStatement pst = mfc.prepare(
					"select EKEY, EVAL from REGISTRY where EVAL like ?",
					keyPrefix + "%")) 
			{
				pst.execute();
				ResultSet set = pst.getResultSet();
				while (set.next()) {
					String key = set.getString(1);
					JsonValue value = toValue(set.getString(2));
					result.add(new RegistryEntry(key, value)); 
				}
			} catch (SQLException e) {
				throw new MapFileException(e);
			}
			return result;
		}
		
		static JsonValue toValue(String json) {
			return Json.createReader(new StringReader(json))
					.readValue();
		}
		
		String get(String key) throws MapFileException {
			RegistryEntry entry = selectKey(key);
			return entry != null? ((JsonString)entry.value).getString() : null;
		}

	}
		
	static class RegistryEntry {
		
		final String key;
		
		final JsonValue value;

		RegistryEntry(String key, JsonValue value) {
			this.key = key;
			this.value = value;
		}
		
		JsonObject getObject() {
			return value.asJsonObject();
		}

		String getString() {
			return ((JsonString)value).getString();
		}

	}

	private Map<String,MapPart> parts = new TreeMap<>();

	private MapSchema schema;
	
	void load(MapFileConnection mfc) throws MapFileException {
		schema = new MapSchema(newReader(mfc));
		MapPart.readParts(newReader(mfc)).stream().forEach(p -> add(p));
	}
	
	void add(MapPart part) {
		parts.put(part.path, part);
		parts.put(part.alias, part);
	}

	RegistryReader newReader(MapFileConnection mfc) {
		return new RegistryReader(mfc);
	}

	RegistryWriter newWriter(MapFileConnection mfc) {
		return new RegistryWriter(mfc);
	}
	
	MapSchema getSchema() {
		return schema;
	}
	
	MapPart getPart(String partPath) {
		return parts.get(partPath);
	}
	
}
