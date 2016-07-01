package org.apache.mina.util;

import org.apache.mina.core.service.IoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default {@link ExceptionMonitor} implementation that logs uncaught
 * exceptions using {@link Logger}.
 * <p>
 * All {@link IoService}s have this implementation as a default exception
 * monitor.
 * 
 * 默认的异常监控器：记录日志
 * 所有IoService有一个本类的实例，作为异常监控器。
 * 
 * @date	2016年6月15日 上午10:57:35	completed
 */
public class DefaultExceptionMonitor extends ExceptionMonitor {

	private final static Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionMonitor.class);

	/**
     * {@inheritDoc}
     */
    @Override
	public void exceptionCaught(Throwable cause) {
		if (cause instanceof Error) {
			throw (Error) cause;
		}
		LOGGER.warn("Unexpected exception.", cause);
	}
	
}
