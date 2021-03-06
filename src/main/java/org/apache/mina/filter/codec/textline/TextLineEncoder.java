package org.apache.mina.filter.codec.textline;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * A {@link ProtocolEncoder} which encodes a string into a text line
 * which ends with the delimiter.
 * 
 * 将字符串编码成文本行的编码器
 * 
 * @date	2016年6月29日 上午11:57:56	completed
 */
public class TextLineEncoder extends ProtocolEncoderAdapter {

	private static final AttributeKey ENCODER = new AttributeKey(TextLineEncoder.class, "encoder");
	
	private final Charset charset;

    private final LineDelimiter delimiter;

    private int maxLineLength = Integer.MAX_VALUE;
    
    /**
     * Creates a new instance with the current default {@link Charset}
     * and {@link LineDelimiter#UNIX} delimiter.
     * 
     * 构造方法：使用默认Charset和"\n"做分隔符。
     */
    public TextLineEncoder() {
        this(Charset.defaultCharset(), LineDelimiter.UNIX);
    }
    
    /**
     * Creates a new instance with the current default {@link Charset}
     * and the specified <tt>delimiter</tt>.
     * 
     * 构造方法
     * 
     * @param delimiter The line delimiter to use
     */
    public TextLineEncoder(String delimiter) {
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
    public TextLineEncoder(LineDelimiter delimiter) {
        this(Charset.defaultCharset(), delimiter);
    }

    /**
     * Creates a new instance with the specified <tt>charset</tt>
     * and {@link LineDelimiter#UNIX} delimiter.
     * 
     * 构造方法
     * 
     * @param charset The {@link Charset} to use
     */
    public TextLineEncoder(Charset charset) {
        this(charset, LineDelimiter.UNIX);
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
    public TextLineEncoder(Charset charset, String delimiter) {
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
    public TextLineEncoder(Charset charset, LineDelimiter delimiter) {
        if (charset == null) {
            throw new IllegalArgumentException("charset");
        }
        if (delimiter == null) {
            throw new IllegalArgumentException("delimiter");
        }
        if (LineDelimiter.AUTO.equals(delimiter)) {
            throw new IllegalArgumentException("AUTO delimiter is not allowed for encoder.");
        }
        this.charset = charset;
        this.delimiter = delimiter;
    }
    
    /**
     * @return the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * 
     * 返回此编码器能处理的一行的最大长度。默认：Integer.MAX_VALUE
     */
    public int getMaxLineLength() {
        return maxLineLength;
    }
    
    /**
     * Sets the allowed maximum size of the encoded line.
     * If the size of the encoded line exceeds this value, the encoder
     * will throw a {@link IllegalArgumentException}.  The default value
     * is {@link Integer#MAX_VALUE}.
     * 
     * 设置此编码器能处理的一行的最大长度。默认：Integer.MAX_VALUE
     * 
     * @param maxLineLength The maximum line length
     */
    public void setMaxLineLength(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength: " + maxLineLength);
        }
        this.maxLineLength = maxLineLength;
    }
    
    /**
     * {@inheritDoc}
     */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
    	CharsetEncoder encoder = (CharsetEncoder) session.getAttribute(ENCODER);
    	if (encoder == null) {
			encoder = charset.newEncoder();
			session.setAttribute(ENCODER, encoder);
		}
    	String value = (message == null ? "" : message.toString());
    	IoBuffer buffer = IoBuffer.allocate(value.length()).setAutoExpand(true);
    	buffer.putString(value, encoder);
    	if (buffer.position() > maxLineLength) {
    		throw new IllegalArgumentException("Line length: " + buffer.position());
		}
    	buffer.putString(delimiter.getValue(), encoder);
    	buffer.flip();
    	out.write(buffer);
    }
    
    /**
     * {@inheritDoc}
     */
    public void dispose() throws Exception {
        // Do nothing
    }
}
