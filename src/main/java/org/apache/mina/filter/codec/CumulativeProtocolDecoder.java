package org.apache.mina.filter.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

/**
 * A {@link ProtocolDecoder} that cumulates the content of received buffers to a
 * <em>cumulative buffer</em> to help users implement decoders.
 * <p>
 * If the received {@link IoBuffer} is only a part of a message. decoders should
 * cumulate received buffers to make a message complete or to postpone decoding
 * until more buffers arrive.
 * <p>
 * Here is an example decoder that decodes CRLF terminated lines into
 * <code>Command</code> objects:
 * 
 * <pre>
 * public class CrLfTerminatedCommandLineDecoder extends CumulativeProtocolDecoder {
 * 
 *     private Command parseCommand(IoBuffer in) {
 *         // Convert the bytes in the specified buffer to a
 *         // Command object.
 *         ...
 *     }
 * 
 *     protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
 * 
 *         // Remember the initial position.
 *         int start = in.position();
 * 
 *         // Now find the first CRLF in the buffer.
 *         byte previous = 0;
 *         while (in.hasRemaining()) {
 *             byte current = in.get();
 * 
 *             if (previous == '\r' &amp;&amp; current == '\n') {
 *                 // Remember the current position and limit.
 *                 int position = in.position();
 *                 int limit = in.limit();
 *                 try {
 *                     in.position(start);
 *                     in.limit(position);
 *                     // The bytes between in.position() and in.limit()
 *                     // now contain a full CRLF terminated line.
 *                     out.write(parseCommand(in.slice()));
 *                 } finally {
 *                     // Set the position to point right after the
 *                     // detected line and set the limit to the old
 *                     // one.
 *                     in.position(position);
 *                     in.limit(limit);
 *                 }
 *                 // Decoded one line; CumulativeProtocolDecoder will
 *                 // call me again until I return false. So just
 *                 // return true until there are no more lines in the
 *                 // buffer.
 *                 return true;
 *             }
 * 
 *             previous = current;
 *         }
 * 
 *         // Could not find CRLF in the buffer. Reset the initial
 *         // position to the one we recorded above.
 *         in.position(start);
 * 
 *         return false;
 *     }
 * }
 * </pre>
 * <p>
 * Please note that this decoder simply forward the call to
 * {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)} if the
 * underlying transport doesn't have a packet fragmentation. Whether the
 * transport has fragmentation or not is determined by querying
 * {@link TransportMetadata}.
 * 
 * 1. 帮助处理断包粘包的解码器类。当到达的数据(IoBuffer)只是消息的一部分时，解码器需要保存这些数据，等后续数据到达时再解码成完整的消息对象。
 * 
 * 2. 上面有一个用法示例：解码CRLF结尾的命令行消息到Command对象中。
 * 
 * @date	2016年6月29日 上午10:47:14	completed
 */
public abstract class CumulativeProtocolDecoder extends ProtocolDecoderAdapter {
	
	/** 在session属性中保存时使用的key */
	private final AttributeKey BUFFER = new AttributeKey(getClass(), "buffer");

	/**
     * Creates a new instance.
     * 
     * 构造方法
     */
    protected CumulativeProtocolDecoder() {
        // Do nothing
    }
	
