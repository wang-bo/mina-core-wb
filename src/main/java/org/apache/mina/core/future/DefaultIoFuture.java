package org.apache.mina.core.future;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.ExceptionMonitor;

/**
 * A default implementation of {@link IoFuture} associated with
 * an {@link IoSession}.
 * 
 * IoFuture的默认实现
 * 
 * @date	2016年6月15日 下午5:38:17	completed
 */
public class DefaultIoFuture implements IoFuture {
	
	/** A number of milliseconds to wait between two deadlock controls ( 5 seconds ) */
    private static final long DEAD_LOCK_CHECK_INTERVAL = 5000L;

    /** The associated session */
    private final IoSession session;

    /** A lock used by the wait() method */
    private final Object lock;
    
    /** 
     * The first listener. This is easier to have this variable
     * when we most of the time have one single listener 
     * 
     * 第一个IoFutureListener，因为通常只有一个listener，所以定义这个变量可以方便使用 
     */
    private IoFutureListener<?> firstListener;
    
    /** 
     * All the other listeners, in case we have more than one 
     * 
     * 当有多个监听器时，这里保存其它的监听器(除了firstListener)
     */
    private List<IoFutureListener<?>> otherListeners;
    
    /** future的结果 */
    private Object result;

    /** The flag used to determinate if the Future is completed or not. 确认future是否完成的标识*/
    private boolean ready;

    /** A counter for the number of threads waiting on this future. 等待这个future完成的线程数 */
    private int waiters;
    
    /**
     * Creates a new instance associated with an {@link IoSession}.
     * 
     * 构造方法
     *
     * @param session an {@link IoSession} which is associated with this future
     */
    public DefaultIoFuture(IoSession session) {
        this.session = session;
        this.lock = this;
    }

    /**
     * {@inheritDoc}
     */
    public IoSession getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public void join() {
        awaitUninterruptibly();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public boolean join(long timeoutMillis) {
        return awaitUninterruptibly(timeoutMillis);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toMillis(timeout), true);
    }

