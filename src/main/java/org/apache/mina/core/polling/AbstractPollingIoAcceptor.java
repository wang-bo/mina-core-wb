package org.apache.mina.core.polling;

import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.service.AbstractIoAcceptor.AcceptorOperationFuture;
import org.apache.mina.core.service.AbstractIoService.ServiceOperationFuture;
import org.apache.mina.core.session.AbstractIoSession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.ExceptionMonitor;

/**
 * A base class for implementing transport using a polling strategy. The
 * underlying sockets will be checked in an active loop and woke up when an
 * socket needed to be processed. This class handle the logic behind binding,
 * accepting and disposing the server sockets. An {@link Executor} will be used
 * for running client accepting and an {@link AbstractPollingIoProcessor} will
 * be used for processing client I/O operations like reading, writing and
 * closing.
 * 
 * All the low level methods for binding, accepting, closing need to be provided
 * by the subclassing implementation.
 * 
 * 一个实现基于轮询策略传输方式的基类。
 * 一个线程会检测底层的socket，当socket需要处理的时候唤醒。
 * 这个类处理绑定端口、接收请求、释放资源等操作后的逻辑。
 * 一个线程执行器Executor会用来执行接收客户端的请求。
 * 一个AbstractPollingIoProcessor实例会用来处理客户端的I/O操作，如：读、写、关闭。
 * 一切底层方法如绑定端口、接收请求、关闭等操作需要有子类来实现。
 * 
 * @see NioSocketAcceptor for a example of implementation
 * 
 * @date	2016年6月17日 下午3:51:35	completed
 */
public abstract class AbstractPollingIoAcceptor<S extends AbstractIoSession, H> extends AbstractIoAcceptor {

	/** A lock used to protect the selector to be waked up before it's created */
    private final Semaphore lock = new Semaphore(1);
    
    /** I/O处理器 */
    private final IoProcessor<S> processor;
    
    /** 是否已创建processor */
    private final boolean createdProcessor;
    
    /** 绑定操作的结果future队列 */
    private final Queue<AcceptorOperationFuture> registerQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();

    /** 解绑操作的结果future队列 */
    private final Queue<AcceptorOperationFuture> cancelQueue = new ConcurrentLinkedQueue<AcceptorOperationFuture>();
    
    /** 本地Socket地址对应的已绑定的服务器套接字通道(使用NIO时) */
    private final Map<SocketAddress, H> boundHandles = Collections.synchronizedMap(new HashMap<SocketAddress, H>());

    /** 表示清理结果的future */
    private final ServiceOperationFuture disposalFuture = new ServiceOperationFuture();
    
    /** A flag set when the acceptor has been created and initialized. 标记acceptor是否已可用 */
    private volatile boolean selectable;
    
    /** The thread responsible of accepting incoming requests. acceptor线程，负责接收客户端发送的连接请求。 */
    private AtomicReference<Acceptor> acceptorRef = new AtomicReference<Acceptor>();

    /** 地址是否可重用：解绑后是否可以立即绑定这个地址 */
    protected boolean reuseAddress = false;

