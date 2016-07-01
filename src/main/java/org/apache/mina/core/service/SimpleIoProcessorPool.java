package org.apache.mina.core.service;

import java.lang.reflect.Constructor;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoProcessor} pool that distributes {@link IoSession}s into one or more
 * {@link IoProcessor}s. Most current transport implementations use this pool internally
 * to perform better in a multi-core environment, and therefore, you won't need to 
 * use this pool directly unless you are running multiple {@link IoService}s in the
 * same JVM.
 * <p>
 * If you are running multiple {@link IoService}s, you could want to share the pool
 * among all services.  To do so, you can create a new {@link SimpleIoProcessorPool}
 * instance by yourself and provide the pool as a constructor parameter when you
 * create the services.
 * <p>
 * This pool uses Java reflection API to create multiple {@link IoProcessor} instances.
 * It tries to instantiate the processor in the following order:
 * <ol>
 * <li>A public constructor with one {@link ExecutorService} parameter.</li>
 * <li>A public constructor with one {@link Executor} parameter.</li>
 * <li>A public default constructor</li>
 * </ol>
 * The following is an example for the NIO socket transport:
 * <pre><code>
 * // Create a shared pool.
 * SimpleIoProcessorPool&lt;NioSession&gt; pool = 
 *         new SimpleIoProcessorPool&lt;NioSession&gt;(NioProcessor.class, 16);
 * 
 * // Create two services that share the same pool.
 * SocketAcceptor acceptor = new NioSocketAcceptor(pool);
 * SocketConnector connector = new NioSocketConnector(pool);
 * 
 * ...
 * 
 * // Release related resources.
 * connector.dispose();
 * acceptor.dispose();
 * pool.dispose();
 * </code></pre>
 * 
 * 1. IoProcessor池：分配IoSession到1个或多个IoProcessor中处理。
 * 
 * 2. 在内部使用这个池在多核环境中可以有更好的性能，然而不要直接使用这个类，除非在单个JVM中有多个IoService。
 * 在单个JVM中有多个IoService，应该在所有IoService间共享这个IoProcessor池：创建这个类的实例，
 * 然后再创建IoService时把这个实例作为构造方法的参数。
 * 
 * 3. 这个类使用Java的反射API来创建多个IoProcessor的实例，初始化IoProcessor的顺序：
 * 	a. 使用ExecutorService做参数的构造方法。
 * 	b. 使用Executor做参数的构造方法。
 * 	c. 使用无参构造方法。
 * 	
 * 
 * @param <S> the type of the {@link IoSession} to be managed by the specified
 *            {@link IoProcessor}.
 * 
 * @date	2016年6月22日 上午11:32:26
 */
public class SimpleIoProcessorPool<S extends AbstractIoSession> implements IoProcessor<S> {

