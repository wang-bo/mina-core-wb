package org.apache.mina.filter.codec.textline;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

/**
 * A delimiter which is appended to the end of a text line, such as
 * <tt>CR/LF</tt>. This class defines default delimiters for various
 * OS :
 * <ul>
 * <li><b>Unix/Linux</b> : LineDelimiter.UNIX ("\n")</li>
 * <li><b>Windows</b> : LineDelimiter.WINDOWS ("\r\n")</li>
 * <li><b>MAC</b> : LineDelimiter.MAC ("\r")</li>
 * </ul>
 * 
 * 文本行的分隔符，如CR/LF。
 * 本类定义了各种操作系统的默认分隔符：
 * 	a. Unix/Linux ：LineDelimiter.UNIX ("\n")
 * 	b. Windows ：LineDelimiter.WINDOWS ("\r\n")
 * 	c. MAC : LineDelimiter.MAC ("\r")
 * 
 * @date	2016年6月29日 上午11:44:01	completed
 */
public class LineDelimiter {

	/** the line delimiter constant of the current O/S. 
	 *  当前操作系统的默认文本行分隔符 */
	public static final LineDelimiter DEFAULT;
	
	/** Compute the default delimiter on he current OS 
	 *  计算当前操作系统的默认文本行分隔符 */
	static {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintWriter out = new PrintWriter(bout, true);
		out.println();
		DEFAULT = new LineDelimiter(new String(bout.toByteArray()));
	}
	
	/**
     * A special line delimiter which is used for auto-detection of
     * EOL in {@link TextLineDecoder}.  If this delimiter is used,
     * {@link TextLineDecoder} will consider both  <tt>'\r'</tt> and
     * <tt>'\n'</tt> as a delimiter.
     * 
     * 一个特殊的文本行分隔符，使用这个分隔符时，解码器会自动侦测真正的文本行分隔符，一般会把'\r'和'\n'都认为是分割符。
     */
    public static final LineDelimiter AUTO = new LineDelimiter("");

    /**
     * The CRLF line delimiter constant (<tt>"\r\n"</tt>)
     * 
     * 使用CRLF做分隔符(即:"\r\n")
     */
    public static final LineDelimiter CRLF = new LineDelimiter("\r\n");

    /**
     * The line delimiter constant of UNIX (<tt>"\n"</tt>)
     * 
     * 使用UNIX系统的默认分隔符(即:"\n")
     */
    public static final LineDelimiter UNIX = new LineDelimiter("\n");

    /**
     * The line delimiter constant of MS Windows/DOS (<tt>"\r\n"</tt>)
     * 
     * 使用Windows、DOS系统的默认分隔符(即:"\r\n")
     */
    public static final LineDelimiter WINDOWS = CRLF;

    /**
     * The line delimiter constant of Mac OS (<tt>"\r"</tt>)
     * 
     * 使用Mac系统的默认分隔符(即:"\r")
     */
    public static final LineDelimiter MAC = new LineDelimiter("\r");

    /**
     * The line delimiter constant for NUL-terminated text protocols
     * such as Flash XML socket (<tt>"\0"</tt>)
     * 
     * 没有分隔符的文本协议(如Flash XML socket)使用的分隔符(即:"\0")
     */
    public static final LineDelimiter NUL = new LineDelimiter("\0");
    
    /** Stores the selected Line delimiter 
     *  分隔符的内容*/
    private final String value;
    
    /**
     * Creates a new line delimiter with the specified <tt>value</tt>.
     * 
     * 构造方法：使用指定的字符串做分隔符。
     * 
     * @param value The new Line Delimiter
     */
    public LineDelimiter(String value) {
        if (value == null) {
            throw new IllegalArgumentException("delimiter");
        }
        this.value = value;
    }
    
    /**
     * @return the delimiter string.
     * 
     * 返回做分隔符使用的字符串。
     */
    public String getValue() {
        return value;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LineDelimiter)) {
            return false;
        }
        LineDelimiter that = (LineDelimiter) o;
        return this.value.equals(that.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (value.length() == 0) {
            return "delimiter: auto";
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("delimiter:");

            for (int i = 0; i < value.length(); i++) {
                buf.append(" 0x");
                buf.append(Integer.toHexString(value.charAt(i)));
            }
            return buf.toString();
        }
    }
}
