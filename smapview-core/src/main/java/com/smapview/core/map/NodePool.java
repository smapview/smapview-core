package com.smapview.core.map;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.smapview.core.map.MapRegistry.RegistryReader;
import com.smapview.core.map.NodeType.LinkField;


/**
 * Used to update or replace part of a map.
 */
public class NodePool implements AutoCloseable {
	
	public enum PoolState {
		
		BUILD,
		
		MERGE,
		
		COMPLETE;
		
	}
	
	public enum PoolMode { 
		
		/** 
		 * Updates a map part by modifying existing nodes or adding new ones.
		 * If the updated map part contains nodes that are not in this pool,
		 * they are not modified and kept in the map.
		 */
		UPDATE_PART,
		
		
		/**
		 * Creates or replaces an entire map part.
		 * During pool merge, if the map part contains nodes that are not in this pool,
		 * they are removed from the map with their associated links. 
		 */
		CREATE_PART;
	
	}
	
	static final String POOL_STATE_KEY = "pool.state";
	
	final MapView view;
	
	final MapPart part;
	
	final PoolMode mode;
	
	final int poolId;

	final MapSchema schema;
		
	boolean permanent = false;
	
	PoolState state = PoolState.BUILD;
	
	NodeBuilder rootBuilder = new NodeBuilder(this, null);
	
	NodePool(MapView view, String partPath, PoolMode mode) throws MapFileException {
		this.view = view;
		this.part = getPart(partPath); 
		this.mode = mode;
		this.poolId = findPoolId();
		this.schema = new MapSchema(view.getSchema(), this);
		writePoolState();
	}
	
	MapPart getPart(String partPath) {
		// TODO complete this
		return null;
		
	}
	
	List<Integer> listPartPools() throws MapFileException {
		List<Integer> result = new ArrayList<>();
		try (PreparedStatement pst = prepare(
				"select POOLID from POOLS where PATH = ?",
				part.path)) 
		{
			pst.execute();
			ResultSet set = pst.getResultSet();
			while (set.next()) {
				result.add(set.getInt(1));
			}
		} catch (SQLException e) {
			throw new MapFileException(e);
		}
		return result;
	}
	
	void addToPoolsTable() throws MapFileException {
		if (mode == PoolMode.CREATE_PART) {
			execUpdate("delete from POOLS where PATH = ?",
					part.path);
		}
		execUpdate("insert into POOLS(POOLID, PATH) values (?, ?)",
				poolId, part.path);
	}
	
	void registerPart() throws MapFileException {
		if (view.getRegistry().getPart(part.path) == null) {
			// TODO complete this
		}
	}
		
	void exec(String sql, Object... params) throws MapFileException {
		view.mfc.exec(sql, params);
	}

	int execUpdate(String sql, Object... params) throws MapFileException {
		return view.mfc.execUpdate(sql, params);
	}
	
	PreparedStatement prepare(String sql, Object... params) throws MapFileException {
		return view.mfc.prepare(sql, params);
	}
	
	private int findPoolId() throws MapFileException {
		// TODO complete this
		return 0;
	}
		
	public NodeBuilder builder() {
		checkState(PoolState.BUILD);
		return new NodeBuilder(this);
	}
	
	/**
	 * Removes a node.
	 * 
	 * @param nodePath  Identifies the node to be removed.
	 * 
	 * @throws MapFileException  If node path cannot be marked for deletion. 
	 */
	public void deleteNode(String nodePath) throws MapFileException {
		checkState(PoolState.BUILD);
		checkPoolPath(nodePath);
		int nodeId = getViewNodeId(nodePath);
		if (nodeId != 0) {
			execUpdate("insert into PNODES(NODEID) values (?)", nodeId);
		}
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
	 * @throws MapFileException If map file cannot be accessed.
	 */
	int getViewNodeId(String nodePath) throws MapFileException {
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
			throw new MapFileException(e);
		}
	}

