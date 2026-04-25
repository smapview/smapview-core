package com.smapview.core.map;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

public class PoolStats {

	static final DateFormat TIME_FORMAT = new SimpleDateFormat();
	
	final int poolId;
	
	final String source;
	
	Date createTime;
	
	Date mergeStart;

	Date mergeEnd;

	PoolStats(int poolId, String source) {
		this.poolId = poolId;
		this.source = source;
	}
	
	void write(DatabaseConnection dbc) throws MapDatabaseException {
		JsonObjectBuilder stats = Json.createObjectBuilder();
		if (mergeStart != null) stats.add("mergeStart", TIME_FORMAT.format(mergeStart));
		if (mergeEnd != null) stats.add("mergeEnd", TIME_FORMAT.format(mergeEnd));
		dbc.execUpdate("insert into PSTATS(POOLID, SOURCE, MTIME, STATS) "
				+ "values (?, ?, ?, ?)", poolId, source, createTime, 
				stats.build().toString());
	}

}
