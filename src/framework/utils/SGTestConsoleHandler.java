package framework.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

public class SGTestConsoleHandler extends ConsoleHandler {

	@Override
	public void publish(LogRecord record) {
		String loggerName = record.getLoggerName();
		String logLevel = record.getLevel().toString();
		String message = record.getMessage();
		String log = logLevel + " [" + loggerName + "] " + " - " + message;
		LogUtils.log(log);
	}
}
