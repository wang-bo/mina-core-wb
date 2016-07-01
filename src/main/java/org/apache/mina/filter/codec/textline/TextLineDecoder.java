package org.apache.mina.filter.codec.textline;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.core.buffer.BufferDataException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.RecoverableProtocolDecoderException;

/**
 * A {@link ProtocolDecoder} which decodes a text line into a string.
 * 
 * 将文本行解码成字符串的解码器
 * 
 * @date	2016年6月29日 下午4:48:19	completed
 */
public class TextLineDecoder implements ProtocolDecoder {

	private final AttributeKey CONTEXT = new AttributeKey(getClass(), "context");
	
	private final Charset charset;
	
	/** The delimiter used to determinate when a line has been fully decoded. 文本行分隔符 */
    private final LineDelimiter delimiter;
    
    /** An IoBuffer containing the delimiter. 包含分隔符字符串的buffer */
    private IoBuffer delimBuf;
    
    /** The default maximum Line length. Default to 1024. 最大行大小，默认1024 */
    private int maxLineLength = 1024;

    /** The default maximum buffer length. Default to 128 chars. 最大buffer大小，默认128 */
    private int bufferLength = 128;
    
    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#AUTO} delimiter.
     * 
     * 构造方法：使用默认的Charset和自动识别分隔符。
     */
    public TextLineDecoder() {
        this(LineDelimiter.AUTO);
    }
    