	/** A logger for this class */
    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleIoProcessorPool.class);
    
    /** The default pool size, when no size is provided. 默认的池大小：可用处理器数+1 */
    private static final int DEFAULT_SIZE = Runtime.getRuntime().availableProcessors() + 1;
    
    /** A key used to store the processor pool in the session's Attributes */
    private static final AttributeKey PROCESSOR = new AttributeKey(SimpleIoProcessorPool.class, "processor");
    
    /** The pool table. 池中的IoProcessor实例数组 */
    private final IoProcessor<S>[] pool;
    
    /** The contained which is passed to the IoProcessor when they are created */
    private final Executor executor;
    
    /** A flag set to true if we had to create an executor. 是否已创建executor的标识 */
    private final boolean createdExecutor;
    
    /** A lock to protect the disposal against concurrent calls. 控制dispose()方法被调用的锁 */
    private final Object disposalLock = new Object();
    
    /** A flg set to true if the IoProcessor in the pool are being disposed.  */
    private volatile boolean disposing;

    /** A flag set to true if all the IoProcessor contained in the pool have been disposed */
    private volatile boolean disposed;
    
    /**
     * Creates a new instance of SimpleIoProcessorPool with a default
     * size of NbCPUs +1.
     * 
     * 构造方法：默认的池大小
     *
     * @param processorType The type of IoProcessor to use
     */
    public SimpleIoProcessorPool(Class<? extends IoProcessor<S>> processorType) {
        this(processorType, null, DEFAULT_SIZE, null);
    }

    /**
     * Creates a new instance of SimpleIoProcessorPool with a defined
     * number of IoProcessors in the pool
     * 
     * 构造方法：指定的池大小
     *
     * @param processorType The type of IoProcessor to use
     * @param size The number of IoProcessor in the pool
     */
    public SimpleIoProcessorPool(Class<? extends IoProcessor<S>> processorType, int size) {
        this(processorType, null, size, null);
    }

    /**
     * Creates a new instance of SimpleIoProcessorPool with a defined
     * number of IoProcessors in the pool
     * 
     * 构造方法：指定的池大小和选择器提供者
     *
     * @param processorType The type of IoProcessor to use
     * @param size The number of IoProcessor in the pool
     * @param selectorProvider The SelectorProvider to use
     */
    public SimpleIoProcessorPool(Class<? extends IoProcessor<S>> processorType, int size, SelectorProvider selectorProvider) {
        this(processorType, null, size, selectorProvider);
    }

    /**
     * Creates a new instance of SimpleIoProcessorPool with an executor
     * 
     * 构造方法：默认的池大小和Executor
     *
     * @param processorType The type of IoProcessor to use
     * @param executor The {@link Executor}
     */
    public SimpleIoProcessorPool(Class<? extends IoProcessor<S>> processorType, Executor executor) {
        this(processorType, executor, DEFAULT_SIZE, null);
    }
    
    /**
     * Creates a new instance of SimpleIoProcessorPool with an executor
     * 
     * 构造方法：指定的池大小、选择器提供者、Executor
     *
     * @param processorType The type of IoProcessor to use
     * @param executor The {@link Executor}
     * @param size The number of IoProcessor in the pool
     * @param selectorProvider The SelectorProvider to used
     */
    @SuppressWarnings("unchecked")
    public SimpleIoProcessorPool(Class<? extends IoProcessor<S>> processorType, Executor executor, int size, 
            SelectorProvider selectorProvider) {
    	// 1. 参数验证
    	if (processorType == null) {
    		throw new IllegalArgumentException("processorType");
		}
    	if (size <= 0) {
            throw new IllegalArgumentException("size: " + size + " (expected: positive integer)");
        }
    	
    	// Create the executor if none is provided
    	// 2. 如果没有指定executor，创建executor
    	createdExecutor = (executor == null);
    	if (createdExecutor) {
			this.executor = Executors.newCachedThreadPool();
			// Set a default reject handler
			((ThreadPoolExecutor) this.executor).setRejectedExecutionHandler(
					new ThreadPoolExecutor.CallerRunsPolicy());
		} else {
			this.executor = executor;
		}
    	
    	// 3. 初始化pool
    	pool = new IoProcessor[size];
    	
    	boolean success = false;
    	Constructor<? extends IoProcessor<S>> processorConstructor = null;
    	boolean usesExecutorArg = true;
    	
    	try {
    		// We create at least one processor
    		// 4. 至少先创建1个processor
    		try {
    			try {
    				processorConstructor = processorType.getConstructor(ExecutorService.class);
    				pool[0] = processorConstructor.newInstance(this.executor);
    			} catch (NoSuchMethodException e1) {
    				// To the next step...
    				try {
    					if (selectorProvider == null) {
    						processorConstructor = processorType.getConstructor(Executor.class);
    						pool[0] = processorConstructor.newInstance(this.executor);
						} else {
							processorConstructor = processorType.getConstructor(Executor.class, SelectorProvider.class);
							pool[0] = processorConstructor.newInstance(this.executor, selectorProvider);
						}
    				} catch (NoSuchMethodException e2) {
    					// To the next step...
    					try {
    						processorConstructor = processorType.getConstructor();
    						usesExecutorArg = false;
    						pool[0] = processorConstructor.newInstance();
    					} catch (NoSuchMethodException e3) {
                            // To the next step...
                        }
    				}
    			}
    		} catch (RuntimeException re) {
    			LOGGER.error("Cannot create an IoProcessor :{}", re.getMessage());
                throw re;
            } catch (Exception e) {
                String msg = "Failed to create a new instance of " + processorType.getName() + ":" + e.getMessage();
                LOGGER.error(msg, e);
                throw new RuntimeIoException(msg, e);
            }
    		
    		if (processorConstructor == null) {
    			// Raise an exception if no proper constructor is found.
    			String msg = String.valueOf(processorType) + " must have a public constructor with one "
                        + ExecutorService.class.getSimpleName() + " parameter, a public constructor with one "
                        + Executor.class.getSimpleName() + " parameter or a public default constructor.";
                LOGGER.error(msg);
                throw new IllegalArgumentException(msg);
			}
    		
    		// Constructor found now use it for all subsequent instantiations
    		// 5. 使用构造方法创建剩下的processor
            for (int i = 1; i < pool.length; i++) {
            	try {
					if (usesExecutorArg) {
						if (selectorProvider == null) {
							pool[i] = processorConstructor.newInstance(this.executor);
						} else {
							pool[i] = processorConstructor.newInstance(this.executor, selectorProvider);
						}
					} else {
						pool[i] = processorConstructor.newInstance();
					}
				} catch (Exception e) {
					// Won't happen because it has been done previously
				}
            }
            success = true;
    	} finally {
    		if (!success) {
    			dispose();
    		}
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    public final void add(S session) {
    	getProcessor(session).add(session);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void flush(S session) {
        getProcessor(session).flush(session);
    }

    /**
     * {@inheritDoc}
     */
    public final void write(S session, WriteRequest writeRequest) {
        getProcessor(session).write(session, writeRequest);
    }

    /**
     * {@inheritDoc}
     */
    public final void remove(S session) {
        getProcessor(session).remove(session);
    }

    /**
     * {@inheritDoc}
     */
    public final void updateTrafficControl(S session) {
        getProcessor(session).updateTrafficControl(session);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisposing() {
        return disposing;
    }

    /**
     * {@inheritDoc}
     */
    public final void dispose() {
    	if (disposed) {
			return;
		}
    	synchronized (disposalLock) {
			if (!disposing) {
				disposing = true;
				for (IoProcessor<S> processor : pool) {
					if (processor == null) {
						// Special case if the pool has not been initialized properly
                        continue;
					}
					if (processor.isDisposing()) {
                        continue;
                    }
					try {
						processor.dispose();
					} catch (Exception e) {
                        LOGGER.warn("Failed to dispose the {} IoProcessor.", processor.getClass().getSimpleName(), e);
                    }
				}
				if (createdExecutor) {
					((ExecutorService) executor).shutdown();
				}
			}
			Arrays.fill(pool, null);
			disposed = true;
		}
    }
    
    /**
     * Find the processor associated to a session. If it hasen't be stored into
     * the session's attributes, pick a new processor and stores it.
     * 
     * 找到处理指定session的processor实例。
     * 如果指定session没有processor，就挑选一个并与指定session关联。
     */
    private IoProcessor<S> getProcessor(S session) {
    	// 1. 从session中获取处理它的processor
    	IoProcessor<S> processor = (IoProcessor<S>) session.getAttribute(PROCESSOR);
    	if (processor == null) {
			if (disposed || disposing) {
				throw new IllegalStateException("A disposed processor cannot be accessed.");
			}
			// 2. 根据session的ID对pool池大小取余，挑选处理这个session的processor。
			processor = pool[Math.abs((int) session.getId()) % pool.length];
			if (processor == null) {
				throw new IllegalStateException("A disposed processor cannot be accessed.");
			}
			// 3. 将挑选出来的processor设置到session的属性中(即标记为使用这个processor处理这个session)。
			session.setAttributeIfAbsent(PROCESSOR, processor);
		}
    	return processor;
    }
}
