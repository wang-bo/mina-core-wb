package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.apache.mina.core.polling.AbstractPollingIoAcceptor;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoProcessor;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.SimpleIoProcessorPool;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.transport.socket.DefaultSocketSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;

/**
 * {@link IoAcceptor} for socket transport (TCP/IP).  This class
 * handles incoming TCP/IP based socket connections.
 * 
 * socket传输方式(TCP/IP)的IoAcceptor接口实现类。
 * 这个类基于socket连接处理客户端请求的TCP/IP包。
 * 
 * @date	2016年6月20日 上午11:59:00	completed
 */
public final class NioSocketAcceptor extends AbstractPollingIoAcceptor<NioSession, ServerSocketChannel> 
		implements SocketAcceptor {

	private volatile Selector selector;
	
    private volatile SelectorProvider selectorProvider = null;
    
    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters (multiple thread model).
     * 
     * 无参构造方法：使用默认参数DefaultSocketSessionConfig和NioProcessor。
     */
    public NioSocketAcceptor() {
        super(new DefaultSocketSessionConfig(), NioProcessor.class);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }
    
    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters, and
     * given number of {@link NioProcessor} for multithreading I/O operations.
     * 
     * 构造方法：指定了SimpleIoProcessorPool池中processor数，使用默认参数DefaultSocketSessionConfig和NioProcessor。
     * 
     * @param processorCount the number of processor to create and place in a
     * {@link SimpleIoProcessorPool}
     */
    public NioSocketAcceptor(int processorCount) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }
    
    /**
     * Constructor for {@link NioSocketAcceptor} with default configuration but a
     * specific {@link IoProcessor}, useful for sharing the same processor over multiple
     * {@link IoService} of the same type.
     *  
     * 构造方法：指定了IoProcessor，当多个IoService公用同一个IoProcessor适用这个构造方法。
     *  
     * @param processor the processor to use for managing I/O events
     */
    public NioSocketAcceptor(IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }
    
    /**
     * Constructor for {@link NioSocketAcceptor} with a given {@link Executor} for handling
     * connection events and a given {@link IoProcessor} for handling I/O events, useful for
     * sharing the same processor and executor over multiple {@link IoService} of the same type.
     *  
     *  构造方法：指定了Executor来处理连接事件，指定了IoProcessor，来处理I/O事件。
     *  
     * @param executor the executor for connection
     * @param processor the processor for I/O operations
     */
    public NioSocketAcceptor(Executor executor, IoProcessor<NioSession> processor) {
        super(new DefaultSocketSessionConfig(), executor, processor);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
    }
    
    /**
     * Constructor for {@link NioSocketAcceptor} using default parameters, and
     * given number of {@link NioProcessor} for multithreading I/O operations, and
     * a custom SelectorProvider for NIO
     * 
     * 构造方法：指定了SimpleIoProcessorPool池中processor数，指定了一个自定义的NIO使用的SelectorProvider。
     *
     * @param processorCount the number of processor to create and place in a
     * @param selectorProvider teh SelectorProvider to use
     * {@link SimpleIoProcessorPool}
     */
    public NioSocketAcceptor(int processorCount, SelectorProvider selectorProvider) {
        super(new DefaultSocketSessionConfig(), NioProcessor.class, processorCount, selectorProvider);
        ((DefaultSocketSessionConfig) getSessionConfig()).init(this);
        // 这一行代码重复了，在下面的init(SelectorProvider)方法中又赋值了一次。
        this.selectorProvider = selectorProvider;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {
        selector = Selector.open();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void init(SelectorProvider selectorProvider) throws Exception {
        this.selectorProvider = selectorProvider;
        if (selectorProvider == null) {
            selector = Selector.open();
        } else {
            selector = selectorProvider.openSelector();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroy() throws Exception {
        if (selector != null) {
            selector.close();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public TransportMetadata getTransportMetadata() {
        return NioSocketSession.METADATA;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) super.getLocalAddress();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InetSocketAddress getDefaultLocalAddress() {
        return (InetSocketAddress) super.getDefaultLocalAddress();
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDefaultLocalAddress(InetSocketAddress localAddress) {
        setDefaultLocalAddress((SocketAddress) localAddress);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected NioSession accept(IoProcessor<NioSession> processor, ServerSocketChannel handle) throws Exception {
    	SelectionKey key = null;
    	if (handle != null) {
    		// 返回通道在指定选择器上注册的键。
			key = handle.keyFor(selector);
		}
    	// 判断key是否有效、key的通道是否已准备好接受新的套接字连接。
    	if (key == null || !key.isValid() || !key.isAcceptable()) {
			return null;
		}
    	// accept the connection from the client
    	// 接受客户端的连接请求。
    	SocketChannel channel = handle.accept();
    	if (channel == null) {
			return null;
		}
    	return new NioSocketSession(this, processor, channel);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected ServerSocketChannel open(SocketAddress localAddress) throws Exception {
    	// Creates the listening ServerSocket
    	
    	// 1. 创建ServerSocketChannel
    	ServerSocketChannel channel = null;
    	if (selectorProvider != null) {
			channel = selectorProvider.openServerSocketChannel();
		} else {
			// 其实是使用系统级默认SelectorProvider对象的openServerSocketChannel()方法。
			channel = ServerSocketChannel.open();
		}
    	
    	boolean success = false;
    	try {
    		// 2. This is a non blocking socket channel
    		// 设置为非阻塞的通道
    		channel.configureBlocking(false);
    		// 3. Configure the server socket,
    		// 配置ServerSocket
    		ServerSocket socket = channel.socket();
    		// 4. Set the reuseAddress flag accordingly with the setting
    		// 根据配置设置是否可重用本地Socket地址
    		socket.setReuseAddress(isReuseAddress());
    		
    		// 5. and bind.
    		// 绑定指定的本地Socket地址。
    		try {
    			socket.bind(localAddress, getBacklog());
    		} catch (IOException ioe) {
    			// 7. Add some info regarding the address we try to bind to the
                // message
    			// 绑定失败的提示信息
    			String newMessage = "Error while binding on " + localAddress + "\n" + "original message : "
                        + ioe.getMessage();
    			Exception e = new IOException(newMessage);
    			
    			// 8. And close the channel
    			// 关闭通道
    			channel.close();
    			throw e;
    		}
    		
    		// 6. Register the channel within the selector for ACCEPT event
    		// 往selector中注册ACCEPT事件
    		channel.register(selector, SelectionKey.OP_ACCEPT);
    		success = true;
    	} finally {
    		if (!success) {
				close(channel);
			}
    	}
    	return channel;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected SocketAddress localAddress(ServerSocketChannel handle) throws Exception {
        return handle.socket().getLocalSocketAddress();
    }
    
    /**
     * Check if we have at least one key whose corresponding channels is
     * ready for I/O operations.
     *
     * This method performs a blocking selection operation.
     * It returns only after at least one channel is selected,
     * this selector's wakeup method is invoked, or the current thread
     * is interrupted, whichever comes first.
     * 
     * 检测是否至少一个key代表的通道已准备好做I/O操作。
     * 这个方法是个阻塞操作，只有当至少一个通道做好I/O操作准备时才返回。
     * 除非当前selector的wakeup()方法被调用、或当前线程被中断才停止阻塞。
     * 
     * @return The number of keys having their ready-operation set updated
     * @throws IOException If an I/O error occurs
     * @throws ClosedSelectorException If this selector is closed
     */
    @Override
    protected int select() throws Exception {
        return selector.select();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<ServerSocketChannel> selectedHandles() {
        return new ServerSocketChannelIterator(selector.selectedKeys());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void close(ServerSocketChannel handle) throws Exception {
    	// 返回通道在指定选择器上注册的键。
    	SelectionKey key = handle.keyFor(selector);
    	if (key != null) {
    		// 取消注册
			key.cancel();
		}
    	// 关闭通道：如果已关闭则立即返回，否则会将该通道标记为已关闭，然后调用implCloseChannel()方法完成关闭操作。 
    	handle.close();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void wakeup() {
        selector.wakeup();
    }
    
    /**
     * Defines an iterator for the selected-key Set returned by the
     * selector.selectedKeys(). It replaces the SelectionKey operator.
     * 
     * 定义一个操作selector.selectedKeys()返回的selected-key集合的迭代器。
     * 使用ServerSocketChannel来代替SelectionKey的操作。
     */
    private static class ServerSocketChannelIterator implements Iterator<ServerSocketChannel> {
    	
    	/** The selected-key iterator */
    	private final Iterator<SelectionKey> iterator;
    	
    	/**
         * Build a SocketChannel iterator which will return a SocketChannel instead of
         * a SelectionKey.
         * 
         * 构造方法
         * 
         * @param selectedKeys The selector selected-key set
         */
        private ServerSocketChannelIterator(Collection<SelectionKey> selectedKeys) {
            iterator = selectedKeys.iterator();
        }
        
        /**
         * Tells if there are more SockectChannel left in the iterator
         * 
         * 返回迭代器中是否还有ServerSocketChannel对象。
         * 
         * @return <tt>true</tt> if there is at least one more
         * SockectChannel object to read
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        /**
         * Get the next SocketChannel in the operator we have built from
         * the selected-key et for this selector.
         * 
         * 返回迭代器中的下一个ServerSocketChannel对象。
         * 
         * @return The next SocketChannel in the iterator
         */
        public ServerSocketChannel next() {
            SelectionKey key = iterator.next();
            // 判断key是否有效、key的通道是否已准备好接受新的套接字连接。
            if (key.isValid() && key.isAcceptable()) {
                return (ServerSocketChannel) key.channel();
            }
            return null;
        }
        
        /**
         * Remove the current SocketChannel from the iterator
         * 
         * 删除迭代器中当前的ServerSocketChannel对象。
         */
        public void remove() {
            iterator.remove();
        }
    }
}
