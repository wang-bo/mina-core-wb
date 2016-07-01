package org.apache.mina.core.service;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.IoUtil;
import org.apache.mina.core.filterchain.DefaultIoFilterChain;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.filterchain.IoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.session.IoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSessionInitializationException;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.util.ExceptionMonitor;
import org.apache.mina.util.NamePreservingRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link IoService}s.
 * 
 * An instance of IoService contains an Executor which will handle the incoming
 * events.
 * 
 * IoService的基础实现。
 * 一个IoService的实例会包含一个Executor来处理请求事件。
 * 
 * @date	2016年6月14日 下午3:47:24	completed
 */
public abstract class AbstractIoService implements IoService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIoService.class);

	/**
     * The unique number identifying the Service. It's incremented
     * for each new IoService created.
     * 
     * service实例的id，当新的service创建时会自增。
     */
	private static final AtomicInteger id = new AtomicInteger();
	
	/**
     * The thread name built from the IoService inherited
     * instance class name and the IoService Id
     * 
     * 通过IoService实例的类名和IoService的id生成的线程名称。
     */
	private final String threadName;
	
	/**
     * The associated executor, responsible for handling execution of I/O events.
     * 
     * 负责处理I/O事件的线程执行器。
     */
	private final Executor executor;
	
	/**
     * A flag used to indicate that the local executor has been created
     * inside this instance, and not passed by a caller.
     * 
     * If the executor is locally created, then it will be an instance
     * of the ThreadPoolExecutor class.
     * 
     * 一个用来表明当前service里的executor是否初始化的标识，不会传递给调用者。
     * 如果executor被创建，会是一个ThreadPoolExecutor类型的实例。
     */
	private final boolean createdExecutor;
	
	/**
     * The IoHandler in charge of managing all the I/O Events. It is
     * 
     * 负责处理所有I/O事件的IoHandler。
     */
	private IoHandler handler;
	
	/**
     * The default {@link IoSessionConfig} which will be used to configure new sessions.
     * 
     * 当前service上创建session时的默认session config。
     */
	protected final IoSessionConfig sessionConfig;
	
	/**
	 * 当前service的事件监听器
	 */
	private final IoServiceListener serviceActivationListener = new IoServiceListener() {
		
		public void serviceActivated(IoService service) throws Exception {
			// Update lastIoTime 更新最后一次发生I/O操作的时间
			IoServiceStatistics statistics = service.getStatistics();
			statistics.setLastReadTime(service.getActivationTime());
			statistics.setLastWriteTime(service.getActivationTime());
			statistics.setLastThroughputCalculationTime(service.getActivationTime());
		}
		
		public void serviceDeactivated(IoService service) throws Exception {
            // Empty handler
        }

        public void serviceIdle(IoService service, IdleStatus idleStatus) throws Exception {
            // Empty handler
        }

        public void sessionCreated(IoSession session) throws Exception {
            // Empty handler
        }

        public void sessionClosed(IoSession session) throws Exception {
            // Empty handler
        }

        public void sessionDestroyed(IoSession session) throws Exception {
            // Empty handler
        }
	};
	
	/**
     * Current filter chain builder.
     * 
     * 当前过滤器链建造器
     */
	private IoFilterChainBuilder filterChainBuilder = new DefaultIoFilterChainBuilder();
	
	/**
	 * 新建session时提供数据结构的工厂
	 */
	private IoSessionDataStructureFactory sessionDataStructureFactory = new DefaultIoSessionDataStructureFactory();
	
	/**
     * Maintains the {@link IoServiceListener}s of this service.
     * 
     * 维持当前service上的IoServiceListener的帮助类
     */
	private final IoServiceListenerSupport listeners;
	
	/**
     * A lock object which must be acquired when related resources are
     * destroyed.
     * 
     * 清理用的锁，要清理service关联的资源时必须先获得这个锁。
     */
	protected final Object disposalLock = new Object();
	
	/**
	 * 标记当前service是否处于disposing状态
	 */
	private volatile boolean disposing;
	
	/**
	 * 标记当前service是否处于disposed状态
	 */
	private volatile boolean disposed;
	
	/**
	 * 统计IoService数据的对象
	 */
	private IoServiceStatistics statistics = new IoServiceStatistics(this);
	
	/**
     * Constructor for {@link AbstractIoService}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * a null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个处理I/O事件的Executor实例。
     * 如果提供的Executor实例为null，默认使用Executors.newCachedThreadPool()方法来创建一个。
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling execution of I/O
     *            events. Can be <code>null</code>.
     */
	protected AbstractIoService(IoSessionConfig sessionConfig, Executor executor) {
		// 1. 数据验证
		if (sessionConfig == null) {
            throw new IllegalArgumentException("sessionConfig");
        }
        if (getTransportMetadata() == null) {
            throw new IllegalArgumentException("TransportMetadata");
        }
        // 判断参数sessionConfig的类型是否是TransportMetadata里规定的SessionConfig的类型或子类
        if (!getTransportMetadata().getSessionConfigType().isAssignableFrom(sessionConfig.getClass())) {
            throw new IllegalArgumentException("sessionConfig type: " + sessionConfig.getClass() + " (expected: "
                    + getTransportMetadata().getSessionConfigType() + ")");
        }
        
        // 2. Create the listeners, and add a first listener : a activation listener
        // for this service, which will give information on the service state.
        listeners = new IoServiceListenerSupport(this);
        listeners.add(serviceActivationListener);
        
        // 3. Stores the given session configuration
        this.sessionConfig = sessionConfig;
        
        // 4. Make JVM load the exception monitor before some transports
        // change the thread context class loader.
        ExceptionMonitor.getInstance();
        
        // 5. 设置负责处理I/O事件的Executor
        if (executor == null) {
			this.executor = Executors.newCachedThreadPool();
			createdExecutor = true;
		} else {
			this.executor = executor;
			createdExecutor = true;
		}
        
        // 6. 设置threadName
        threadName = getClass().getSimpleName() + "-" + id.incrementAndGet();
	}
	
	/**
     * {@inheritDoc}
     */
	public final IoFilterChainBuilder getFilterChainBuilder() {
		return filterChainBuilder;
	}
	
	/**
     * {@inheritDoc}
     */
    public final void setFilterChainBuilder(IoFilterChainBuilder builder) {
        if (builder == null) {
            builder = new DefaultIoFilterChainBuilder();
        }
        filterChainBuilder = builder;
    }
	
    /**
     * {@inheritDoc}
     */
    public final DefaultIoFilterChainBuilder getFilterChain() {
    	if (filterChainBuilder instanceof DefaultIoFilterChainBuilder) {
    		return (DefaultIoFilterChainBuilder) filterChainBuilder;
        }
        throw new IllegalStateException("Current filter chain builder is not a DefaultIoFilterChainBuilder.");
    }
	
    /**
     * {@inheritDoc}
     */
    public final void addListener(IoServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final void removeListener(IoServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isActive() {
        return listeners.isActive();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDisposed() {
        return disposed;
    }
	
    /**
     * {@inheritDoc}
     */
    public final void dispose() {
        dispose(false);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void dispose(boolean awaitTermination) {
    	// 1. 已dispose过，直接返回
    	if (disposed) {
			return;
		}
    	
    	// 2. 做dispose操作
    	synchronized (disposalLock) {
			if (!disposing) {
				disposing = true;
				try {
					dispose0();
				} catch (Exception e) {
					ExceptionMonitor.getInstance().exceptionCaught(e);
				}
			}
		}
    	
    	// 3. 结束executor，如果参数awaitTermination为true，则该方法阻塞到executor终止为止。
    	if (createdExecutor) {
			ExecutorService executorService = (ExecutorService) executor;
			executorService.shutdown();
			if (awaitTermination) {
				try {
					LOGGER.debug("awaitTermination on {} called by thread=[{}]", this, Thread.currentThread().getName());
					executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
					LOGGER.debug("awaitTermination on {} finished", this);
				} catch (InterruptedException e) {
					LOGGER.warn("awaitTermination on [{}] was interrupted", this);
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
				}
			}
		}
    	
    	// 4. 设置disposed状态
    	disposed = true;
    }
    
    /**
     * Implement this method to release any acquired resources.  This method
     * is invoked only once by {@link #dispose()}.
     * 
     * 释放已获得的资源的方法，由具体的实现类实现，这个方法只会被dispose()方法调用1次。
     * 
     * @throws Exception If the dispose failed
     */
    protected abstract void dispose0() throws Exception;
	
    /**
     * {@inheritDoc}
     */
    public final Map<Long, IoSession> getManagedSessions() {
        return listeners.getManagedSessions();
    }

    /**
     * {@inheritDoc}
     */
    public final int getManagedSessionCount() {
        return listeners.getManagedSessionCount();
    }

    /**
     * {@inheritDoc}
     */
    public final IoHandler getHandler() {
        return handler;
    }

    /**
     * {@inheritDoc}
     */
    public final void setHandler(IoHandler handler) {
    	if (handler == null) {
			throw new IllegalArgumentException("handler cannot be null");
		}
    	if (isActive()) {
            throw new IllegalStateException("handler cannot be set while the service is active.");
        }
    	this.handler = handler;
    }
	
    /**
     * {@inheritDoc}
     */
    public final IoSessionDataStructureFactory getSessionDataStructureFactory() {
        return sessionDataStructureFactory;
    }

    /**
     * {@inheritDoc}
     */
    public final void setSessionDataStructureFactory(IoSessionDataStructureFactory sessionDataStructureFactory) {
        if (sessionDataStructureFactory == null) {
            throw new IllegalArgumentException("sessionDataStructureFactory");
        }
        if (isActive()) {
            throw new IllegalStateException("sessionDataStructureFactory cannot be set while the service is active.");
        }
        this.sessionDataStructureFactory = sessionDataStructureFactory;
    }
    
    /**
     * {@inheritDoc}
     */
    public IoServiceStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * {@inheritDoc}
     */
    public final long getActivationTime() {
        return listeners.getActivationTime();
    }
    
    /**
     * {@inheritDoc}
     */
    public final Set<WriteFuture> broadcast(Object message) {
    	// Convert to Set.  We do not return a List here because only the
        // direct caller of MessageBroadcaster knows the order of write
        // operations.
    	// 返回值转换成Set，因为：只有直接调用的地方才知道调用的顺序
    	final List<WriteFuture> futures = IoUtil.broadcast(message, getManagedSessions().values());
    	return new AbstractSet<WriteFuture>() {
    		@Override
            public Iterator<WriteFuture> iterator() {
                return futures.iterator();
            }
            @Override
            public int size() {
                return futures.size();
            }
		};
    }
    
    /**
     * 返回IoServiceListenerSupport实例
     */
    public final IoServiceListenerSupport getListeners() {
        return listeners;
    }
    
    /**
     * 使用executor来执行worker所代表的任务
     * @param worker
     */
    protected final void executeWorker(Runnable worker) {
		executeWorker(worker, null);
	}
    
    /**
     * 使用executor来执行worker所代表的任务
     * @param worker
     * @param suffix
     */
    protected final void executeWorker(Runnable worker, String suffix) {
    	String actualThreadName = threadName;
    	if (suffix != null) {
    		actualThreadName = actualThreadName + '-' + suffix;
		}
    	executor.execute(new NamePreservingRunnable(worker, actualThreadName));
	}
    
    /**
     * 初始化IoSession
     * @param session
     * @param future
     * @param sessionInitializer
     */
    protected final void initSession(IoSession session, IoFuture future, IoSessionInitializer<IoFuture> sessionInitializer) {
    	// 1. Update lastIoTime if needed. 修改最近一次I/O的时间
    	if (statistics.getLastReadTime() == 0) {
			statistics.setLastReadTime(getActivationTime());
		}
    	if (statistics.getLastWriteTime() == 0) {
    		statistics.setLastWriteTime(getActivationTime());
        }
    	
    	// 2. Every property but attributeMap should be set now.
        // Now initialize the attributeMap.  The reason why we initialize
        // the attributeMap at last is to make sure all session properties
        // such as remoteAddress are provided to IoSessionDataStructureFactory.
    	try {
            ((AbstractIoSession) session).setAttributeMap(session.getService().getSessionDataStructureFactory()
                    .getAttributeMap(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException("Failed to initialize an attributeMap.", e);
        }
    	
    	// 3. 设置RequestQueue
    	try {
            ((AbstractIoSession) session).setWriteRequestQueue(session.getService().getSessionDataStructureFactory()
                    .getWriteRequestQueue(session));
        } catch (IoSessionInitializationException e) {
            throw e;
        } catch (Exception e) {
            throw new IoSessionInitializationException("Failed to initialize a writeRequestQueue.", e);
        }
    	
    	// 4. 处理ConnectFuture
    	if ((future != null) && (future instanceof ConnectFuture)) {
            // DefaultIoFilterChain will notify the future. (We support ConnectFuture only for now).
            session.setAttribute(DefaultIoFilterChain.SESSION_CREATED_FUTURE, future);
        }
    	
    	// 5. 使用IoSessionInitializer初始化IoSession
    	if (sessionInitializer != null) {
    		sessionInitializer.initializeSession(session, future);
		}
    	
    	// 6. session初始化时需要执行的其它任务。
    	finishSessionInitialization0(session, future);
    }
    
    /**
     * Implement this method to perform additional tasks required for session
     * initialization. Do not call this method directly;
     * {@link #initSession(IoSession, IoFuture, IoSessionInitializer)} will call
     * this method instead.
     * 
     * 子类覆盖这个方法来执行session初始化时需要的其它任务。
     * 别直接调用这个方法，因为执行initSession(IoSession, IoFuture, IoSessionInitializer)方法时会调用此方法。
     * 
     * @param session The session to initialize
     * @param future The Future to use
     * 
     */
    protected void finishSessionInitialization0(IoSession session, IoFuture future) {
        // Do nothing. Extended class might add some specific code
    }
    
    /**
     * {@inheritDoc}
     */
    public int getScheduledWriteBytes() {
        return statistics.getScheduledWriteBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getScheduledWriteMessages() {
        return statistics.getScheduledWriteMessages();
    }

    /**
     * A specific class used to 
     * 
     * 一个代表Service操作结果的类
     * 
     * @date	2016年6月15日 下午3:15:34
     */
    protected static class ServiceOperationFuture extends DefaultIoFuture {
        public ServiceOperationFuture() {
            super(null);
        }

        public final boolean isDone() {
            return getValue() == Boolean.TRUE;
        }

        public final void setDone() {
            setValue(Boolean.TRUE);
        }

        public final Exception getException() {
            if (getValue() instanceof Exception) {
                return (Exception) getValue();
            }
            return null;
        }

        public final void setException(Exception exception) {
            if (exception == null) {
                throw new IllegalArgumentException("exception");
            }
            setValue(exception);
        }
    }
}