    /**
     * Define the number of socket that can wait to be accepted. Default
     * to 50 (as in the SocketServer default).
     * 
     * 能排队等待请求被接受的socket数，默认50个。
     */
    protected int backlog = 50;
    
    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration, a class of {@link IoProcessor} which will be instantiated in a
     * {@link SimpleIoProcessorPool} for better scaling in multiprocessor systems. The default
     * pool size will be used.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个IoProcessor类型，这个IoProcessor会被使用SimpleIoProcessorPool实例化。
     * 使用SimpleIoProcessorPool是为了在多处理器的系统中更好地扩展，池的大小使用默认值。
     * 
     * @see SimpleIoProcessorPool
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processorClass a {@link Class} of {@link IoProcessor} for the associated {@link IoSession}
     *            type.
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Class<? extends IoProcessor<S>> processorClass) {
        this(sessionConfig, null, new SimpleIoProcessorPool<S>(processorClass), true, null);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration, a class of {@link IoProcessor} which will be instantiated in a
     * {@link SimpleIoProcessorPool} for using multiple thread for better scaling in multiprocessor
     * systems.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个IoProcessor类型，这个IoProcessor会被使用SimpleIoProcessorPool实例化。
     * 使用SimpleIoProcessorPool是为了在多处理器的系统中更好地扩展，池的大小由参数指定。
     * 
     * @see SimpleIoProcessorPool
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processorClass a {@link Class} of {@link IoProcessor} for the associated {@link IoSession}
     *            type.
     * @param processorCount the amount of processor to instantiate for the pool
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Class<? extends IoProcessor<S>> processorClass,
            int processorCount) {
        this(sessionConfig, null, new SimpleIoProcessorPool<S>(processorClass, processorCount), true, null);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration, a class of {@link IoProcessor} which will be instantiated in a
     * {@link SimpleIoProcessorPool} for using multiple thread for better scaling in multiprocessor
     * systems.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个IoProcessor类型，这个IoProcessor会被使用SimpleIoProcessorPool实例化。
     * 使用SimpleIoProcessorPool是为了在多处理器的系统中更好地扩展，池的大小由参数指定。
     *
     * @see SimpleIoProcessorPool
     *
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processorClass a {@link Class}�of {@link IoProcessor} for the associated {@link IoSession}
     *            type.
     * @param processorCount the amount of processor to instantiate for the pool
     * @param selectorProvider The SelectorProvider to use
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Class<? extends IoProcessor<S>> processorClass,
            int processorCount, SelectorProvider selectorProvider ) {
        this(sessionConfig, null, new SimpleIoProcessorPool<S>(processorClass, processorCount, selectorProvider), true, selectorProvider);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a default
     * session configuration, a default {@link Executor} will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个IoProcessor实例。
     * 
     * @see AbstractIoService
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param processor the {@link IoProcessor} for processing the {@link IoSession} of this transport, triggering
     *            events to the bound {@link IoHandler} and processing the chains of {@link IoFilter}
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, IoProcessor<S> processor) {
        this(sessionConfig, null, processor, false, null);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a
     * default session configuration and an {@link Executor} for handling I/O
     * events. If a null {@link Executor} is provided, a default one will be
     * created using {@link Executors#newCachedThreadPool()}.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个处理I/O事件的Executor实例，一个IoProcessor实例。
     * 如果提供的Executor实例为null，默认使用Executors.newCachedThreadPool()方法来创建一个。
     * 
     * @see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling asynchronous execution
     *            of I/O events. Can be <code>null</code>.
     * @param processor
     *            the {@link IoProcessor} for processing the {@link IoSession}
     *            of this transport, triggering events to the bound
     *            {@link IoHandler} and processing the chains of
     *            {@link IoFilter}
     */
    protected AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Executor executor, IoProcessor<S> processor) {
        this(sessionConfig, executor, processor, false, null);
    }

    /**
     * Constructor for {@link AbstractPollingIoAcceptor}. You need to provide a
     * default session configuration and an {@link Executor} for handling I/O
     * events. If a null {@link Executor} is provided, a default one will be
     * created using {@link Executors#newCachedThreadPool()}.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个处理I/O事件的Executor实例，一个IoProcessor实例。
     * 如果提供的Executor实例为null，默认使用Executors.newCachedThreadPool()方法来创建一个。
     * 
     * @see AbstractIoService(IoSessionConfig, Executor)
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling asynchronous execution
     *            of I/O events. Can be <code>null</code>.
     * @param processor
     *            the {@link IoProcessor} for processing the {@link IoSession}
     *            of this transport, triggering events to the bound
     *            {@link IoHandler} and processing the chains of
     *            {@link IoFilter}
     * @param createdProcessor
     *            tagging the processor as automatically created, so it will be
     *            automatically disposed
     */
    private AbstractPollingIoAcceptor(IoSessionConfig sessionConfig, Executor executor, IoProcessor<S> processor,
            boolean createdProcessor, SelectorProvider selectorProvider) {
        super(sessionConfig, executor);

        if (processor == null) {
            throw new IllegalArgumentException("processor");
        }

        this.processor = processor;
        this.createdProcessor = createdProcessor;

        try {
            // Initialize the selector
            init(selectorProvider);

            // The selector is now ready, we can switch the
            // flag to true so that incoming connection can be accepted
            selectable = true;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeIoException("Failed to initialize.", e);
        } finally {
            if (!selectable) {
                try {
                    destroy();
                } catch (Exception e) {
                    ExceptionMonitor.getInstance().exceptionCaught(e);
                }
            }
        }
    }
    
    /**
     * Initialize the polling system, will be called at construction time.
     * 
     * 初始化轮询系统，在构造方法中被调用。
     * 
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void init() throws Exception;

    /**
     * Initialize the polling system, will be called at construction time.
     * 
     * 初始化轮询系统，在构造方法中被调用。
     * 
     * @param selectorProvider The Selector Provider that will be used by this polling acceptor
     * @throws Exception any exception thrown by the underlying system calls
     */
    protected abstract void init(SelectorProvider selectorProvider) throws Exception;

    /**
     * Destroy the polling system, will be called when this {@link IoAcceptor}
     * implementation will be disposed.
     * 
     * 销毁轮询系统，当前acceptor实例dispose时被调用。
     * 
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void destroy() throws Exception;

    /**
     * Check for acceptable connections, interrupt when at least a server is ready for accepting.
     * All the ready server socket descriptors need to be returned by {@link #selectedHandles()}
     * 
     * 检测准备好能做接受操作的连接，当至少一个服务能做接受操作时中断，即阻塞到至少有一个key代表的通道做好I/O准备时返回。
     * 使用selectedHandles()方法来返回所有能做接受操作的服务端socket描述符。
     * 
     * @return The number of sockets having got incoming client. 返回有连接请求的socket数。
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract int select() throws Exception;

    /**
     * Interrupt the {@link #select()} method. Used when the poll set need to be modified.
     * 
     * 中断select()方法，当改变轮询的设置时调用。
     */
    protected abstract void wakeup();

    /**
     * {@link Iterator} for the set of server sockets found with acceptable incoming connections
     *  during the last {@link #select()} call.
     *  
     *  返回上一次select()调用时，所有准备好能做接受操作的服务端socket集合的迭代器。
     *  
     * @return the list of server handles ready
     */
    protected abstract Iterator<H> selectedHandles();

    /**
     * Open a server socket for a given local address.
     * 
     * 使用一个本地Socket地址，打开一个服务端socket。
     * 
     * @param localAddress the associated local address
     * @return the opened server socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract H open(SocketAddress localAddress) throws Exception;

    /**
     * Get the local address associated with a given server socket
     * 
     * 返回指定的服务端socket关联的本地Socket地址。
     * 
     * @param handle the server socket
     * @return the local {@link SocketAddress} associated with this handle
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract SocketAddress localAddress(H handle) throws Exception;

    /**
     * Accept a client connection for a server socket and return a new {@link IoSession}
     * associated with the given {@link IoProcessor}
     * 
     * 在指定服务端socket上接受一个客户端的连接请求，生成并返回一个新的IoSession，这个IoSession关联指定的IoProcessor。
     * 
     * @param processor the {@link IoProcessor} to associate with the {@link IoSession}
     * @param handle the server handle
     * @return the created {@link IoSession}
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract S accept(IoProcessor<S> processor, H handle) throws Exception;

    /**
     * Close a server socket.
     * 
     * 关闭一个服务端socket。
     * 
     * @param handle the server socket
     * @throws Exception any exception thrown by the underlying systems calls
     */
    protected abstract void close(H handle) throws Exception;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispose0() throws Exception {
        unbind();
        startupAcceptor();
        wakeup();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws Exception {
    	// Create a bind request as a Future operation. When the selector
        // have handled the registration, it will signal this future.
    	// 1. 创建一个绑定请求操作的future，当selector处理完成后，future会得到通知。
    	AcceptorOperationFuture request = new AcceptorOperationFuture(localAddresses);
    	
    	// adds the Registration request to the queue for the Workers
        // to handle
    	// 2. 将这个绑定请求的future放入注册队列中，让Workers来处理。
        registerQueue.add(request);
        
        // creates the Acceptor instance and has the local
        // executor kick it off.
        // 3. 开启Acceptor：创建Acceptor实例，并使用本地executor启动它。
        startupAcceptor();
        
        // start the acceptor if not already started
        // 4. 如果负责接收来自客户端的连接请求的Acceptor未启动，则会使用本地executor启动它。
        Acceptor acceptor = acceptorRef.get();
        if (acceptor == null) {
			lock.acquire();
			acceptor = new Acceptor();
			if (acceptorRef.compareAndSet(null, acceptor)) {
				executeWorker(acceptor);
			} else {
				lock.release();
			}
		}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected final void unbind0(List<? extends SocketAddress> localAddresses) throws Exception {
    	// 1. 创建一个绑定请求操作的future，当selector处理完成后，future会得到通知。
    	AcceptorOperationFuture future = new AcceptorOperationFuture(localAddresses);

    	// 2. 将这个绑定请求的future放入取消队列中，让Workers来处理。
        cancelQueue.add(future);
        
        // 3. 开启Acceptor：这里应该什么也不会发生。
        startupAcceptor();
        
        // 4. 中断select()方法。
        wakeup();

        // 5. 等待future完成。
        future.awaitUninterruptibly();
        
        // 6. 如果解绑操作发生异常，则抛出异常。
        if (future.getException() != null) {
            throw future.getException();
        }
    }
    
    /**
     * This method is called by the doBind() and doUnbind()
     * methods.  If the acceptor is null, the acceptor object will
     * be created and kicked off by the executor.  If the acceptor
     * object is null, probably already created and this class
     * is now working, then nothing will happen and the method
     * will just return.
     * 
     * 开启Acceptor， 这个方法在绑定和解绑时调用。
     * 如果负责接收来自客户端的连接请求的Acceptor未启动，则会使用本地executor启动它。
     */
    private void startupAcceptor() throws InterruptedException {
        // If the acceptor is not ready, clear the queues
        // TODO : they should already be clean : do we have to do that ?
    	// 1. 如果当前IoAcceptor初始化未完成，清理队列。
        if (!selectable) {
            registerQueue.clear();
            cancelQueue.clear();
        }

        // start the acceptor if not already started
        // 2. 如果负责接收来自客户端的连接请求的Acceptor未启动，则会使用本地executor启动它。
        Acceptor acceptor = acceptorRef.get();
        if (acceptor == null) {
            lock.acquire();
            acceptor = new Acceptor();
            if (acceptorRef.compareAndSet(null, acceptor)) {
                executeWorker(acceptor);
            } else {
                lock.release();
            }
        }
    }
    
    
    /**
     * This class is called by the startupAcceptor() method and is
     * placed into a NamePreservingRunnable class.
     * It's a thread accepting incoming connections from clients.
     * The loop is stopped when all the bound handlers are unbound.
     * 
     * 负责接收来自客户端的连接请求。
     * 这个类被包装在NamePreservingRunnable中，在startupAcceptor()方法中启动，
     * 直到所有绑定的处理器都解绑了才停止循环操作。
     */
    private class Acceptor implements Runnable {

		public void run() {
			// 1. 判断acceptorRef保存的实例是否就是本实例。
			assert (acceptorRef.get() == this);
			
			int nHandles = 0;
			
			// 2. Release the lock
			lock.release();
			
			// 3. 如果当前IoAcceptor是可用的，就一直做循环操作。
			while (selectable) {
				try {
					// Detect if we have some keys ready to be processed
                    // The select() will be woke up if some new connection
                    // have occurred, or if the selector has been explicitly
                    // woke up
					// 4. 检测是否有些key已经准备好需要处理。如果有新连接发生，select()方法会唤醒(读起来这么怪...)。
					int selected = select();
					
					// this actually sets the selector to OP_ACCEPT,
                    // and binds to the port on which this class will
                    // listen on
					// 5. 实际操作是：在服务器监听的本地Socket地址，将selector选择器和OP_ACCEPT绑定。
                    nHandles += registerHandles();
                    
                    // Now, if the number of registred handles is 0, we can
                    // quit the loop: we don't have any socket listening
                    // for incoming connection.
                    // 6. 如果注册的handle数是0，可以退出循环：因为没有监听本地Socket地址。
                    if (nHandles == 0) {
						acceptorRef.set(null);
						if (registerQueue.isEmpty() && cancelQueue.isEmpty()) {
							// 下面这段代码永远为true：因为上面刚刚设置了null，如果其它线程重新生成了acceptor，也和当前acceptor不是同一个了。
							assert (acceptorRef.get() != this);
							break;
						}
						if (!acceptorRef.compareAndSet(null, this)) {
							assert (acceptorRef.get() != this);
							break;
						}
						assert (acceptorRef.get() == this);
					}
                    if (selected > 0) {
                    	// We have some connection request, let's process
                        // them here.
                    	// 7. 处理客户端的连接请求
                        processHandles(selectedHandles());
					}
                    
                    // check to see if any cancellation request has been made.
                    // 8. 检测是否有解绑操作执行过(解绑时会添加取消请求到取消队列)
                    nHandles -= unregisterHandles();
				} catch (ClosedSelectorException cse) {
					// If the selector has been closed, we can exit the loop
                    ExceptionMonitor.getInstance().exceptionCaught(cse);
                    break;
				} catch (Exception e) {
					ExceptionMonitor.getInstance().exceptionCaught(e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        ExceptionMonitor.getInstance().exceptionCaught(e1);
                    }
				}
			}
			
			// Cleanup all the processors, and shutdown the acceptor.
			// 9. 清理所有processor、关闭当前acceptor。
			if (selectable && isDisposing()) {
				selectable = false;
				try {
					if (createdProcessor) {
						processor.dispose();
					}
				} finally {
					try {
						synchronized (disposalLock) {
							if (isDisposing()) {
								destroy();
							}
						}
					} catch (Exception e) {
                        ExceptionMonitor.getInstance().exceptionCaught(e);
                    } finally {
                        disposalFuture.setDone();
                    }
				}
			}
		}
		
		/**
         * This method will process new sessions for the Worker class.  All
         * keys that have had their status updates as per the Selector.selectedKeys()
         * method will be processed here.  Only keys that are ready to accept
         * connections are handled here.
         * <p/>
         * Session objects are created by making new instances of SocketSessionImpl
         * and passing the session object to the SocketIoProcessor class.
         * 
         * 这个方法负责处理新的session(即新连接的建立)。
         * 每次调用Selector.selectedKeys()方法，所有key的状态会更新。
         * 只有准备好接受连接请求的key会在这里处理。
         * IoSession会创建并初始化后传递给IoProcessor。
         */
        @SuppressWarnings("unchecked")
        private void processHandles(Iterator<H> handles) throws Exception {
        	while (handles.hasNext()) {
        		H handle = handles.next();
        		handles.remove();
        		
        		// Associates a new created connection to a processor,
                // and get back a session
        		// 返回关联到processor代表新建立的连接的IoSession
        		S session = accept(processor, handle);
        		if (session == null) {
					continue;
				}
        		
        		// 初始化session：主要是设置AttributeMap和WriteRequestQueue
        		initSession(session, null, null);
        		
        		// add the session to the SocketIoProcessor
        		// 将session加入processor，使processor开始处理session的所有I/O操作。
        		session.getProcessor().add(session);
        	}
        }
    }
    
    /**
     * Sets up the socket communications.  Sets items such as:
     * <p/>
     * Blocking
     * Reuse address
     * Receive buffer size
     * Bind to listen port
     * Registers OP_ACCEPT for selector
     * 
     * 设置socket连接。设置项如上所列。
     * 实际操作是：在服务器监听的本地Socket地址，将selector选择器和OP_ACCEPT绑定
     */
    private int registerHandles() {
    	while (true) {
    		// The register queue contains the list of services to manage
            // in this acceptor.
    		// 1. 注册队列里包含了当前acceptor里要处理的服务列表。
            AcceptorOperationFuture future = registerQueue.poll();
            if (future == null) {
				return 0;
			}
            
            // We create a temporary map to store the bound handles,
            // as we may have to remove them all if there is an exception
            // during the sockets opening.
            // 2. 使用一个临时Map来保存绑定的handler，因为如果打开socket时发生异常，这些handler要被移除。
            Map<SocketAddress, H> newHandles = new ConcurrentHashMap<SocketAddress, H>();
            
            try {
            	// Process all the addresses
            	// 3. 处理所有本地监听的Socket地址。
            	for (SocketAddress address : future.getLocalAddresses()) {
            		H handle = open(address);
            		// 为什么不是newHandles.put(address, handle) ?
            		newHandles.put(localAddress(handle), handle);
            	}
            	
            	// Everything went ok, we can now update the map storing
                // all the bound sockets.
            	// 4. 一切正常，现在可以把所有绑定的handler放入存储的Map。
                boundHandles.putAll(newHandles);
                
                // and notify.
                // 5. 发送通知：设置future操作已完成。
                future.setDone();
                
                // 6. 返回新建的服务端套接字通道数。
                return newHandles.size();
            } catch (Exception e) {
                // We store the exception in the future
                future.setException(e);
            } finally {
                // Roll back if failed to bind all addresses.
                if (future.getException() != null) {
                    for (H handle : newHandles.values()) {
                        try {
                            close(handle);
                        } catch (Exception e) {
                            ExceptionMonitor.getInstance().exceptionCaught(e);
                        }
                    }
                    // TODO : add some comment : what is the wakeup() waking up ?
                    wakeup();
                }
            }
    	}
    }
    
    /**
     * This method just checks to see if anything has been placed into the
     * cancellation queue.  The only thing that should be in the cancelQueue
     * is CancellationRequest objects and the only place this happens is in
     * the doUnbind() method.
     * 
     * 关闭指定本地Socket地址上的服务端socket连接。
     * 这个方法检测取消队列中是否有取消请求加入：只在解绑操作中会加入取消请求。
     */
    private int unregisterHandles() {
    	int cancelledHandles = 0;
    	while (true) {
    		AcceptorOperationFuture future = cancelQueue.poll();
    		if (future == null) {
				break;
			}
    		
    		// close the channels
    		// 关闭通道
    		for (SocketAddress address : future.getLocalAddresses()) {
				H handle = boundHandles.remove(address);
				if (handle == null) {
					continue;
				}
				try {
					close(handle);
					wakeup(); // wake up again to trigger thread death
				} catch (Exception e) {
					ExceptionMonitor.getInstance().exceptionCaught(e);
                } finally {
                    cancelledHandles++;
                }
			}
    	}
    	return cancelledHandles;
    }
    
    /**
     * {@inheritDoc}
     * 
     * 这里使用面向连接的传输方式，所以不支持newSession操作。
     */
    public final IoSession newSession(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @return the backLog
     * 
     * 返回能排队等待请求被接受的socket数，默认50个。
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Sets the Backlog parameter
     * 
     * 设置能排队等待请求被接受的socket数，默认50个。
     * 
     * @param backlog
     *            the backlog variable
     */
    public void setBacklog(int backlog) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException("backlog can't be set while the acceptor is bound.");
            }
            this.backlog = backlog;
        }
    }
    
    /**
     * @return the flag that sets the reuseAddress information
     * 
     * 返回监听的本地Socket地址是否可重用标识。
     */
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * Set the Reuse Address flag
     * 
     * 设置监听的本地Socket地址是否可重用标识。
     * 
     * @param reuseAddress
     *            The flag to set
     */
    public void setReuseAddress(boolean reuseAddress) {
        synchronized (bindLock) {
            if (isActive()) {
                throw new IllegalStateException("backlog can't be set while the acceptor is bound.");
            }
            this.reuseAddress = reuseAddress;
        }
    }

    /**
     * {@inheritDoc}
     */
    public SocketSessionConfig getSessionConfig() {
        return (SocketSessionConfig) sessionConfig;
    }
}