    /**
     * {@inheritDoc}
     */
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(timeoutMillis, true);
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFuture awaitUninterruptibly() {
        try {
            await0(Long.MAX_VALUE, false);
        } catch (InterruptedException ie) {
            // Do nothing : this catch is just mandatory by contract
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toMillis(timeout), false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFuture await() throws InterruptedException {
    	synchronized (lock) {
			while (!ready) {
				waiters++;
				try {
					// Wait for a notify, or if no notify is called,
                    // assume that we have a deadlock and exit the
                    // loop to check for a potential deadlock.
					// 等待notify，如果没有notify()方法调用，假设死锁了，检测这个可能的死锁
					lock.wait(DEAD_LOCK_CHECK_INTERVAL);
				} finally {
					waiters--;
					if (!ready) {
						checkDeadLock();
					}
				}
			}
		}
    	return this;
    }
    
    /**
     * Wait for the Future to be ready. If the requested delay is 0 or
     * negative, this method immediately returns the value of the
     * 'ready' flag.
     * Every 5 second, the wait will be suspended to be able to check if
     * there is a deadlock or not.
     * 
     * 等待future完成，如果timeout传了非正数，则该方法直接返回ready标识的值。
     * 每5秒钟，wait()操作会被阻塞并检测是否有死锁。
     * 
     * @param timeoutMillis The delay we will wait for the Future to be ready
     * @param interruptable Tells if the wait can be interrupted or not
     * @return <tt>true</tt> if the Future is ready
     * @throws InterruptedException If the thread has been interrupted
     * when it's not allowed.
     */
    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
    	long endTime = System.currentTimeMillis() + timeoutMillis;
    	if (endTime < 0) {
			endTime = Long.MAX_VALUE;
		}
    	
    	synchronized (lock) {
    		// We can quit if the ready flag is set to true, or if
            // the timeout is set to 0 or below : we don't wait in this case.
    		if (ready || timeoutMillis <= 0) {
				return ready;
			}
    		
    		// The operation is not completed : we have to wait
            waiters++;
            
            try {
            	while (true) {
            		try {
						long timeOut = Math.min(timeoutMillis, DEAD_LOCK_CHECK_INTERVAL);
						// Wait for the requested period of time,
                        // but every DEAD_LOCK_CHECK_INTERVAL seconds, we will
                        // check that we aren't blocked.
						lock.wait(timeOut);
					} catch (InterruptedException e) {
						if (interruptable) {
                            throw e;
                        }
					}
                	if (ready || endTime < System.currentTimeMillis()) {
    					return ready;
    				} else {
    					// Take a chance, detect a potential deadlock
                        checkDeadLock();
    				}
            	}
            } finally {
            	// We get here for 3 possible reasons :
                // 1) We have been notified (the operation has completed a way or another)
                // 2) We have reached the timeout
                // 3) The thread has been interrupted
                // In any case, we decrement the number of waiters, and we get out.
            	// 代码执行到这里有3种情况：1-被notify()方法唤醒了, 2-超时了, 3-被中断了
            	waiters--;
                if (!ready) {
                    checkDeadLock();
                }
            }
		}
    }
    
    /**
     * Check for a deadlock, ie look into the stack trace that we don't have already an 
     * instance of the caller.
     * 
     * 检测死锁：通过观察当前线程的stack trace。
     */
    private void checkDeadLock() {
    	// Only read / write / connect / write future can cause dead lock.
        if (!(this instanceof CloseFuture || this instanceof WriteFuture || this instanceof ReadFuture || this instanceof ConnectFuture)) {
            return;
        }
        
        // Get the current thread stackTrace.
        // Using Thread.currentThread().getStackTrace() is the best solution,
        // even if slightly less efficient than doing a new Exception().getStackTrace(),
        // as internally, it does exactly the same thing. The advantage of using
        // this solution is that we may benefit some improvement with some
        // future versions of Java.
        // 获得当前线程的stackTrace，使用Thread.currentThread().getStackTrace()是最好的方法。
        // 虽然比new Exception().getStackTrace()性能稍差，但是我们可以对一些版本的Java future做改进。
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // Simple and quick check.
        for (StackTraceElement stackElement : stackTrace) {
        	if (AbstractPollingIoProcessor.class.getName().equals(stackElement.getClassName())) {
				IllegalStateException e = new IllegalStateException("t");
				e.getStackTrace();
				throw new IllegalStateException("DEAD LOCK: " + IoFuture.class.getSimpleName()
                        + ".await() was invoked from an I/O processor thread.  " + "Please use "
                        + IoFutureListener.class.getSimpleName() + " or configure a proper thread model alternatively.");
			}
        }
        
        // And then more precisely.
        for (StackTraceElement stackElement : stackTrace) {
            try {
                Class<?> clazz = DefaultIoFuture.class.getClassLoader().loadClass(stackElement.getClassName());
                if (IoProcessor.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("DEAD LOCK: " + IoFuture.class.getSimpleName()
                            + ".await() was invoked from an I/O processor thread.  " + "Please use "
                            + IoFutureListener.class.getSimpleName()
                            + " or configure a proper thread model alternatively.");
                }
            } catch (ClassNotFoundException cnfe) {
                // Ignore
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isDone() {
        synchronized (lock) {
            return ready;
        }
    }
    
    /**
     * Sets the result of the asynchronous operation, and mark it as finished.
     * 
     * 设置异步操作的结果，并标记future已完成。
     * 需要notify在wait的线程，并通知listener。
     * 
     * @param newValue The result to store into the Future
     * @return {@code true} if the value has been set, {@code false} if
     * the future already has a value (thus is in ready state)
     */
    public boolean setValue(Object newValue) {
    	synchronized (lock) {
			// Allowed only once
    		if (ready) {
				return false;
			}
    		
    		result = newValue;
    		ready = true;
    		
    		// Now, if we have waiters, notofy them that the operation has completed
    		if (waiters > 0) {
				lock.notifyAll();
			}
		}
    	
    	// Last, not least, inform the listeners
    	notifyListeners();
    	
    	return true;
    }
    
    /**
     * @return the result of the asynchronous operation.
     * 
     * 返回异步操作的结果。
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFuture addListener(IoFutureListener<?> listener) {
    	if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
    	synchronized (lock) {
			if (ready) {
				// Shortcut : if the operation has completed, no need to 
                // add a new listener, we just have to notify it. The existing
                // listeners have already been notified anyway, when the 
                // 'ready' flag has been set.
				// 如果future操作已完成，再添加listener，则直接通知listener。
                notifyListener(listener);
			} else {
				if (firstListener == null) {
					firstListener = listener;
				} else {
					if (otherListeners == null) {
						otherListeners = new ArrayList<IoFutureListener<?>>(1);
					}
					otherListeners.add(listener);
				}
			}
		}
    	return this;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoFuture removeListener(IoFutureListener<?> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener");
        }
        synchronized (lock) {
            if (!ready) {
                if (listener == firstListener) {
                    if ((otherListeners != null) && !otherListeners.isEmpty()) {
                        firstListener = otherListeners.remove(0);
                    } else {
                        firstListener = null;
                    }
                } else if (otherListeners != null) {
                    otherListeners.remove(listener);
                }
            }
        }
        return this;
    }
    
    /**
     * Notify the listeners, if we have some.
     * 
     * 如果有监听器，通知监听器future操作已完成。
     */
    private void notifyListeners() {
        // There won't be any visibility problem or concurrent modification
        // because 'ready' flag will be checked against both addListener and
        // removeListener calls.
    	// 不会有线程安全问题，因为在addListener()和removeListener()方法都检查了future操作是否已完成标识。
        if (firstListener != null) {
            notifyListener(firstListener);
            firstListener = null;
            if (otherListeners != null) {
                for (IoFutureListener<?> listener : otherListeners) {
                    notifyListener(listener);
                }
                otherListeners = null;
            }
        }
    }
	
    private void notifyListener(IoFutureListener listener) {
    	try {
    		listener.operationComplete(this);
    	} catch (Exception e) {
    		ExceptionMonitor.getInstance().exceptionCaught(e);
    	}
    }

}
