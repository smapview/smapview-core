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
	
	private static final String LOGGER_NAME = "com.smapview.agent.view";
	
	private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);
	
	private static final LogHandler logHandler = new LogHandler();
	
	static {
		logHandler.setLevel(Level.FINEST);
		LOGGER.addHandler(logHandler);
		LOGGER.setUseParentHandlers(false);
		LOGGER.setLevel(Level.OFF);
	}
	
	public static void setLoggingLevel(Level level) {
		LOGGER.setLevel(level);
	}
		
	static void trace(String msg, Object... args) {
		LOGGER.log(Level.FINE, msg, args);
	}

	static void info(String msg, Object... args) {
		LOGGER.log(Level.INFO, msg, args);
	}

	static long parseTime(String time) {
		// TODO implement this
		return 0;
	}
		
}
