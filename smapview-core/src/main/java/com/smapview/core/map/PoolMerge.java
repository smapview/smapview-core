package com.smapview.core.map;

import java.util.Date;

class PoolMerge {

	final NodePool pool;
	
	public PoolMerge(NodePool pool) {
		this.pool = pool;
	}

	void execute() throws MapDatabaseException {
		pool.stats.mergeStart = new Date();
		pool.stats.write(pool.view.dbc);
		mergeNodes();
		mergeJoins();
		compileOldLinks();
		compileNewLinks();
		compileDeadLinks();
		mergeUnresolvedLinks();
		mergeResolvedLinks();
		mergeSchema();
		pool.stats.mergeEnd = new Date();
		pool.stats.write(pool.view.dbc);
	}
	
	void mergeNodes() throws MapDatabaseException {
		// update existing nodes and insert new ones
		pool.execUpdate("merge into VNODES V "
				+ "using PNODES P on V.NODEID = P.NODEID "
				// node to be deleted -> move to pool zero
				+ "when matched and P.VALUES is null then "
				+ "delete "
				// case of an existing node to be updated
				+ "when matched and P.VALUES is not null then "
				+ "update set V.VALUES = P.VALUES "
				// case of a new node
				+ "when not matched then "
				+ "insert values (P.NODEID, P.PATH, P.VALUES)");
	}
	
	void removeOldJoins() throws MapDatabaseException {
		pool.execUpdate("merge into VJOINS V "
				+ "using PNODES P on V.NODEID = P.NODEID ");
	}

	void mergeJoins() throws MapDatabaseException {
		// remove existing view joins for all nodes in this pool
		pool.execUpdate("merge into VJOINS J "
				+ "using PNODES N on J.TNODEID = N.NODEID "
				+ "when matched then delete");
		// add new joins as defined in this pool
		pool.execUpdate("insert into VJOINS(JPATH, TNODEID) "
				+ "direct select JPATH, TNODEID from PJOINS");
	}
	
	void compileOldLinks() throws MapDatabaseException {
		// compile all view links from nodes in this pool
		pool.execUpdate("insert into POLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select P.NODEID, LINKID, TNODEID "
				+ "from PNODES P join VRLINKS using (NODEID) "
				+ "where LINKID > 0");			
	}

	void compileNewLinks() throws MapDatabaseException {
		// compile straight pool links (PSLINKS) to new view links (PNEWLS table)
		pool.execUpdate("insert into PNLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select L.NODEID, LINKID, T.NODEID "
				+ "from PSLINKS L join VNODES T on TPATH = T.PATH");
		// compile join pool links (PJLINKS) to new view links (PNEWLS table)
		pool.execUpdate("insert into PNLINKS(NODEID, LINKID, TNODEID) "
				+ "direct select NODEID, LINKID, TNODEID "
				+ "from PJLINKS join VJOINS T on TPATH = JPATH");
	}
	
	void mergeUnresolvedLinks() throws MapDatabaseException {
		// remove old unresolved links from nodes in this pool
		pool.execUpdate("merge into VULINKS L "
				+ "using PNODES P on L.NODEID = P.NODEID "
				+ "when matched then delete");
		// TODO rewrite unresolved real paths to map part (CREATE_PART)
		// TODO try to resolve unresolved links, feed into PNLINKS
		// TODO collect new unresolved links from nodes in this pool
		
	}
	
	void compileDeadLinks() {
		// TODO compile links to nodes marked for deletion (pool-zero nodes)
	}

	void mergeResolvedLinks() throws MapDatabaseException {
		// remove old links
		pool.execUpdate("merge into VRLINKS V using POLINKS P "
				+ "on V.NODEID = P.NODEID and V.LINKID = P.LINKID and V.TNODEID = P.TNODEID "
				+ "when matched then delete");
		// remove corresponding reverse links
		pool.execUpdate("merge into VRLINKS V using POLINKS P "
				+ "on V.NODEID = P.TNODEID and V.LINKID = -P.LINKID and V.TNODEID = P.NODEID "
				+ "when matched then delete");
		// add new links
		pool.execUpdate("insert into VRLINKS (NODEID, LINKID, TNODEID) "
				+ "direct select NODEID, LINKID, TNODEID from PNLINKS");
		// add corresponding reverse links (to nodes in this pool)
		pool.execUpdate("insert into VRLINKS (NODEID, LINKID, TNODEID) "
				+ "direct select TNODEID, -LINKID, NODEID from PNLINKS");
		// TODO do what with dead links?
	}
		
	void mergeSchema() throws MapDatabaseException {
		pool.view.getRegistry().newWriter(pool.view.dbc).setSchema(pool.schema);
	}
	
}
