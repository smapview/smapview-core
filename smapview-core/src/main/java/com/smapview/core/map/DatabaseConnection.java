package com.smapview.core.map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class DatabaseConnection implements AutoCloseable {

	final MapDatabase db;
	
	final Connection jdbc;

	DatabaseConnection(MapDatabase db) throws MapDatabaseException {
		try {
			this.db = db;
			this.jdbc = db.connectPool.getConnection();
		} catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}
	
	private void setParams(PreparedStatement pst, Object... params) throws SQLException {
		for (int i=0; i<params.length; i++) {
			Object value = params[i];
			if (value==null) throw new IllegalArgumentException();
			else if (value instanceof String) pst.setString(i+1, (String)value);
			else if (value instanceof Integer) pst.setInt(i+1, (Integer)value);
			else if (value instanceof Short) pst.setShort(i+1, (Short)value);
		}		
	}
	
	PreparedStatement prepare(String sql, Object... params) throws MapDatabaseException {
		try {
			PreparedStatement pst = jdbc.prepareStatement(sql);
			setParams(pst, params);
			return pst;
		} catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}

	void exec(String sql, Object... params) throws MapDatabaseException {
		try (PreparedStatement pst = prepare(sql, params)) {
			pst.execute();
		} 
		catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}
	
	int execUpdate(String sql, Object... params) throws MapDatabaseException {
		try (PreparedStatement pst = prepare(sql, params)) {
			pst.execute();
			return pst.getUpdateCount();
		}
		catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}

	public void close() throws MapDatabaseException {
		try {
			jdbc.close();
		} catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}
	
}
