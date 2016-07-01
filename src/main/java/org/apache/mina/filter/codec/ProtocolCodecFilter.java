package org.apache.mina.filter.codec;

import java.net.SocketAddress;
import java.util.Queue;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.file.FileRegion;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.DefaultWriteFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.NothingWrittenException;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IoFilter} which translates binary or protocol specific data into
 * message objects and vice versa using {@link ProtocolCodecFactory},
 * {@link ProtocolEncoder}, or {@link ProtocolDecoder}.
 * 
 * 一个将二进制信息与消息对象转换的过滤器。
 * 使用ProtocolCodecFactory、ProtocolEncoder、ProtocolDecoder
 * 
 * ['protə'kɔl] ['kodɛk]
 * 
 * @date	2016年6月28日 下午5:04:50	completed
 */
public class ProtocolCodecFilter extends IoFilterAdapter {

	/** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCodecFilter.class);
    
    private static final Class<?>[] EMPTY_PARAMS = new Class[0];

    private static final IoBuffer EMPTY_BUFFER = IoBuffer.wrap(new byte[0]);

    private static final AttributeKey ENCODER = new AttributeKey(ProtocolCodecFilter.class, "encoder");

    private static final AttributeKey DECODER = new AttributeKey(ProtocolCodecFilter.class, "decoder");

    private static final AttributeKey DECODER_OUT = new AttributeKey(ProtocolCodecFilter.class, "decoderOut");

    private static final AttributeKey ENCODER_OUT = new AttributeKey(ProtocolCodecFilter.class, "encoderOut");
    
    /** The factory responsible for creating the encoder and decoder 
     *  创建编解码器的工厂 */
    private final ProtocolCodecFactory factory;
    
    /**
     * Creates a new instance of ProtocolCodecFilter, associating a factory
     * for the creation of the encoder and decoder.
     * 
     * 构造方法：使用ProtocolCodecFactory做参数。
     *
     * @param factory The associated factory
     */
    public ProtocolCodecFilter(ProtocolCodecFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }
        this.factory = factory;
    }
    
    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory.
     * The encoder/decoder factory will be created as an inner class, using
     * the two parameters (encoder and decoder).
     * 
     * 构造方法：使用ProtocolEncoder、ProtocolDecoder做参数。
     * 
     * @param encoder The class responsible for encoding the message
     * @param decoder The class responsible for decoding the message
     */
    public ProtocolCodecFilter(final ProtocolEncoder encoder, final ProtocolDecoder decoder) {
        if (encoder == null) {
            throw new IllegalArgumentException("encoder");
        }
        if (decoder == null) {
            throw new IllegalArgumentException("decoder");
        }
        // Create the inner Factory based on the two parameters
        this.factory = new ProtocolCodecFactory() {
            public ProtocolEncoder getEncoder(IoSession session) {
                return encoder;
            }
            public ProtocolDecoder getDecoder(IoSession session) {
                return decoder;
            }
        };
    }

    /**
     * Creates a new instance of ProtocolCodecFilter, without any factory.
     * The encoder/decoder factory will be created as an inner class, using
     * the two parameters (encoder and decoder), which are class names. Instances
     * for those classes will be created in this constructor.
     * 
     * 构造方法：使用ProtocolEncoder类、ProtocolDecoder类做参数。
     * 
     * @param encoderClass The class responsible for encoding the message
     * @param decoderClass The class responsible for decoding the message
     */
    public ProtocolCodecFilter(final Class<? extends ProtocolEncoder> encoderClass,
            final Class<? extends ProtocolDecoder> decoderClass) {
        if (encoderClass == null) {
            throw new IllegalArgumentException("encoderClass");
        }
        if (decoderClass == null) {
            throw new IllegalArgumentException("decoderClass");
        }
        if (!ProtocolEncoder.class.isAssignableFrom(encoderClass)) {
            throw new IllegalArgumentException("encoderClass: " + encoderClass.getName());
        }
        if (!ProtocolDecoder.class.isAssignableFrom(decoderClass)) {
            throw new IllegalArgumentException("decoderClass: " + decoderClass.getName());
        }
        try {
            encoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("encoderClass doesn't have a public default constructor.");
        }
        try {
            decoderClass.getConstructor(EMPTY_PARAMS);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("decoderClass doesn't have a public default constructor.");
        }

        final ProtocolEncoder encoder;
        try {
            encoder = encoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("encoderClass cannot be initialized");
        }

        final ProtocolDecoder decoder;
        try {
            decoder = decoderClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("decoderClass cannot be initialized");
        }

        // Create the inner factory based on the two parameters.
        this.factory = new ProtocolCodecFactory() {
            public ProtocolEncoder getEncoder(IoSession session) throws Exception {
                return encoder;
            }
            public ProtocolDecoder getDecoder(IoSession session) throws Exception {
                return decoder;
            }
        };
    }
    
    /**
     * Get the encoder instance from a given session.
     * 
     * 返回指定session中的encoder实例。
     *
     * @param session The associated session we will get the encoder from
     * @return The encoder instance, if any
     */
    public ProtocolEncoder getEncoder(IoSession session) {
        return (ProtocolEncoder) session.getAttribute(ENCODER);
    }
    
    /**
     * ${@inheritDoc}
     */
    @Override
    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        if (parent.contains(this)) {
            throw new IllegalArgumentException(
                    "You can't add the same filter instance more than once.  Create another instance and add it.");
        }
    }
    
    /**
     * ${@inheritDoc}
     */
    @Override
    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
        // Clean everything
        disposeCodec(parent.getSession());
    }
    
    /**
     * Process the incoming message, calling the session decoder. As the incoming
     * buffer might contains more than one messages, we have to loop until the decoder
     * throws an exception.
     * 
     *  while ( buffer not empty )
     *    try
     *      decode ( buffer )
     *    catch
     *      break;
     * 
     * 处理接收到的数据，接收到的数据中可能包含多个消息，所以我们需要循环处理，直到解码器抛出异常。
     */
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
    	LOGGER.debug("Processing a MESSAGE_RECEIVED for session {}", session.getId());
    	if (!(message instanceof IoBuffer)) {
			nextFilter.messageReceived(session, message);
			return;
		}
    	
    	IoBuffer in = (IoBuffer) message;
    	ProtocolDecoder decoder = factory.getDecoder(session);
    	ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);
    	
    	// Loop until we don't have anymore byte in the buffer,
        // or until the decoder throws an unrecoverable exception or
        // can't decoder a message, because there are not enough
        // data in the buffer
    	// 循环直到buffer中没有数据，或者decoder抛出异常，或者没有足够数据解码成消息对象为止。
    	while (in.hasRemaining()) {
    		int oldPos = in.position();
    		try {
    			synchronized (session) {
    				// Call the decoder with the read bytes
    				decoder.decode(session, in, decoderOut);
				}
    			// Finish decoding if no exception was thrown.
    			decoderOut.flush(nextFilter, session);
    		} catch (Exception e) {
    			ProtocolDecoderException pde;
    			if (e instanceof ProtocolDecoderException) {
                    pde = (ProtocolDecoderException) e;
                } else {
                    pde = new ProtocolDecoderException(e);
                }
    			if (pde.getHexdump() == null) {
    				// Generate a message hex dump
                    int curPos = in.position();
                    in.position(oldPos);
                    pde.setHexdump(in.getHexDump());
                    in.position(curPos);
    			}
    			// Fire the exceptionCaught event.
    			// 触发捕获异常事件。
                decoderOut.flush(nextFilter, session);
                nextFilter.exceptionCaught(session, pde);
                // Retry only if the type of the caught exception is
                // recoverable and the buffer position has changed.
                // We check buffer position additionally to prevent an
                // infinite loop.
                // 当捕获的异常不是RecoverableProtocolDecoderException时重试。
                // 并检测position的位置，以防无穷循环。
                if (!(e instanceof RecoverableProtocolDecoderException) || (in.position() == oldPos)) {
                    break;
                }
    		}
    	}
    }
    
    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
    	if (writeRequest instanceof EncodedWriteRequest) {
            return;
        }
    	if (writeRequest instanceof MessageWriteRequest) {
            MessageWriteRequest wrappedRequest = (MessageWriteRequest) writeRequest;
            nextFilter.messageSent(session, wrappedRequest.getParentRequest());
        } else {
            nextFilter.messageSent(session, writeRequest);
        }
    }
    
    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
    	Object message = writeRequest.getMessage();
    	
    	// Bypass the encoding if the message is contained in a IoBuffer,
        // as it has already been encoded before
        if ((message instanceof IoBuffer) || (message instanceof FileRegion)) {
            nextFilter.filterWrite(session, writeRequest);
            return;
        }
        
        // Get the encoder in the session
        ProtocolEncoder encoder = factory.getEncoder(session);
        ProtocolEncoderOutput encoderOut = getEncoderOut(session, nextFilter, writeRequest);
        if (encoder == null) {
            throw new ProtocolEncoderException("The encoder is null for the session " + session);
        }
        
        try {
        	// Now we can try to encode the response
        	// 尝试编码待写的消息对象。
            encoder.encode(session, message, encoderOut);
            // Send it directly
            // 直接发送
            Queue<Object> bufferQueue = ((AbstractProtocolEncoderOutput) encoderOut).getMessageQueue();
            // Write all the encoded messages now
            // 写所有已编码完成的消息
            while (!bufferQueue.isEmpty()) {
            	Object encodedMessage = bufferQueue.poll();
                if (encodedMessage == null) {
                    break;
                }
                // Flush only when the buffer has remaining.
                // 只有在buffer里有内容的时候才flush。
                if (!(encodedMessage instanceof IoBuffer) || ((IoBuffer) encodedMessage).hasRemaining()) {
                	SocketAddress destination = writeRequest.getDestination();
                	WriteRequest encodedWriteRequest = new EncodedWriteRequest(encodedMessage, null, destination);
                	nextFilter.filterWrite(session, encodedWriteRequest);
                }
            }
            // Call the next filter
            // 疑问：上一步结束后，为什么把使用MessageWriteRequest包装原始的WriteRequest，并触发后续过滤器filterWrite事件，
			// 	这个MessageWriteRequest包装后，message就变成了空的IoBuffer，这样processor处理时不会做写操作。
            // 解疑：实际上是为了messageSent事件的正确性，这里processor处理时不会做写操作，但是会触发过滤器链的messageSent事件，
            // 	因为上层过滤器和handler不知道消息被编码了，所以消息发送后触发messageSent事件时，到了这个过滤器后，要触发后续过滤器未编码消息的messageSent事件。
            nextFilter.filterWrite(session, new MessageWriteRequest(writeRequest));
        } catch (Exception e) {
            ProtocolEncoderException pee;
            // Generate the correct exception
            if (e instanceof ProtocolEncoderException) {
                pee = (ProtocolEncoderException) e;
            } else {
                pee = new ProtocolEncoderException(e);
            }
            throw pee;
        }
    }
    
    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
    	// Call finishDecode() first when a connection is closed.
    	ProtocolDecoder decoder = factory.getDecoder(session);
        ProtocolDecoderOutput decoderOut = getDecoderOut(session, nextFilter);
        try {
            decoder.finishDecode(session, decoderOut);
        } catch (Exception e) {
            ProtocolDecoderException pde;
            if (e instanceof ProtocolDecoderException) {
                pde = (ProtocolDecoderException) e;
            } else {
                pde = new ProtocolDecoderException(e);
            }
            throw pde;
        } finally {
            // Dispose everything
            disposeCodec(session);
            decoderOut.flush(nextFilter, session);
        }
        // Call the next filter
        nextFilter.sessionClosed(session);
    }
    
    /**
     * 已编码的写请求
     */
    private static class EncodedWriteRequest extends DefaultWriteRequest {
    	
        public EncodedWriteRequest(Object encodedMessage, WriteFuture future, SocketAddress destination) {
            super(encodedMessage, future, destination);
        }

        public boolean isEncoded() {
            return true;
        }
    }
    
    /**
     * 有消息的写请求，包装了真正的写请求。
     */
    private static class MessageWriteRequest extends WriteRequestWrapper {
        public MessageWriteRequest(WriteRequest writeRequest) {
            super(writeRequest);
        }

        @Override
        public Object getMessage() {
            return EMPTY_BUFFER;
        }

        @Override
        public String toString() {
            return "MessageWriteRequest, parent : " + super.toString();
        }
    }
    
    /**
     * ProtocolDecoderOutput接口的实现类。
     */
    private static class ProtocolDecoderOutputImpl extends AbstractProtocolDecoderOutput {
    	
    	public ProtocolDecoderOutputImpl() {
            // Do nothing
        }
    	
    	/**
    	 * ${@inheritDoc}
    	 */
    	public void flush(NextFilter nextFilter, IoSession session) {
    		Queue<Object> messageQueue = getMessageQueue();
    		// 遍历队列，把解码出的所有消息都触发消息接收事件。
    		while (!messageQueue.isEmpty()) {
    			nextFilter.messageReceived(session, messageQueue.poll());
    		}
    	}
    }
    
    /**
     * ProtocolEncoderOutput接口的实现类。
     */
    private static class ProtocolEncoderOutputImpl extends AbstractProtocolEncoderOutput {
    	
    	private final IoSession session;

        private final NextFilter nextFilter;

        /** The WriteRequest destination. 写请求要发送的目的地Socket地址 */
        private final SocketAddress destination;
        
        public ProtocolEncoderOutputImpl(IoSession session, NextFilter nextFilter, WriteRequest writeRequest) {
            this.session = session;
            this.nextFilter = nextFilter;
            // Only store the destination, not the full WriteRequest.
            destination = writeRequest.getDestination();
        }
        
        /**
         * ${@inheritDoc}
         */
        public WriteFuture flush() {
        	Queue<Object> bufferQueue = getMessageQueue();
            WriteFuture future = null;
            while (!bufferQueue.isEmpty()) {
            	Object encodedMessage = bufferQueue.poll();
            	if (encodedMessage == null) {
					break;
				}
            	// Flush only when the buffer has remaining.
            	// 只有buffer中有数据时才做flush。
            	if (!(encodedMessage instanceof IoBuffer) || ((IoBuffer) encodedMessage).hasRemaining()) {
					future = new DefaultWriteFuture(session);
					nextFilter.filterWrite(session, new EncodedWriteRequest(encodedMessage, future, destination));
				}
            }
            if (future == null) {
            	// Creates an empty writeRequest containing the destination
            	WriteRequest writeRequest = new DefaultWriteRequest(
            			DefaultWriteRequest.EMPTY_MESSAGE, null, destination);
            	future = DefaultWriteFuture.newNotWrittenFuture(session, new NothingWrittenException(writeRequest));
			}
            return future;
        }
    }
    
	//----------- Helper methods ---------------------------------------------
    
    /**
     * Dispose the encoder, decoder, and the callback for the decoded
     * messages.
     * 
     * 释放encoder、decoder、ProtocolDecoderOutput
     */
    private void disposeCodec(IoSession session) {
        // We just remove the two instances of encoder/decoder to release resources
        // from the session
        disposeEncoder(session);
        disposeDecoder(session);
        // We also remove the callback
        disposeDecoderOut(session);
    }
    
    /**
     * Dispose the encoder, removing its instance from the
     * session's attributes, and calling the associated
     * dispose method.
     * 
     * 释放encoder的资源：从session的属性中移除，然后调用dispose()方法。
     */
    private void disposeEncoder(IoSession session) {
    	ProtocolEncoder encoder = (ProtocolEncoder) session.removeAttribute(ENCODER);
        if (encoder == null) {
            return;
        }
        try {
            encoder.dispose(session);
        } catch (Exception e) {
            LOGGER.warn("Failed to dispose: " + encoder.getClass().getName() + " (" + encoder + ')');
        }
    }
    
    /**
     * Dispose the decoder, removing its instance from the
     * session's attributes, and calling the associated
     * dispose method.
     * 
     * 释放decoder的资源：从session的属性中移除，然后调用dispose()方法。
     */
    private void disposeDecoder(IoSession session) {
        ProtocolDecoder decoder = (ProtocolDecoder) session.removeAttribute(DECODER);
        if (decoder == null) {
            return;
        }
        try {
            decoder.dispose(session);
        } catch (Exception e) {
            LOGGER.warn("Failed to dispose: " + decoder.getClass().getName() + " (" + decoder + ')');
        }
    }
    
    
    /**
     * Return a reference to the decoder callback. If it's not already created
     * and stored into the session, we create a new instance.
     * 
     * 返回解码器要回调的ProtocolDecoderOutput。
     * 如果session中还没有，就创建一个。
     */
    private ProtocolDecoderOutput getDecoderOut(IoSession session, NextFilter nextFilter) {
    	ProtocolDecoderOutput out = (ProtocolDecoderOutput) session.getAttribute(DECODER_OUT);
    	if (out == null) {
    		// Create a new instance, and stores it into the session
            out = new ProtocolDecoderOutputImpl();
            session.setAttribute(DECODER_OUT, out);
		}
    	return out;
    }
    
    /**
     * 返回编码器要回调的ProtocolEncoderOutput。
     * 如果session中还没有，就创建一个。
     */
    private ProtocolEncoderOutput getEncoderOut(IoSession session, NextFilter nextFilter, WriteRequest writeRequest) {
        ProtocolEncoderOutput out = (ProtocolEncoderOutput) session.getAttribute(ENCODER_OUT);
        if (out == null) {
            // Create a new instance, and stores it into the session
            out = new ProtocolEncoderOutputImpl(session, nextFilter, writeRequest);
            session.setAttribute(ENCODER_OUT, out);
        }
        return out;
    }
    
    /**
     * Remove the decoder callback from the session's attributes.
     * 
     * 释放ProtocolDecoderOutput：从session的属性中移除。
     */
    private void disposeDecoderOut(IoSession session) {
        session.removeAttribute(DECODER_OUT);
    }
}
