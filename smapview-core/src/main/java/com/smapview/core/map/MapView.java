package com.smapview.core.map;

import java.util.List;

import com.smapview.core.map.NodePool.PoolMode;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;

public class MapView implements AutoCloseable {

	final MapFileConnection mfc;
	
	public MapView(MapFile file) throws MapFileException {
		file.viewLock.readLock().lock();
		this.mfc = new MapFileConnection(file);
	}

	Node getNode(String path) {
		return null;
	}
	
	List<Node> getLinkNodes(Node node, String linkName) {
		return null;
	}
	
	List<Node> searchText(String text) {
		return null;
	}
	
	MapGraph getGraph() {
		// TODO complete this
		return null;
	}
	
	ExecutionResult execQuery(String graphQL) {
		// TODO add the appropriate things to this wiring
		// TODO consider using NodeQuery
		RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring().build();
		GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(getGraph().types, wiring);
		return GraphQL.newGraphQL(schema).build().execute(graphQL);
	}
	
	List<Node> queryNodes(String graphQL) {
		// TODO implement this method
		return null;
	}

	@Override
	public void close() throws Exception {
		mfc.file.viewLock.readLock().unlock();
		mfc.close();
	}
	
	/**
	 * Opens a new pool to create or update a map part.
	 * 
	 * @param partPath  Map part unique path. 
	 * @param mode      Tells if pool is used to update or create/replace map part.
	 * @return  The pool opened for map part creation or update.
	 * @throws MapFileException If map file cannot be accessed.
	 */
	public NodePool openPool(String partPath, PoolMode mode) throws MapFileException {
		mfc.file.poolLock.lock();
		return new NodePool(this, partPath, mode);
	}
	
	MapRegistry getRegistry() {
		return mfc.file.registry;
	}
	
	public MapSchema getSchema() {
		return getRegistry().getSchema();
	}

}
