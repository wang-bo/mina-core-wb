package org.apache.mina.core.write;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

/**
 * The default implementation of {@link WriteRequest}.
 * 
 * WriteRequest接口的默认实现
 * 
 * @date	2016年6月16日 上午10:35:27	completed
 */
public class DefaultWriteRequest implements WriteRequest {
	
	/** An empty message */
    public static final byte[] EMPTY_MESSAGE = new byte[] {};
    
    /** An empty WriteFuture */
    private static final WriteFuture UNUSED_FUTURE = new WriteFuture() {
    	public boolean isWritten() {
            return false;
        }

        public void setWritten() {
            // Do nothing
        }

        public IoSession getSession() {
            return null;
        }

        public void join() {
            // Do nothing
        }

        public boolean join(long timeoutInMillis) {
            return true;
        }

        public boolean isDone() {
            return true;
        }

        public WriteFuture addListener(IoFutureListener<?> listener) {
            throw new IllegalStateException("You can't add a listener to a dummy future.");
        }

        public WriteFuture removeListener(IoFutureListener<?> listener) {
            throw new IllegalStateException("You can't add a listener to a dummy future.");
        }

        public WriteFuture await() throws InterruptedException {
            return this;
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public boolean await(long timeoutMillis) throws InterruptedException {
            return true;
        }

        public WriteFuture awaitUninterruptibly() {
            return this;
        }

        public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
            return true;
        }

        public boolean awaitUninterruptibly(long timeoutMillis) {
            return true;
        }

        public Throwable getException() {
            return null;
        }

        public void setException(Throwable cause) {
            // Do nothing
        }
    };

	/** 待写的消息 */
	private final Object message;

	/** 写消息操作相关的WriteFuture */
    private final WriteFuture future;

    /** 待写的消息的目的地Socket地址 */
    private final SocketAddress destination;
    
    /**
     * Creates a new instance without {@link WriteFuture}.  You'll get
     * an instance of {@link WriteFuture} even if you called this constructor
     * because {@link #getFuture()} will return a bogus future.
     * 
     * 构造方法：没有使用参数WriteFuture，此时会伪造一个空的WriteFuture对象。
     * 
     * @param message The message that will be written
     */
    public DefaultWriteRequest(Object message) {
        this(message, null, null);
    }

    /**
     * Creates a new instance with {@link WriteFuture}.
     * 
     * 构造方法：使用参数WriteFuture
     * 
     * @param message The message that will be written
     * @param future The associated {@link WriteFuture}
     */
    public DefaultWriteRequest(Object message, WriteFuture future) {
        this(message, future, null);
    }

    /**
     * Creates a new instance.
     * 
     * 构造方法：使用所有参数
     *
     * @param message a message to write
     * @param future a future that needs to be notified when an operation is finished
     * @param destination the destination of the message.  This property will be
     *                    ignored unless the transport supports it.
     */
    public DefaultWriteRequest(Object message, WriteFuture future, SocketAddress destination) {
        if (message == null) {
            throw new IllegalArgumentException("message");
        }
        if (future == null) {
            future = UNUSED_FUTURE;
        }
        this.message = message;
        this.future = future;
        this.destination = destination;
    }
    
    /**
     * {@inheritDoc}
     */
    public WriteFuture getFuture() {
        return future;
    }

    /**
     * {@inheritDoc}
     */
    public Object getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    public WriteRequest getOriginalRequest() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getDestination() {
        return destination;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isEncoded() {
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WriteRequest: ");
        // Special case for the CLOSE_REQUEST writeRequest : it just
        // carries a native Object instance
        // 如果是CLOSE_REQUEST的写请求，则消息是一个Object对象，这个写请求对象定义在AbstractIoSession类中。
        if (message.getClass().getName().equals(Object.class.getName())) {
            sb.append("CLOSE_REQUEST");
        } else {
            if (getDestination() == null) {
                sb.append(message);
            } else {
                sb.append(message);
                sb.append(" => ");
                sb.append(getDestination());
            }
        }
        return sb.toString();
    }
}