    /**
     * Cumulates content of <tt>in</tt> into internal buffer and forwards
     * decoding request to
     * {@link #doDecode(IoSession, IoBuffer, ProtocolDecoderOutput)}.
     * <tt>doDecode()</tt> is invoked repeatedly until it returns <tt>false</tt>
     * and the cumulative buffer is compacted after decoding ends.
     * 
     * 堆积接收到的数据到一个内部的buffer，并调用doDecode()方法做真正的解码操作。
     * doDecode()会一直被调用知道它返回false，然后会收缩内部buffer(即丢弃已解码的部分，保存未解码的部分)。
     *
     * @throws IllegalStateException
     *             if your <tt>doDecode()</tt> returned <tt>true</tt> not
     *             consuming the cumulative buffer.
     */
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {
		// 1. 底层协议不支持分片时，不做断包粘包处理。
		if (!session.getTransportMetadata().hasFragmentation()) {
			while (in.hasRemaining()) {
				if (!doDecode(session, in, out)) {
					break;
				}
			}
			return;
		}
		
		// 2. 底层协议支持分片时，做断包粘包处理。
		boolean usingSessionBuffer = true;
		IoBuffer buffer = (IoBuffer) session.getAttribute(BUFFER);
		// If we have a session buffer, append data to that; otherwise
        // use the buffer read from the network directly.
		// 3. 如果从session中获取到buffer，把这次接收到的数据添加进去，否则直接使用封装了这次接收到的数据的buffer(即参数in)。
		if (buffer != null) {
			boolean appended = false;
			// Make sure that the buffer is auto-expanded.
			// 4. 确认buffer是否支持自动扩展。
			if (buffer.isAutoExpand()) {
				try {
					buffer.put(in);
					appended = true;
				} catch (IllegalStateException e) {
                    // A user called derivation method (e.g. slice()),
                    // which disables auto-expansion of the parent buffer.
                } catch (IndexOutOfBoundsException e) {
                    // A user disabled auto-expansion.
                }
			}
			// 5. 数据添加成功：反转buffer，做好读取准备。
			if (appended) {
				buffer.flip();
			} 
			// Reallocate the buffer if append operation failed due to
            // derivation or disabled auto-expansion.
			// 6. 数据添加不成功：重新分配一个buffer，并把两部分数据都封装进去。
			else {
				buffer.flip();
				IoBuffer newBuffer = IoBuffer.allocate(buffer.remaining() + in.remaining()).setAutoExpand(true);
				newBuffer.order(buffer.order());
				newBuffer.put(buffer);
				newBuffer.put(in);
				newBuffer.flip();
				buffer = newBuffer;
				// Update the session attribute.
                session.setAttribute(BUFFER, buffer);
			}
		} else {
			buffer = in;
            usingSessionBuffer = false;
		}
		
		// 7. 做解码操作。
		while (true) {
			int oldPos = buffer.position();
			boolean decoded = doDecode(session, buffer, out);
			if (decoded) {
				if (buffer.position() == oldPos) {
					throw new IllegalStateException("doDecode() can't return true when buffer is not consumed.");
				}
				if (!buffer.hasRemaining()) {
					break;
				}
			} else {
				break;
			}
		}
		
		// if there is any data left that cannot be decoded, we store
        // it in a buffer in the session and next time this decoder is
        // invoked the session buffer gets appended to
		// 8. 当解码操作后，如果buffer中还有数据，将这部分数据保存到session中，
		// 当有新的数据到达时与这部分数据合并再解码。
		if (buffer.hasRemaining()) {
			if (usingSessionBuffer && buffer.isAutoExpand()) {
				// 丢弃已解码的数据，留下未解码的数据。
				buffer.compact();
			} else {
				storeRemainingInSession(buffer, session);
			}
		} else {
			if (usingSessionBuffer) {
				removeSessionBuffer(session);
			}
		}
	}
	
	/**
     * Implement this method to consume the specified cumulative buffer and
     * decode its content into message(s).
     * 
     * 子类实现这个方法，使用内部buffer中的数据来解码成消息对象。
     * 当buffer中有更多数据并希望继续解码时返回true。
     * 当buffer中剩余的数据不够解码时返回false，当更多数据到达时，这个方法会被再次调用。
     *
     * @param session The current Session
     * @param in the cumulative buffer
     * @param out The {@link ProtocolDecoderOutput} that will receive the decoded message
     * @return <tt>true</tt> if and only if there's more to decode in the buffer
     *         and you want to have <tt>doDecode</tt> method invoked again.
     *         Return <tt>false</tt> if remaining data is not enough to decode,
     *         then this method will be invoked again when more data is
     *         cumulated.
     * @throws Exception if cannot decode <tt>in</tt>.
     */
    protected abstract boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception;
	
	/**
	 * 从session中移除保存的buffer。
	 * @param session
	 */
	private void removeSessionBuffer(IoSession session) {
        session.removeAttribute(BUFFER);
    }
	
	/**
	 * 把buffer中的数据保存到session中，以备下次使用。
	 * @param buf
	 * @param session
	 */
	private void storeRemainingInSession(IoBuffer buf, IoSession session) {
		final IoBuffer remainingBuffer = IoBuffer.allocate(buf.capacity()).setAutoExpand(true);
		remainingBuffer.order(buf.order());
		remainingBuffer.put(buf);
		session.setAttribute(BUFFER, remainingBuffer);
	}

}
