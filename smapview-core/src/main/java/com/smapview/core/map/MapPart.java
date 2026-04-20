package com.smapview.core.map;

import java.util.ArrayList;
import java.util.List;

import com.smapview.core.map.MapRegistry.RegistryEntry;
import com.smapview.core.map.MapRegistry.RegistryReader;

import jakarta.json.JsonObject;

public class MapPart {

	static final String REGISTRY_KEY_PREFIX = "map.parts.";
	
	final String path;
	
	final String alias;
	
	int poolId;

	MapPart(String path, String alias) {
		this.path = path;
		this.alias = alias;
	}
	
	MapPart(RegistryEntry entry) {
		JsonObject json = entry.getObject();
		this.path = json.getString("path");
		this.alias = json.getString("alias");
	}
	
	RegistryEntry toRegistryEntry() {
		// TODO complete this
		return null;
	}
	
	static List<MapPart> readParts(RegistryReader reader) throws MapFileException {
		List<MapPart> result = new ArrayList<>();
		for (RegistryEntry e : reader.scanKeys(MapPart.REGISTRY_KEY_PREFIX)) { 
			result.add(new MapPart(e));
		}
		return result;
	}
	
}
