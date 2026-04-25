package com.smapview.core.map;

import com.smapview.core.map.NodePool.PoolState;

class DatabaseLoader extends DatabaseConnection {
	
	DatabaseLoader(MapDatabase file) throws MapDatabaseException {
		super(file);
	}
	
	void init() throws MapDatabaseException {
		initSchema();
    	db.registry.load(this);
    	checkPoolState();
	}
	
	void initSchema() throws MapDatabaseException {
   		exec("create schema if not exists SMAP");
		exec("set schema SMAP");
		exec("create alias FT_INIT for \"org.h2.fulltext.FullText.init\"");
		exec("call FT_INIT()");
		// entry-point table with information such as map name, node types, etc.
    	exec("create table if not exists REGISTRY(EKEY varchar primary key, EVAL varchar)");
    	// more specific tables for map view, build pool and query
    	initViewTables();
    	initPoolTables();
    	initQueryTables();
	}
	
	void initViewTables() throws MapDatabaseException {
    	// table used to store source for every root node
    	exec("create table if not exists VSOURCE(PATH varchar, SOURCE varchar)");
    	// view table used to store nodes with their path and data
    	exec("create table if not exists VNODES(NODEID int primary key, PATH varchar, VALUES varchar)");
    	exec("create unique index if not exists VNODES_IDX1 on VNODES(PATH)");
    	exec("call FT_CREATE_INDEX('SMAP', 'VNODES', 'VALUES')");
    	// table with owned/joined node key values used to resolve join links
    	// ROLE gives the node role: O for key owner, or J for key joiner 
    	exec("create table if not exists VJOINS(NODEID int, ROLE char(1), KEYID smallint, VALUE varchar)");
    	exec("create index if not exists VJOINS_IDX1 on VJOINS(NODEID)");
    	exec("create index if not exists VJOINS_IDX2 on VJOINS(KEYID, ROLE)");
    	// view table used to link nodes together (resolved straight links)
    	// NODEID refers to source node, TNODEID to target node
    	exec("create table if not exists VRLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create unique index if not exists VRLINKS_IDX1 on VRLINKS(NODEID, LINKID, TNODEID)");
    	// table for unresolved straight links (pointing to node paths that cannot be resolved) 
    	exec("create table if not exists VULINKS(NODEID int, LINKID smallint, TPATH varchar)");
    	exec("create index if not exists VULINKS_IDX1 on VULINKS(NODEID)");
    	exec("create index if not exists VULINKS_IDX2 on VULINKS(TPATH)");
	}

	void initPoolTables() throws MapDatabaseException {
    	// table that lists pools, with source, merge time and statistics for each pool
    	exec("create table if not exists PSTATS(POOLID int primary key, SOURCE varchar, "
    			+ "CTIME time with time zone, STATS varchar)");
    	exec("create index if not exists PSTATS_IDX1 on PSTATS(MTIME)");
    	// table used to store pool nodes with their path and data
    	// FLAG can be I (insert node), U (update node), K (keep node) or D (delete node)
    	exec("create table if not exists PNODES(NODEID int, PATH varchar, DATA varchar, FLAG char(1))");
    	exec("create unique index if not exists PNODES_IDX1 on PNODES(PATH)");
    	// table with owned/joined pool node key values used to resolve join links
    	exec("create table if not exists PJOINS(NODEID int, ROLE char(1), KEYID smallint, VALUE varchar)");
    	// table with all links from pool nodes, to node paths (straight links)
    	exec("create table if not exists PSLINKS(NODEID int, LINKID smallint, TPATH varchar)");
    	exec("create index if not exists PSLINKS_IDX1 on PSLINKS(TPATH)");
    	// tables with compiled links (old links, new links and dead links)
    	exec("create table if not exists POLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create table if not exists PNLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create table if not exists PDLINKS(NODEID int, LINKID smallint, TNODEID int)");
	}

	void initQueryTables() throws MapDatabaseException {
    	exec("create local temporary table if not exists QSCOPE(SFIELD varchar, "
    			+ "LINKID smallint, TFIELD varchar) not persistent");
    	exec("create index if not exists QSCOPE_IDX1 on QSCOPE(SFIELD, LINKID)");		
    	exec("create local temporary table if not exists QLINKS(LFIELD varchar, "
    			+ "NODEID int, TNODEID int) not persistent");
    	exec("create index if not exists QLINKS_IDX1 on QLINKS(LFIELD)");		
	}
	
	void checkPoolState() throws MapDatabaseException {
		PoolState state = NodePool.readPoolState(db.registry.newReader(this));
		if (state == PoolState.BUILD) {
			NodePool.cleanPoolTables(this);
		}
		else if (state == PoolState.MERGE) {
			throw new MapDatabaseException("Cannot recover from incomplete pool merge");
		}
	}

}
