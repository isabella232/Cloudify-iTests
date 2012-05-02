package framework.utils;

import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;

import framework.utils.LogUtils;

public class SGConsoleHandler extends ConsoleHandler {

	@Override
	public void publish(LogRecord record) {
		String msg = getFormatter().format(record);
		LogUtils.log(msg);
	}
	
	

}
