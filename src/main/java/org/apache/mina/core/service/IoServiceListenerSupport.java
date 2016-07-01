package org.apache.mina.core.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.util.ExceptionMonitor;

/**
 * A helper class which provides addition and removal of {@link IoServiceListener}s and firing
 * events.
 * 
 * 一个帮助类，用来增加或移除IoServiceListener并触发事件。
 * 
 * 给IoFilter和IoServiceListener触发4种事件：
 * 	a. serviceActivated
 * 	b. serviceDeactivated
 * 	c. sessionCreated
 * 	d. sessionDestroyed
 * @date	2016年6月14日 下午5:17:14	completed
 */
public class IoServiceListenerSupport {

	/** The {@link IoService} that this instance manages. */
	private final IoService service;
	
	/** A list of {@link IoServiceListener}s. 监听器列表变化不会很频繁，适合用CopyOnWriteArrayList */
	private final List<IoServiceListener> listeners = new CopyOnWriteArrayList<IoServiceListener>();
	
	/** Tracks managed sessions. 当前service上管理的所有session */
	private final ConcurrentMap<Long, IoSession> managedSessions = new ConcurrentHashMap<Long, IoSession>();
	
	/**  Read only version of {@link #managedSessions}. 当前service上管理的所有session的只读Map */
	private final Map<Long, IoSession> readOnlyManagedSessions = Collections.unmodifiableMap(managedSessions);
	
	/** 标记当前service是否活跃 */
	private final AtomicBoolean activated = new AtomicBoolean();
	
	/** Time this listenerSupport has been activated 当前IoServiceListenerSupport活跃的时间 */
    private volatile long activationTime;

    /** A counter used to store the maximum sessions we managed since the listenerSupport has been activated. 同一时间管理过的最大session数。 */
    private volatile int largestManagedSessionCount = 0;

    /** A global counter to count the number of sessions managed since the start. 管理过的所有session数(当前管理的session数和已关闭的session数之和)。*/
    private AtomicLong cumulativeManagedSessionCount = new AtomicLong(0);

    /**
     * Creates a new instance of the listenerSupport.
     * 
     * 构造方法
     * 
     * @param service The associated IoService
     */
	public IoServiceListenerSupport(IoService service) {
		if (service == null) {
            throw new IllegalArgumentException("service");
        }
		this.service = service;
	}
    
