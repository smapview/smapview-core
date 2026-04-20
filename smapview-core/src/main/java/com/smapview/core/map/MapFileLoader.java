package com.smapview.core.map;

import com.smapview.core.map.NodePool.PoolState;

class MapFileLoader extends MapFileConnection {
	
	MapFileLoader(MapFile file) throws MapFileException {
		super(file);
	}
	
	void init() throws MapFileException {
		initSchema();
    	file.registry.load(this);
    	checkPoolState();
	}
	
	void initSchema() throws MapFileException {
   		exec("create schema if not exists SMAP");
		exec("set schema SMAP");
		exec("create alias FT_INIT for \"org.h2.fulltext.FullText.init\"");
		exec("call FT_INIT()");
		// entry-point table with information such as map name, node types, etc.
    	exec("create table if not exists REGISTRY(EKEY varchar primary key, EVAL varchar)");
    	// table that lists pools
    	exec("create table if not exists POOLS(POOLID smallint primary key, PATH varchar)");
    	// more specific tables for map view, build pool and query
    	initViewTables();
    	initPoolTables();
    	initQueryTables();
	}

	void initViewTables() throws MapFileException {
    	// view table used to store nodes with their path and data
    	exec("create table if not exists VNODES(NODEID int primary key, PATH varchar, "
    			+ "DATA varchar, POOLID smallint)");
    	exec("create unique index if not exists VNODES_IDX1 on VNODES(PATH)");
    	exec("create index if not exists VNODES_IDX2 on VNODES(POOLID)");
    	exec("call FT_CREATE_INDEX('SMAP', 'VNODES', 'DATA')");
    	exec("create sequence NODEID_SEQ");
    	// table with join paths to nodes, for link purpose
    	// please note that node paths and join paths are totally separated path spaces
    	exec("create table if not exists VJOINS(JPATH varchar, TNODEID int)");
    	exec("create index if not exists VJOINS_IDX1 on VJOINS(JPATH)");
    	exec("create index if not exists VJOINS_IDX2 on VJOINS(TNODEID)");
    	// view table used to link nodes together (resolved links)
    	// NODEID refers to source node, TNODEID to target node
    	exec("create table if not exists VRLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create unique index if not exists VRLINKS_IDX1 on VRLINKS(NODEID, LINKID, TNODEID)");
    	// table for unresolved links, pointing to node paths that cannot be resolved 
    	exec("create table if not exists VULINKS(NODEID int, LINKID smallint, TPATH varchar)");
    	exec("create index if not exists VULINKS_IDX1 on VULINKS(NODEID)");
    	exec("create index if not exists VULINKS_IDX2 on VULINKS(TPATH)");
	}

	void initPoolTables() throws MapFileException {
    	// table used to store nodes with their path and data
    	exec("create table if not exists PNODES(NODEID int, PATH varchar, DATA varchar)");
    	exec("create unique index if not exists PNODES_IDX1 on PNODES(PATH)");
    	// table with all join paths to new or updated nodes
    	exec("create table if not exists PJOINS(JPATH varchar, TNODEID int)");
    	// table with all links from new or updated nodes, to node paths (straight links)
    	exec("create table if not exists PSLINKS(NODEID int, LINKID smallint, TPATH varchar)");
    	exec("create index if not exists PSLINKS_IDX1 on PSLINKS(TPATH)");
    	// table with all links from new or updated nodes, to join paths (join links)
    	exec("create table if not exists PJLINKS(NODEID int, LINKID smallint, TPATH varchar)");
    	exec("create index if not exists PJLINKS_IDX1 on PJLINKS(TPATH)");
    	// tables with compiled links (old links, new links and dead links)
    	exec("create table if not exists POLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create table if not exists PNLINKS(NODEID int, LINKID smallint, TNODEID int)");
    	exec("create table if not exists PDLINKS(NODEID int, LINKID smallint, TNODEID int)");
	}

	void initQueryTables() throws MapFileException {
    	exec("create local temporary table if not exists QSCOPE(SFIELD varchar, "
    			+ "LINKID smallint, TFIELD varchar) not persistent");
    	exec("create index if not exists QSCOPE_IDX1 on QSCOPE(SFIELD, LINKID)");		
    	exec("create local temporary table if not exists QLINKS(LFIELD varchar, "
    			+ "NODEID int, TNODEID int) not persistent");
    	exec("create index if not exists QLINKS_IDX1 on QLINKS(LFIELD)");		
	}
	
	void checkPoolState() throws MapFileException {
		PoolState state = NodePool.readPoolState(file.registry.newReader(this));
		if (state == PoolState.BUILD) {
			NodePool.cleanPoolTables(this);
		}
		else if (state == PoolState.MERGE) {
			throw new MapFileException("Cannot recover from incomplete pool merge");
		}
	}

}
