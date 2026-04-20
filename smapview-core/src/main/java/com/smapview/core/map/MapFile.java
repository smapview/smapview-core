package com.smapview.core.map;

import java.time.OffsetDateTime;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class MapFile implements AutoCloseable {
				
	class SelectResult {
		
		final ResultSet set;
		
		int column;
		
		SelectResult(ResultSet set) {
			this.set = set;
		}
		
		boolean nextRow() throws SQLException {
			try {
				return set.next();
			}
			finally {
				column = 0;
			}
		}
		
		String getString() throws SQLException {
			return set.getString(++column);
		}

		Instant getTime() throws SQLException {
			return ((OffsetDateTime)set.getObject(++column)).toInstant();
		}

		int getInt(int defaultValue) throws SQLException {
			Long value = set.getLong(++column) ;
			return value!=null? value.intValue() : defaultValue;
		}

		long getLong(long defaultValue) throws SQLException {
			Long value = set.getLong(++column) ;
			return value!=null? value : defaultValue;
		}

	}
	
	private static final String DB_USER = "smap";
	
	final ReentrantReadWriteLock viewLock = new ReentrantReadWriteLock();

	final ReentrantLock poolLock = new ReentrantLock();

	private final File file;
	
	final JdbcConnectionPool connectPool;
	
	final MapRegistry registry;
			
				
	/**
	 * Opens a map file.
	 * 
	 * @param file The file containing map data.
	 * 
	 * @throws MapFileException if the file cannot be accessed or does not contain appropriate data.
	 */
	public MapFile(File file) throws MapFileException {
		this.file = file;
		this.connectPool = JdbcConnectionPool.create("jdbc:h2:"+file.getAbsolutePath(), DB_USER, "");
		try (MapFileLoader loader = new MapFileLoader(this)) {
			this.registry = new MapRegistry();
			loader.init();
		}
	}


	/**
	 * Closes this map file.
	 * This method waits for all views and current build pool to be closed.
	 * 
	 * @throws MapFileException If the map cannot be closed.
	 * @throws InterruptedException If interrupted while waiting for views and pool.
	 */
	synchronized public void close() throws MapFileException, InterruptedException {
		while (connectPool.getActiveConnections()>0) {
			Thread.sleep(1000);
		}
		connectPool.dispose();
	}
					
	public File getFile() {
		return file;
	}
	
}
