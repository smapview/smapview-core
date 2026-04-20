package com.smapview.core.map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class MapFileConnection implements AutoCloseable {

	final MapFile file;
	
	final Connection dbc;

	MapFileConnection(MapFile file) throws MapFileException {
		try {
			this.file = file;
			this.dbc = file.connectPool.getConnection();
		} catch (SQLException e) {
			throw new MapFileException(e);
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
	
	PreparedStatement prepare(String sql, Object... params) throws MapFileException {
		try {
			PreparedStatement pst = dbc.prepareStatement(sql);
			setParams(pst, params);
			return pst;
		} catch (SQLException e) {
			throw new MapFileException(e);
		}
	}

	void exec(String sql, Object... params) throws MapFileException {
		try (PreparedStatement pst = prepare(sql, params)) {
			pst.execute();
		} 
		catch (SQLException e) {
			throw new MapFileException(e);
		}
	}
	
	int execUpdate(String sql, Object... params) throws MapFileException {
		try (PreparedStatement pst = prepare(sql, params)) {
			pst.execute();
			return pst.getUpdateCount();
		}
		catch (SQLException e) {
			throw new MapFileException(e);
		}
	}

	public void close() throws MapFileException {
		try {
			dbc.close();
		} catch (SQLException e) {
			throw new MapFileException(e);
		}
	}
	
}
