package org.apache.mina.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Runnable} wrapper that preserves the name of the thread after the runnable is
 * complete (for {@link Runnable}s that change the name of the Thread they use.)
 * 
 * 一个Runnable的包装类，保存了runnable和runnable的名称。当线程执行runnable时会修改线程名称，执行完毕后改回线程的原名称。
 * 
 * @date	2016年6月15日 下午2:36:45	completed
 */
public class NamePreservingRunnable implements Runnable {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(NamePreservingRunnable.class);
	
	/** The runnable name */
    private final String newName;

    /** The runnable task */
    private final Runnable runnable;
    
    /**
     * Creates a new instance of NamePreservingRunnable.
     * 
     * 构造方法
     *
     * @param runnable The underlying runnable
     * @param newName The runnable's name
     */
    public NamePreservingRunnable(Runnable runnable, String newName) {
        this.runnable = runnable;
        this.newName = newName;
    }

    /**
     * Run the runnable after having renamed the current thread's name 
     * to the new name. When the runnable has completed, set back the 
     * current thread name back to its origin. 
     * 
     * 修改当前线程的名称，并执行runnable，执行完成后改回线程的原名称。
     */
	public void run() {
		Thread currentThread = Thread.currentThread();
		String oldName = currentThread.getName();
		if (newName != null) {
			setName(currentThread, newName);
		}
		try {
			runnable.run();
		} finally {
			setName(currentThread, oldName);
		}
	}
	
	/**
     * Wraps {@link Thread#setName(String)} to catch a possible {@link Exception}s such as
     * {@link SecurityException} in sandbox environments, such as applets
     * 
     * 修改线程的名称：封装了Thread.setName(String)方法并捕获可能发生的SecurityException。
     * 当在沙盒(如Applet)中运行时可能抛出此异常。
     */
	private void setName(Thread thread, String name) {
		try {
			thread.setName(name);
		} catch (SecurityException e) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Failed to set the thread name.", e);
			}
		}
	}

}
