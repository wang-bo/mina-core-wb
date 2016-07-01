package org.apache.mina.util;

/**
 * Monitors uncaught exceptions.  {@link #exceptionCaught(Throwable)} is
 * invoked when there are any uncaught exceptions.
 * <p>
 * You can monitor any uncaught exceptions by setting {@link ExceptionMonitor}
 * by calling {@link #setInstance(ExceptionMonitor)}.  The default
 * monitor logs all caught exceptions in <tt>WARN</tt> level using
 * SLF4J.
 * 
 * 监控未捕获的异常。当有未捕获的异常时，会调用exceptionCaught(Throwable)方法。
 * 可以通过setInstance(ExceptionMonitor)方法来设置ExceptionMonitor实例。
 * 默认的监控日志使用slf4j的WARN级别记录。
 * 
 * @date	2016年6月15日 上午10:45:55	completed
 */
public abstract class ExceptionMonitor {

	/** 当前使用的监控异常的实例 */
	private static ExceptionMonitor instance = new DefaultExceptionMonitor();
	
	/**
     * @return the current exception monitor.
     * 
     * 返回当前使用的监控异常的实例
     */
    public static ExceptionMonitor getInstance() {
        return instance;
    }
    
    /**
     * Sets the uncaught exception monitor.  If <code>null</code> is specified,
     * the default monitor will be set.
     * 
     * 设置监控异常的实例，如果参数为null，则会使用DefaultExceptionMonitor实例。
     *
     * @param monitor A new instance of {@link DefaultExceptionMonitor} is set
     *                if <tt>null</tt> is specified.
     */
    public static void setInstance(ExceptionMonitor monitor) {
    	if (monitor == null) {
			monitor = new DefaultExceptionMonitor();
		}
    	instance = monitor;
    }
    
    /**
     * Invoked when there are any uncaught exceptions.
     * 
     * 当有未捕获的异常时调用。
     * 
     * @param cause The caught exception
     */
    public abstract void exceptionCaught(Throwable cause);
}
