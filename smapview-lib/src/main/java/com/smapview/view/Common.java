package com.smapview.view;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Common {

	static class LogHandler extends java.util.logging.Handler {
		
		@Override
		public void close() throws SecurityException {}
		
		@Override
		public void flush() {}
		
		@Override
		synchronized public void publish(LogRecord record) {
			System.out.format("[%s] ", record.getLevel().getName());
			System.out.format(record.getMessage(), record.getParameters());
			System.out.println();
			System.out.flush();
		}		
	}
	
	public static final String LOGGER_NAME = "com.smapview.agent.view";
	
	public static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);
	
	static final LogHandler logHandler = new LogHandler();
	
	static {
		logHandler.setLevel(Level.FINER);
		LOGGER.addHandler(logHandler);
	}
		
	static void trace(String msg, Object... args) {
		LOGGER.log(Level.FINE, msg, args);
	}
	
	static long parseTime(String time) {
		// TODO implement this
		return 0;
	}
		
}
