package com.smapview.core.map;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

class MapRegistry {
	
	class RegistryWriter {
		
		final DatabaseConnection dbc;
		
		RegistryWriter(DatabaseConnection dbc) {
			this.dbc = dbc;
		}
		
		void insert(RegistryEntry entry) throws MapDatabaseException {
			// TODO complete this
		}

		void update(RegistryEntry entry) throws MapDatabaseException {
			// TODO complete this
		}
		
		void put(String key, String value) throws MapDatabaseException {
			// TODO complete this
		}

		public void setSchema(MapSchema newSchema) throws MapDatabaseException {
			for (NodeType type : newSchema.listTypes()) {
				// case of a new type
				if (schema.getType(type.name) == null) {
					insert(type.toRegistryEntry());
				}
				// case of a modified type
				else if (type.modified) update(type.toRegistryEntry());
			}
			newSchema.publish();
			schema = newSchema;
		}

	}
	
	class RegistryReader {
		
		final DatabaseConnection dbc;
		
		RegistryReader(DatabaseConnection dbc) {
			this.dbc = dbc;
		}

		RegistryEntry selectKey(String key) throws MapDatabaseException {
			try (PreparedStatement pst = dbc.prepare(
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
				throw new MapDatabaseException(e);
			}
		}

		List<RegistryEntry> scanKeys(String keyPrefix) throws MapDatabaseException {
			List<RegistryEntry> result = new ArrayList<>();
			try (PreparedStatement pst = dbc.prepare(
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
				throw new MapDatabaseException(e);
			}
			return result;
		}
		
		static JsonValue toValue(String json) {
			return Json.createReader(new StringReader(json))
					.readValue();
		}
		
		String get(String key) throws MapDatabaseException {
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

	private MapSchema schema;
	
	void load(DatabaseConnection dbc) throws MapDatabaseException {
		schema = new MapSchema(newReader(dbc));
		schema.publish();
	}
	
	RegistryReader newReader(DatabaseConnection dbc) {
		return new RegistryReader(dbc);
	}

	RegistryWriter newWriter(DatabaseConnection dbc) {
		return new RegistryWriter(dbc);
	}
	
	MapSchema getSchema() {
		return schema;
	}
		
}