    /**
     * Creates a new instance with the current default {@link Charset}
     * and the specified <tt>delimiter</tt>.
     * 
     * 构造方法
     * 
     * @param delimiter The line delimiter to use
     */
    public TextLineDecoder(String delimiter) {
        this(new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the current default {@link Charset}
     * and the specified <tt>delimiter</tt>.
     * 
     * 构造方法
     * 
     * @param delimiter The line delimiter to use
     */
    public TextLineDecoder(LineDelimiter delimiter) {
        this(Charset.defaultCharset(), delimiter);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and {@link LineDelimiter#AUTO} delimiter.
     * 
     * 构造方法
     * 
     * @param charset The {@link Charset} to use
     */
    public TextLineDecoder(Charset charset) {
        this(charset, LineDelimiter.AUTO);
    }

    /**
     * Creates a new instance with the spcified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     * 
     * 构造方法
     * 
     * @param charset The {@link Charset} to use
     * @param delimiter The line delimiter to use
     */
    public TextLineDecoder(Charset charset, String delimiter) {
        this(charset, new LineDelimiter(delimiter));
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt>
     * and the specified <tt>delimiter</tt>.
     * 
     * 构造方法
     * 
     * @param charset The {@link Charset} to use
     * @param delimiter The line delimiter to use
     */
    public TextLineDecoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new IllegalArgumentException("charset parameter shuld not be null");
        }
        if (delimiter == null) {
            throw new IllegalArgumentException("delimiter parameter should not be null");
        }
        this.charset = charset;
        this.delimiter = delimiter;
        // Convert delimiter to ByteBuffer if not done yet.
        if (delimBuf == null) {
            IoBuffer tmp = IoBuffer.allocate(2).setAutoExpand(true);
            try {
                tmp.putString(delimiter.getValue(), charset.newEncoder());
            } catch (CharacterCodingException cce) {

            }
            tmp.flip();
            delimBuf = tmp;
        }
    }
    
    /**
     * @return the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     * 
     * 返回此解码器能处理的一行的最大长度。默认：1024字节。
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }
    
    /**
     * Sets the allowed maximum size of the line to be decoded.
     * If the size of the line to be decoded exceeds this value, the
     * decoder will throw a {@link BufferDataException}.  The default
     * value is <tt>1024</tt> (1KB).
     * 
     * 设置此解码器能处理的一行的最大长度。默认：1024字节。
     * 
     * @param maxLineLength The maximum line length
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength (" + maxLineLength + ") should be a positive value");
        }
        this.maxLineLength = maxLineLength;
    }
    
    /**
     * Sets the default buffer size. This buffer is used in the Context
     * to store the decoded line.
     * 
     * 设置用来保存已解码行的buffer的最大大小。
     *
     * @param bufferLength The default bufer size
     */
    public void setBufferLength(int bufferLength) {
        if (bufferLength <= 0) {
            throw new IllegalArgumentException("bufferLength (" + maxLineLength + ") should be a positive value");
        }
        this.bufferLength = bufferLength;
    }

    /**
     * @return the allowed buffer size used to store the decoded line
     * in the Context instance.
     * 
     * 返回用来保存已解码行的buffer的最大大小。
     */
    public int getBufferLength() {
        return bufferLength;
    }
    
    /**
     * {@inheritDoc}
     */
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
    	Context context = getContext(session);
    	if (LineDelimiter.AUTO.equals(delimiter)) {
			decodeAuto(context, session, in, out);
		} else {
			decodeNormal(context, session, in, out);
		}
    }
    
    /**
     * {@inheritDoc}
     */
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    public void dispose(IoSession session) throws Exception {
        Context context = (Context) session.getAttribute(CONTEXT);
        if (context != null) {
            session.removeAttribute(CONTEXT);
        }
    }
    
    /**
     * @return the context for this session
     * 
     * 返回指定session的上下文。
     * 
     * @param session The session for which we want the context
     */
    private Context getContext(IoSession session) {
        Context context;
        context = (Context) session.getAttribute(CONTEXT);
        if (context == null) {
        	context = new Context(bufferLength);
            session.setAttribute(CONTEXT, context);
        }
        return context;
    }
    
    /**
     * Decode a line using the default delimiter on the current system
     * 
     * 使用当前系统的默认分隔符解码文本行(把"\n"或"\r\n"作为分隔符)。
     */
    private void decodeAuto(Context ctx, IoSession session, IoBuffer in, ProtocolDecoderOutput out)
    		throws CharacterCodingException, ProtocolDecoderException {

    	int matchCount = ctx.getMatchCount();
    	
    	// Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        
        while (in.hasRemaining()) {
        	byte b = in.get();
        	boolean matched = false;
        	switch (b) {
			case '\r':
				// Might be Mac, but we don't auto-detect Mac EOL
                // to avoid confusion.
				// 可能是Mac，但为了避免混乱，我们不处理。
				matchCount++;
				break;
			
			case '\n':
                // UNIX
                matchCount++;
                matched = true;
                break;

			default:
				matchCount = 0;
				break;
			}
        	
    		// Found a match.
        	// 发现一个匹配
        	if (matched) {
        		// 解析一个匹配字符串
        		int pos = in.position();
        		in.limit(pos);
        		in.position(oldPos);
        		ctx.append(in);
        		
        		in.limit(oldLimit);
        		in.position(pos);
        		
        		// 上下文中的临时buffer还没超长
        		if (ctx.getOverflowPosition() == 0) {
					IoBuffer buffer = ctx.getBuffer();
					buffer.flip();
					// 去掉分隔符
					buffer.limit(buffer.limit() - matchCount);
					
					try {
						// 将数据读取到数组中
						byte[] data = new byte[buffer.limit()];
						buffer.get(data);
						// 解码数据到CharBuffer中
						CharsetDecoder decoder = ctx.getDecoder();
						CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(data));
						String str = charBuffer.toString();
						writeText(session, str, out);
					} finally {
						// 清空临时buffer
						buffer.clear();
					}
				} 
        		// 已超长，重置上下文，并抛出异常。
        		else {
					int overflowPosition = ctx.getOverflowPosition();
					ctx.reset();
					throw new RecoverableProtocolDecoderException("Line is too long: " + overflowPosition);
				}
        		oldPos = pos;
        		matchCount = 0;
			}
        }
        // Put remainder to buffer.
        // 把in里剩余未解析的数据放入buffer中，以待下一次数据到来时处理。
        in.position(oldPos);
        ctx.append(in);
        ctx.setMatchCount(matchCount);
    }
    
    /**
     * Decode a line using the delimiter defined by the caller
     * 
     * 使用指定分隔符解码文本行。
     */
    private void decodeNormal(Context ctx, IoSession session, IoBuffer in, ProtocolDecoderOutput out)
            throws CharacterCodingException, ProtocolDecoderException {
    	
    	int matchCount = ctx.getMatchCount();

        // Try to find a match
        int oldPos = in.position();
        int oldLimit = in.limit();
        
        while (in.hasRemaining()) {
        	byte b = in.get();
        	if (delimBuf.get(matchCount) == b) {
				matchCount++;
				// Found a match.
				// 发现一个匹配
				if (matchCount == delimBuf.limit()) {
					// 解析一个匹配字符串
					int pos = in.position();
					in.limit(pos);
					in.position(oldPos);
					ctx.append(in);
					
					in.limit(oldLimit);
					in.position(pos);
					
					// 上下文中的临时buffer还没超长
					if (ctx.getOverflowPosition() == 0) {
						IoBuffer buf = ctx.getBuffer();
                        buf.flip();
                        // 去掉分隔符
                        buf.limit(buf.limit() - matchCount);
                        
                        try {
                        	// 解码数据
                            writeText(session, buf.getString(ctx.getDecoder()), out);
                        } finally {
                        	// 清空临时buffer
                            buf.clear();
                        }
					}
					// 已超长，重置上下文，并抛出异常。
					else {
                        int overflowPosition = ctx.getOverflowPosition();
                        ctx.reset();
                        throw new RecoverableProtocolDecoderException("Line is too long: " + overflowPosition);
                    }
					oldPos = pos;
                    matchCount = 0;
				}
			} else {
                // fix for DIRMINA-506 & DIRMINA-536
				// 多字符做分隔符时，如果前面几位匹配上，后面有1位匹配不上时，要把in的position设回已匹配的数，使匹配过的几位可以重新参与匹配。
				// 如现在的分隔符是aabbcc，要解析的字符数组是faaabdaabbcc：当第一个aa匹配上，读到第三个a时，postion=4，matchCount=2，此时发现没和分隔符匹配上，
				// 然后重置positon为4-2=2，matchCount=0，然后从第2个a重新开始匹配。
                in.position(Math.max(0, in.position() - matchCount));
                matchCount = 0;
            }
        }
        // Put remainder to buffer.
        // 把in里剩余未解析的数据放入buffer中，以待下一次数据到来时处理。
        in.position(oldPos);
        ctx.append(in);
    }
    
    /**
     * By default, this method propagates the decoded line of text to
     * {@code ProtocolDecoderOutput#write(Object)}.  You may override this method to modify
     * the default behavior.
     * 
     * 将已解码的字符串写入ProtocolDecoderOutput。
     *
     * @param session  the {@code IoSession} the received data.
     * @param text  the decoded text
     * @param out  the upstream {@code ProtocolDecoderOutput}.
     */
    protected void writeText(IoSession session, String text, ProtocolDecoderOutput out) {
        out.write(text);
    }
    
    /**
     * A Context used during the decoding of a line. It stores the decoder,
     * the temporary buffer containing the decoded line, and other status flags.
     * 
     * 解码文本行时使用的上下文，保存了解码器、保存已解码数据的临时buffer、其它状态标识。
     */
    private class Context {
    	
    	/** The decoder. 解码器 */
        private final CharsetDecoder decoder;
        
        /** The temporary buffer containing the decoded line. 保存已解码数据的临时buffer */
        private final IoBuffer buf;
        
        /** The number of lines found so far. 已发现的行数 */
        private int matchCount = 0;
        
        /** A counter to signal that the line is too long. 当解码出的数据超过最大值后，用来标记临时buffer的position应该在的位置 */
        private int overflowPosition = 0;
        
        /** 
         * Create a new Context object with a default buffer 
         * 
         * 构造函数：指定保存已解码数据的临时buffer的大小。
         */
        private Context(int bufferLength) {
            decoder = charset.newDecoder();
            buf = IoBuffer.allocate(bufferLength).setAutoExpand(true);
        }
        
        public CharsetDecoder getDecoder() {
            return decoder;
        }

        public IoBuffer getBuffer() {
            return buf;
        }

        public int getOverflowPosition() {
            return overflowPosition;
        }

        public int getMatchCount() {
            return matchCount;
        }

        public void setMatchCount(int matchCount) {
            this.matchCount = matchCount;
        }
        
        /**
         * 重置上下文
         */
        public void reset() {
            overflowPosition = 0;
            matchCount = 0;
            decoder.reset();
        }
        
        /**
         * 添加指定buffer中的内容到内部buffer中
         * @param in
         */
        public void append(IoBuffer in) {
        	// 1. 已超长，直接丢弃in的数据
        	if (overflowPosition != 0) {
				discard(in);
			} 
        	// 2. 如果添加in的数据后超出行的最大值，则标记overflowPosition，并清空临时buffer，丢弃in的数据
        	else if (buf.position() > maxLineLength - in.remaining()) {
        		overflowPosition = buf.position();
        		buf.clear();
        		discard(in);
        	} 
        	// 3. 将in的数据添加到临时buffer中。
        	else {
        		getBuffer().put(in);
        	}
        }
        
        /**
         * 丢弃指定buffer，并标记超出的position的位置。
         * @param in
         */
        private void discard(IoBuffer in) {
        	if (Integer.MAX_VALUE - in.remaining() < overflowPosition) {
				overflowPosition = Integer.MAX_VALUE;
			} else {
				overflowPosition += in.remaining();
			}
        	in.position(in.limit());
        }
    }
}
