package com.smapview.core.map;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.smapview.core.map.NodePool.PoolMode;

public class MapFileTest {
		
	@TempDir
    Path tempDir;
	
	Random random = new Random();
	
	@Test
	public void testCreateMap() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapFile map = new MapFile(path.toFile());
		map.close();
		printMapFolderSize();
	}

	@Test
	public void testEmptyPart() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapFile map = new MapFile(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.openPool("/mypart", PoolMode.CREATE_PART);
		pool.merge();
		pool.close();
		view.close();
		map.close();
		printMapFolderSize();
	}

	@Test
	public void testBuildSchema() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapFile map = new MapFile(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.openPool("/mybooks", PoolMode.UPDATE_PART);
		MapSchema schema = pool.getSchema();
		schema.builder()
			.type("Library", "libraries")
			.type("Author", "Library.authors")
			.type("Book", "Library.books").link("author", "Author");
		// use NodeBuilder to build
		pool.merge();
		pool.close();
		view.close();
		map.close();
		printMapFolderSize();
	}

	@Test
	public void testAddNodes() throws Exception {
		Path path = Files.createFile(tempDir.resolve("test.smap"));
		MapFile map = new MapFile(path.toFile());
		MapView view = new MapView(map);
		NodePool pool = view.openPool("/mybooks", PoolMode.UPDATE_PART);
		pool.builder()
			.folder("libraries")
				.newNode("Library", "My library")
				.set("location", "At home")
				.folder("authors")
					.newNode("Author", "James Dawn")
					.add()
				.folder("books")
					.newNode("Book", "The Grey Pinguin")
					.link("author").toNode("authors/James Dawn")
					.add()
				.add();
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
