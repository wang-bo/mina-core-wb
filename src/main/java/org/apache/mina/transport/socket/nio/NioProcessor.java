package org.apache.mina.transport.socket.nio;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.polling.AbstractPollingIoProcessor;
import org.apache.mina.core.session.SessionState;

/**
 * socket传输方式(TCP/IP)的IoProcessor接口实现类。
 * 
 * @date	2016年6月22日 上午9:17:29	completed
 */
public class NioProcessor extends AbstractPollingIoProcessor<NioSession> {

	/** The selector associated with this processor */
	private Selector selector;

    private SelectorProvider selectorProvider = null;
    
    /**
     * Creates a new instance of NioProcessor.
     * 
     * 构造方法
     *
     * @param executor The executor to use
     */
	public NioProcessor(Executor executor) {
		super(executor);
		try {
			// Open a new selector
			selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeIoException("Failed to open a selector.", e);
		}
	}

   /**
    *
    * Creates a new instance of NioProcessor.
    * 
    * 构造方法
    *
    * @param executor The executor to use
    * @param selectorProvider The Selector provider to use
    */
   public NioProcessor(Executor executor, SelectorProvider selectorProvider) {
       super(executor);
       try {
           // Open a new selector
           if (selectorProvider == null) {
               selector = Selector.open();
           } else {
               selector = selectorProvider.openSelector();
           }
       } catch (IOException e) {
           throw new RuntimeIoException("Failed to open a selector.", e);
       }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void doDispose() throws Exception {
       selector.close();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int select(long timeout) throws Exception {
       return selector.select(timeout);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int select() throws Exception {
       return selector.select();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isSelectorEmpty() {
       return selector.keys().isEmpty();
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void wakeup() {
       wakeupCalled.getAndSet(true);
       selector.wakeup();
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected Iterator<NioSession> allSessions() {
       return new IoSessionIterator<NioSession>(selector.keys());
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected Iterator<NioSession> selectedSessions() {
       return new IoSessionIterator<NioSession>(selector.selectedKeys());
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void init(NioSession session) throws Exception {
	   SelectableChannel channel = (SelectableChannel) session.getChannel();
	   channel.configureBlocking(false);
	   session.setSelectionKey(channel.register(selector, SelectionKey.OP_READ, session));
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void destroy(NioSession session) throws Exception {
	   ByteChannel channel = session.getChannel();
	   SelectionKey key = session.getSelectionKey();
	   if (key != null) {
		   key.cancel();
	   }
	   channel.close();
   }
   
	/**
	 * {@inheritDoc}
	 */
    @Override
	protected void registerNewSelector() throws IOException {
		synchronized (selector) {
			Set<SelectionKey> keys = selector.keys();
			// Open a new selector
			// 1. 打开一个新的选择器。
			Selector newSelector = null;
			if (selectorProvider == null) {
				newSelector = Selector.open();
			} else {
				newSelector = selectorProvider.openSelector();
			}
			
			// Loop on all the registered keys, and register them on the new selector
			// 2. 遍历旧的选择器上的所有key，注册到新选择器上。
			for (SelectionKey key : keys) {
				SelectableChannel channel = key.channel();
				// Don't forget to attache the session, and back !
				NioSession session = (NioSession) key.attachment();
				SelectionKey newKey = channel.register(newSelector, key.interestOps(), session);
				session.setSelectionKey(newKey);
			}
			
			// Now we can close the old selector and switch it
			// 3. 关闭旧选择器，并切换到新选择器。
			selector.close();
			selector = newSelector;
		}
	}
    
    /**
	 * {@inheritDoc}
	 */
    @Override
    protected boolean isBrokenConnection() throws IOException {
    	// A flag set to true if we find a broken session
        boolean brokenSession = false;
        
        synchronized (selector) {
        	// Get the selector keys
        	Set<SelectionKey> keys = selector.keys();
        	
        	// Loop on all the keys to see if one of them
            // has a closed channel
        	for (SelectionKey key : keys) {
				SelectableChannel channel = key.channel();
				if (
					// UDP通道且未连接
					(((channel instanceof DatagramChannel) && !((DatagramChannel) channel).isConnected()))
					// TCP通道且未连接
					|| ((channel instanceof SocketChannel) && !((SocketChannel) channel).isConnected())
						) {
					// The channel is not connected anymore. Cancel
                    // the associated key then.
					// 通道已不再连接，取消关联的key。
					key.cancel();
					// Set the flag to true to avoid a selector switch
                    brokenSession = true;
				}
			}
		}
        return brokenSession;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected SessionState getState(NioSession session) {
    	SelectionKey key = session.getSelectionKey();
    	if (key == null) {
    		// The channel is not yet registered to a selector
    		// session关联的通道还未注册到选择器中。
            return SessionState.OPENING;
		}
    	if (key.isValid()) {
			// The session is opened
    		return SessionState.OPENED;
		} else {
			// The session still as to be closed.
			return SessionState.CLOSING;
		}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadable(NioSession session) {
    	SelectionKey key = session.getSelectionKey();
    	return key != null && key.isValid() && key.isReadable();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isWritable(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return (key != null) && key.isValid() && key.isWritable();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isInterestedInRead(NioSession session) {
        SelectionKey key = session.getSelectionKey();
        return (key != null) && key.isValid() && ((key.interestOps() & SelectionKey.OP_READ) != 0);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInRead(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        if ((key == null) || !key.isValid()) {
            return;
        }
        int oldInterestOps = key.interestOps();
        int newInterestOps = oldInterestOps;
        if (isInterested) {
            newInterestOps |= SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_READ;
        }
        if (oldInterestOps != newInterestOps) {
            key.interestOps(newInterestOps);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInterestedInWrite(NioSession session, boolean isInterested) throws Exception {
        SelectionKey key = session.getSelectionKey();
        if ((key == null) || !key.isValid()) {
            return;
        }
        int newInterestOps = key.interestOps();
        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }
        key.interestOps(newInterestOps);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected int read(NioSession session, IoBuffer buf) throws Exception {
    	ByteChannel channel = session.getChannel();
    	return channel.read(buf.buf());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected int write(NioSession session, IoBuffer buf, int length) throws Exception {
    	if (buf.remaining() <= length) {
			return session.getChannel().write(buf.buf());
		}
    	int oldLimit = buf.limit();
    	buf.limit(buf.position() + length);
    	try {
    		return session.getChannel().write(buf.buf());
    	} finally {
    		buf.limit(oldLimit);
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected int transferFile(NioSession session, FileRegion region, int length) throws Exception {
    	try {
    		return (int) region.getFileChannel().transferTo(region.getPosition(), length, session.getChannel());
    	} catch (IOException e) {
            // Check to see if the IOException is being thrown due to
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103988
    		String message = e.getMessage();
    		if ((message != null) && message.contains("temporarily unavailable")) {
				return 0;
			}
    		throw e;
    	}
    }
    
   /**
    * An encapsulating iterator around the {@link Selector#selectedKeys()} or
    * the {@link Selector#keys()} iterator;
    * 
    * 封装Selector.selectedKeys()或Selector.keys()的迭代器
    */
   protected static class IoSessionIterator<NioSession> implements Iterator<NioSession> {
	   
	   private final Iterator<SelectionKey> iterator;
	   
	   /**
        * Create this iterator as a wrapper on top of the selectionKey Set.
        *
        * @param keys
        *            The set of selected sessions
        */
       private IoSessionIterator(Set<SelectionKey> keys) {
           iterator = keys.iterator();
       }
       
       /**
        * {@inheritDoc}
        */
       public boolean hasNext() {
           return iterator.hasNext();
       }
       
       /**
        * {@inheritDoc}
        */
       public NioSession next() {
           SelectionKey key = iterator.next();
           NioSession nioSession = (NioSession) key.attachment();
           return nioSession;
       }

       /**
        * {@inheritDoc}
        */
       public void remove() {
           iterator.remove();
       }
   }
}