	/**
	 * Identifies a pool node from its path.
	 *  
	 * @param nodePath The view node path.
	 * 
	 * @throws MapFileException If map file cannot be accessed.
	 */
	int getPoolNodeId(String nodePath) throws MapFileException {
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
			throw new MapFileException(e);
		}
	}

	/**
	 * Merges all nodes and links in this pool to the target map part.
	 * 
	 * @throws MapFileException If map file cannot be accessed.
	 */
	public void merge() throws MapFileException {
		checkState(PoolState.BUILD);
		setState(PoolState.MERGE);
		view.mfc.file.viewLock.readLock().unlock();
		view.mfc.file.viewLock.writeLock().lock();
		mergeNodes();
		mergeJoins();
		compileOldLinks();
		compileNewLinks();
		compileDeadLinks();
		mergeUnresolvedLinks();
		mergeResolvedLinks();
		mergeTypes();
		registerPart();
		addToPoolsTable();
		view.mfc.file.viewLock.writeLock().unlock();
		view.mfc.file.viewLock.readLock().lock();
		setState(PoolState.COMPLETE);
	}
			
	void mergeNodes() throws MapFileException {
		// update existing nodes and insert new ones
		execUpdate("merge into VNODES V "
				+ "using PNODES P on V.NODEID = P.NODEID "
				// node to be deleted -> move to pool zero
				+ "when matched and P.DATA is null then "
				+ "update set POOLID = 0 "
				// case of an existing node to be updated
				+ "when matched and P.DATA is not null then "
				+ "update set V.DATA = P.DATA, V.POOLID = ? "
				// case of a new node
				+ "when not matched then insert "
				+ "values (P.NODEID, P.PATH, P.DATA, ?)", 
				poolId, poolId);
		// in CREATE_PART check nodes remaining in old pools
		if (mode==PoolMode.CREATE_PART) {
			// move to pool zero all nodes still in old pools
			for (int oldPoolId : listPartPools()) {
				execUpdate("update VNODES set POOLID = 0 "
						+ "where POOLID = ?", oldPoolId);
			}
			// add them to PNODES to ease further operations
			execUpdate("insert into PNODES(NODEID) "
					+ "direct select NODEID from VNODES "
					+ "where POOLID = 0");
		}
	}
	
	void removeOldJoins() throws MapFileException {
		execUpdate("merge into VJOINS V "
				+ "using PNODES P on V.NODEID = P.NODEID ");
	}

	void mergeJoins() throws MapFileException {
		// remove existing view joins for all nodes in this pool
		execUpdate("merge into VJOINS J "
				+ "using PNODES N on J.TNODEID = N.NODEID "
				+ "when matched then delete");
		// add new joins as defined in this pool
		execUpdate("insert into VJOINS(JPATH, TNODEID) "
				+ "direct select JPATH, TNODEID from PJOINS");
	}
	
	void compileOldLinks() throws MapFileException {
		// compile all view links from nodes in this pool
		execUpdate("insert into POLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select P.NODEID, LINKID, TNODEID "
				+ "from PNODES P join VRLINKS using (NODEID) "
				+ "where LINKID > 0");			
	}

	void compileNewLinks() throws MapFileException {
		// compile straight pool links (PSLINKS) to new view links (PNEWLS table)
		execUpdate("insert into PNLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select L.NODEID, LINKID, T.NODEID "
				+ "from PSLINKS L join VNODES T on TPATH = T.PATH");
		// compile join pool links (PJLINKS) to new view links (PNEWLS table)
		execUpdate("insert into PNLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select NODEID, LINKID, TNODEID "
				+ "from PJLINKS join VJOINS T on TPATH = JPATH");
	}
	
	void mergeUnresolvedLinks() throws MapFileException {
		// remove old unresolved links from nodes in this pool
		execUpdate("merge into VULINKS L "
				+ "using PNODES P on L.NODEID = P.NODEID "
				+ "when matched then delete");
		// TODO rewrite unresolved real paths to map part (CREATE_PART)
		// TODO try to resolve unresolved links, feed into PNLINKS
		// TODO collect new unresolved links from nodes in this pool
		
	}
	
	void compileDeadLinks() {
		// TODO compile links to nodes marked for deletion (pool-zero nodes)
	}

	void mergeResolvedLinks() throws MapFileException {
		// remove old links
		execUpdate("merge into VRLINKS V using POLINKS P "
				+ "on V.NODEID = P.NODEID and V.LINKID = P.LINKID and V.TNODEID = P.TNODEID "
				+ "when matched then delete");
		// remove corresponding reverse links
		execUpdate("merge into VRLINKS V using POLINKS P "
				+ "on V.NODEID = P.TNODEID and V.LINKID = -P.LINKID and V.TNODEID = P.NODEID "
				+ "when matched then delete");
		// add new links
		execUpdate("insert into VRLINKS (NODEID, LINKID, TNODEID) "
				+ "direct select NODEID, LINKID, TNODEID from PNLINKS");
		// add corresponding reverse links (to nodes in this pool)
		execUpdate("insert into VRLINKS (NODEID, LINKID, TNODEID) "
				+ "direct select TNODEID, -LINKID, NODEID from PNLINKS");
		// TODO do what with dead links?
	}
		
	void mergeTypes() throws MapFileException {
		view.getRegistry().newWriter(view.mfc).setSchema(schema);
	}

	public PoolMode getMode() {
		return mode;
	}

	@Override
	public void close() throws Exception {
		view.mfc.file.poolLock.unlock();
	}

	public PoolState getState() {
		return state;
	}
	
	void checkState(PoolState... states) {
		if (!Arrays.asList(states).contains(this.state)) {
			throw new IllegalStateException();
		}
	}

	private void setState(PoolState state) throws MapFileException {
		this.state = state;
		writePoolState();
	}
	
	int newLinkId() {
		// TODO complete this
		return 0;
	}

	String newFolderAlias() {
		// TODO complete this
		return null;
	}

	void addNodeLink(int nodeId, LinkField linkField, String toNodePath) throws MapFileException 
	{
		if (nodeId == 0 || linkField == null || toNodePath == null) {
			throw new IllegalArgumentException();
		}
		execUpdate("insert into PNLINKS(NODEID, LINKID, TPATH) values (?, ?, ?)",
				linkField.linkId, aliasNodePath(toNodePath));
	}

	void addJoinLink(int nodeId, LinkField linkField, String toJoinPath) throws MapFileException 
	{
		if (nodeId == 0 || linkField == null || toJoinPath == null) {
			throw new IllegalArgumentException();
		}
		execUpdate("insert into PJLINKS(NODEID, LINKID, TPATH) values (?, ?, ?)",
				linkField.linkId, aliasJoinPath(toJoinPath));
	}
	
	/**
	 * Adds a node link.
	 * 
	 * @param nodePath    The source node path (must refer to a node in this pool).
	 * @param linkName    The link field name.
	 * @param toNodePath  The target node path (should refer to any view or pool node).
	 * 
	 * @throws IllegalArgumentException If source node path or link name cannot be resolved.
	 * @throws MapFileException If map file cannot be accessed.
	 */
	public void addNodeLink(String nodePath, String linkFieldName, String toNodePath) throws MapFileException {
		// TODO review and complete this to call addNodeLink(int, LinkField, String)
	}

	String aliasNodePath(String path) {
		// TODO complete this
		return null;
	}
	
	String aliasJoinPath(String path) {
		// TODO complete this
		return null;
	}
	
	public boolean isPermanent() {
		return permanent;
	}

	public void setPermanent(boolean permanent) {
		// TODO complete this
		this.permanent = permanent;
	}
	
	private void writePoolState() throws MapFileException {
		view.getRegistry().newWriter(view.mfc).put(POOL_STATE_KEY, state.name());
	}
	
	static PoolState readPoolState(RegistryReader reader) throws MapFileException {
		String state = reader.get(POOL_STATE_KEY);
		return state != null? PoolState.valueOf(state) : null;
	}
	
	static void cleanPoolTables(MapFileConnection mfc) throws MapFileException {
		// TODO complete this
	}

	public MapSchema getSchema() {
		return schema;
	}

}
