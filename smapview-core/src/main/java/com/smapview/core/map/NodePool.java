package com.smapview.core.map;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

import com.smapview.core.map.MapRegistry.RegistryReader;


/**
 * Used to update or replace part of a map.
 */
public class NodePool implements AutoCloseable {
	
	public enum PoolState {
		
		BUILD,
		
		MERGE,
		
		COMPLETE;
		
	}
		
	static final String POOL_STATE_KEY = "pool.state";
	
	final MapView view;
		
	final PoolStats stats;

	final MapSchema schema;
		
	PoolState state = PoolState.BUILD;
	
	NodePool(MapView view, String source) throws MapDatabaseException {
		this.view = view;
		this.stats = new PoolStats(newPoolId(), source);
		this.schema = new MapSchema(view.getSchema(), this);
		writePoolState();
		stats.createTime = new Date();
		stats.write(view.dbc);
	}
						
	void exec(String sql, Object... params) throws MapDatabaseException {
		view.dbc.exec(sql, params);
	}

	int execUpdate(String sql, Object... params) throws MapDatabaseException {
		return view.dbc.execUpdate(sql, params);
	}
	
	PreparedStatement prepare(String sql, Object... params) throws MapDatabaseException {
		return view.dbc.prepare(sql, params);
	}
	
	private int newPoolId() throws MapDatabaseException {
		// TODO complete this
		return 0;
	}
			
	/**
	 * Checks a node path refers to this pool map part. 
	 */
	void checkPoolPath(String path) {
		// TODO complete this
	}
	
	/**
	 * Identifies a view node from its path.
	 *  
	 * @param nodePath The view node path.
	 * 
	 * @throws MapDatabaseException If failing to read from or write to the database. 
	 */
	int getViewNodeId(String nodePath) throws MapDatabaseException {
		// TODO complete this using aliasNodePath()
		try (PreparedStatement pst = prepare(
				"select NODEID from VNODES where PATH = ?",
				nodePath)) 
		{
			pst.execute();
			ResultSet set = pst.getResultSet();
			while (set.next()) {
				return set.getInt(1);
			}
			return 0;
		} catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}

	/**
	 * Identifies a pool node from its path.
	 *  
	 * @param nodePath The view node path.
	 * 
	 * @throws MapDatabaseException If failing to read from or write to the database. 
	 */
	int getPoolNodeId(String nodePath) throws MapDatabaseException {
		// TODO complete this using aliasNodePath()
		try (PreparedStatement pst = prepare(
				"select NODEID from PNODES where PATH = ?",
				nodePath)) 
		{
			pst.execute();
			ResultSet set = pst.getResultSet();
			while (set.next()) {
				return set.getInt(1);
			}
			return 0;
		} catch (SQLException e) {
			throw new MapDatabaseException(e);
		}
	}

	/**
	 * Merges all nodes and links in this pool to the target map part.
	 * 
	 * @throws MapDatabaseException If failing to read from or write to the database. 
	 */
	public void merge() throws MapDatabaseException {
		checkState(PoolState.BUILD);
		setState(PoolState.MERGE);
		view.dbc.db.viewLock.readLock().unlock();
		view.dbc.db.viewLock.writeLock().lock();
		new PoolMerge(this).execute();
		view.dbc.db.viewLock.writeLock().unlock();
		view.dbc.db.viewLock.readLock().lock();
		setState(PoolState.COMPLETE);
	}
			

	@Override
	public void close() throws Exception {
		view.dbc.db.poolLock.unlock();
	}

	public PoolState getState() {
		return state;
	}
	
	void checkState(PoolState... states) {
		if (!Arrays.asList(states).contains(this.state)) {
			throw new IllegalStateException();
		}
	}

	private void setState(PoolState state) throws MapDatabaseException {
		this.state = state;
		writePoolState();
	}
	
	int newNodeId() {
		// TODO complete this
		return 0;		
	}

	int getNodeId(String shortPath) {
		// TODO complete this
		// allocate a new id if path cannot be found in the pool
		return 0;		
	}

	private void writePoolState() throws MapDatabaseException {
		view.getRegistry().newWriter(view.dbc).put(POOL_STATE_KEY, state.name());
	}
	
	static PoolState readPoolState(RegistryReader reader) throws MapDatabaseException {
		String state = reader.get(POOL_STATE_KEY);
		return state != null? PoolState.valueOf(state) : null;
	}
	
	static void cleanPoolTables(DatabaseConnection dbc) throws MapDatabaseException {
		// TODO complete this
	}

	public MapSchema getSchema() {
		return schema;
	}

	public SchemaBuilder getSchemaBuilder() {
		return schema.getBuilder();
	}

	/**
	 * Creates a new builder to create or replace a node.
	 * 
	 * This method 
	 * 
	 * @param nodePath  Path of the created or replaced node.
	 * 
	 * @return A new builder allowing to add node values, links and contained nodes.
	 * 
	 * @throws MapDatabaseException If failing to read from or write to the database. 
	 */
	public NodeBuilder newNodeBuilder(String nodePath) throws MapDatabaseException {
		checkState(PoolState.BUILD);
		// TODO check nodePath to make sure it has a parent!
		// TODO look into table VSOURCE to see if nodePath matches any row
		// TODO if VSOURCE matching row then check it has same source as this pool
		// TODO if no VSOURCE matching row then create one
		String shortPath = schema.toShortPath(nodePath);
		int count = execUpdate("insert into PNODES(NODEID, PATH) direct "
				+ "select NODEID, PATH from VNODES "
				+ "where PATH = ? or PATH like ?", shortPath, shortPath + "/%");
		if (count == 0) {
			execUpdate("insert into PNODES(NODEID, PATH) values (?, ?)",
					newNodeId(), nodePath);
		}
		return new NodeBuilder(this, nodePath);
	}
	
}
