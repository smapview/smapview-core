package com.smapview.core.map;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MapDatabaseTest {
		
	@TempDir
    Path tempDir;
	
	Random random = new Random();
	
	@Test
	public void testCreateMap() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapDatabase map = new MapDatabase(path.toFile());
		map.close();
		printMapFolderSize();
	}

	@Test
	public void testEmptyPart() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapDatabase map = new MapDatabase(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.createPool("MySource");
		pool.merge();
		pool.close();
		view.close();
		map.close();
		printMapFolderSize();
	}

	@Test
	public void testBuildSchema() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapDatabase map = new MapDatabase(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.createPool("MySource");
		buildCarRentalSchema(pool.getSchemaBuilder());
		pool.merge();
		pool.close();
		view.close();
		map.close();
		printMapFolderSize();
	}
	
	void buildCarRentalSchema(SchemaBuilder builder) throws SchemaBuildException {
		builder.type("CarModel", "/models")
			.type("Agency", "/agencies")
			.type("Car", "-/cars")
			.link("model", "CarModel")
			.build();		
	}

	@Test
	public void testAddNodes() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapDatabase map = new MapDatabase(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.createPool("MySource");
		buildCarRentalSchema(pool.getSchemaBuilder());
		pool.newNodeBuilder("/models/Economy").build();
		pool.newNodeBuilder("/models/Compact").build();
		pool.newNodeBuilder("/models/Premium").build();
		pool.newNodeBuilder("/agencies/Paris")
			.addNode("Car", "FR-911-GZ")
			.linkNode("model", "/models/Economy")
			.addNode("Car", "FR-316-NF")
			.linkNode("model", "/models/Compact")
			.build();
		pool.merge();
		pool.close();
		view.close();
		map.close();
		printMapFolderSize();
	}

	void printMapFolderSize() {
		System.out.format("Map folder size: %d\n", getFolderSize(tempDir.toFile()));
	}
	
	long getFolderSize(File folder) {
	    long length = 0;
	    File[] files = folder.listFiles();
	    int count = files.length;
	    for (int i = 0; i < count; i++) {
	        if (files[i].isFile()) {
	            length += files[i].length();
	        }
	        else {
	            length += getFolderSize(files[i]);
	        }
	    }
	    return length;
	}
	
}