	/**
     * Adds a new listener.
     * 
     * 添加一个listener。
     * 
     * @param listener The added listener
     */
    public void add(IoServiceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an existing listener.
     * 
     * 移除一个已存在的listener。
     * 
     * @param listener The listener to remove
     */
    public void remove(IoServiceListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * @return The time (in ms) this instance has been activated
     * 
     * 返回当前IoServiceListenerSupport活跃的时间。
     */
    public long getActivationTime() {
        return activationTime;
    }
    
    /**
     * 返回管理的所有session，返回的Map是只读的。
     */
    public Map<Long, IoSession> getManagedSessions() {
        return readOnlyManagedSessions;
    }

    /**
     * 返回管理的session数。
     */
    public int getManagedSessionCount() {
        return managedSessions.size();
    }

    /**
     * @return The largest number of managed session since the creation of this
     * listenerSupport
     * 
     * 返回开启到现在同一时间管理过的最大session数。
     */
    public int getLargestManagedSessionCount() {
        return largestManagedSessionCount;
    }

    /**
     * @return The total number of sessions managed since the initilization of this
     * ListenerSupport
     * 
     * 返回开启到现在管理过的所有session数(当前管理的session数和已关闭的session数之和)。
     */
    public long getCumulativeManagedSessionCount() {
        return cumulativeManagedSessionCount.get();
    }
    
    /**
     * @return true if the instance is active
     * 
     * 返回当前IoServiceListenerSupport是否活跃。
     */
    public boolean isActive() {
        return activated.get();
    }
    
    /**
     * Calls {@link IoServiceListener#serviceActivated(IoService)}
     * for all registered listeners.
     * 
     * 触发service活跃事件，调用所有listener的serviceActivated()方法。
     */
    public void fireServiceActivated() {
    	// 1. 比较activated状态是否是false，并把状态设置为true。
        if (!activated.compareAndSet(false, true)) {
            // The instance is already active
            return;
        }
        // 2. 修改活跃时间
        activationTime = System.currentTimeMillis();
        
        // 3. Activate all the listeners now
        for (IoServiceListener listener : listeners) {
            try {
                listener.serviceActivated(service);
            } catch (Exception e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }
    
    /**
     * Calls {@link IoServiceListener#serviceDeactivated(IoService)}
     * for all registered listeners.
     * 
     * 触发service不活跃事件，调用所有listener的serviceDeactivated()方法。
     */
    public void fireServiceDeactivated() {
    	// 1. 比较activated状态是否是true，并把状态设置为false。
        if (!activated.compareAndSet(true, false)) {
            // The instance is already desactivated
            return;
        }

        // 2. Desactivate all the listeners
        try {
            for (IoServiceListener listener : listeners) {
                try {
                    listener.serviceDeactivated(service);
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
        	// 3. 关闭所有session
            disconnectSessions();
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionCreated(IoSession)} for all registered listeners.
     * 
     * 触发session创建事件，调用所有listener的sessionCreated()方法。
     * 
     * @param session The session which has been created
     */
    public void fireSessionCreated(IoSession session) {
        // 1. If already registered, ignore.
        if (managedSessions.putIfAbsent(session.getId(), session) != null) {
            return;
        }

        // 2. If the first connector session, fire a virtual service activation event.
        if (session.getService() instanceof IoConnector) {
            boolean firstSession = false;
            synchronized (managedSessions) {
                firstSession = managedSessions.isEmpty();
            }
            if (firstSession) {
                fireServiceActivated();
            }
        }

        // 3. Fire session events.
        IoFilterChain filterChain = session.getFilterChain();
        filterChain.fireSessionCreated();
        filterChain.fireSessionOpened();

        // 4. 管理过的最大session数
        int managedSessionCount = managedSessions.size();
        if (managedSessionCount > largestManagedSessionCount) {
            largestManagedSessionCount = managedSessionCount;
        }

        // 5. 累计管理的session数
        cumulativeManagedSessionCount.incrementAndGet();

        // 6. Fire listener events.
        for (IoServiceListener listener : listeners) {
            try {
            	listener.sessionCreated(session);
            } catch (Exception e) {
                ExceptionMonitor.getInstance().exceptionCaught(e);
            }
        }
    }

    /**
     * Calls {@link IoServiceListener#sessionDestroyed(IoSession)} for all registered listeners.
     * 
     * 触发session销毁事件，调用所有listener的sessionDestroyed()方法。
     * 
     * @param session The session which has been destroyed
     */
    public void fireSessionDestroyed(IoSession session) {
        // 1. Try to remove the remaining empty session set after removal.
        if (managedSessions.remove(session.getId()) == null) {
            return;
        }

        // 2. Fire session events.
        session.getFilterChain().fireSessionClosed();

        // 3. Fire listener events.
        try {
            for (IoServiceListener listener : listeners) {
                try {
                	listener.sessionDestroyed(session);
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        } finally {
            // 4. Fire a virtual service deactivation event for the last session of the connector.
            if (session.getService() instanceof IoConnector) {
                boolean lastSession = false;
                synchronized (managedSessions) {
                    lastSession = managedSessions.isEmpty();
                }
                if (lastSession) {
                    fireServiceDeactivated();
                }
            }
        }
    }
    
    /**
     * Close all the sessions
     * 
     * 关闭所有session。
     * 
     * TODO disconnectSessions.
     */
    private void disconnectSessions() {
        // 1. We don't disconnect sessions for anything but an Acceptor
        if (!(service instanceof IoAcceptor)) {
            return;
        }

        // 2. 如果配置了service不活跃时不关闭已连接的session，直接返回
        if (!((IoAcceptor) service).isCloseOnDeactivation()) {
            return;
        }

        // 3. 关闭所有session
        Object lock = new Object();
        IoFutureListener<IoFuture> listener = new LockNotifyingListener(lock);
        for (IoSession session : managedSessions.values()) {
        	session.closeNow().addListener(listener);
        }
        try {
            synchronized (lock) {
                while (!managedSessions.isEmpty()) {
                    lock.wait(500);
                }
            }
        } catch (InterruptedException ie) {
            // Ignored
        }
    }

    /**
     * A listener in charge of releasing the lock when the close has been completed
     * 
     * 监听session.close()成功事件的监听器，事件发生时释放锁。
     */
    private static class LockNotifyingListener implements IoFutureListener<IoFuture> {
        private final Object lock;

        public LockNotifyingListener(Object lock) {
            this.lock = lock;
        }

        public void operationComplete(IoFuture future) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }
}
