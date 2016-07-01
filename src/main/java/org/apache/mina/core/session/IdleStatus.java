package org.apache.mina.core.session;

/**
 * Represents the type of idleness of {@link IoSession} or
 * {@link IoSession}.  There are three types of idleness:
 * <ul>
 *   <li>{@link #READER_IDLE} - No data is coming from the remote peer.</li>
 *   <li>{@link #WRITER_IDLE} - Session is not writing any data.</li>
 *   <li>{@link #BOTH_IDLE} - Both {@link #READER_IDLE} and {@link #WRITER_IDLE}.</li>
 * </ul>
 * <p>
 * Idle time settings are all disabled by default.  You can enable them
 * using {@link IoSessionConfig#setIdleTime(IdleStatus,int)}.
 * 
 * 代表IoSession或IoService的空闲状态，共有3种状态：
 *  1. READER_IDLE - 远端节点没有发送数据
 *  2. WRITER_IDLE - 当前session没有写数据
 *  3. BOTH_IDLE - READER_IDLE & WRITER_IDLE同时发生
 * 默认是没有空闲时间设定的，如果要用的话，使用IoSessionConfig.setIdleTime(IdleStatus, int)方法来设置。
 * 
 * @date	2016年5月30日 上午10:03:01	completed
 */
public class IdleStatus {

	/**
	 * Represents the session status that no data is coming from the remote peer.
	 * 
	 * 远端节点没有发送数据
	 */
	public static final IdleStatus READER_IDLE = new IdleStatus("reader idle");
	
	/**
	 * Represents the session status that the session is not writing any data.
	 * 
	 * 当前session没有写数据
	 */
	public static final IdleStatus WRITER_IDLE = new IdleStatus("writer idle");
	
	/**
	 * Represents both {@link #READER_IDLE} and {@link #WRITER_IDLE}.
	 * 
	 * READER_IDLE & WRITER_IDLE同时发生
	 */
	public static final IdleStatus BOTH_IDLE = new IdleStatus("both idle");
	
	private final String value;
	
	/**
	 * Creates a new instance.
	 */
	private IdleStatus(String value) {
		this.value = value;
	}

	/**
	 * @return the string representation of this status.
	 * <ul>
     *   <li>{@link #READER_IDLE} - <tt>"reader idle"</tt></li>
     *   <li>{@link #WRITER_IDLE} - <tt>"writer idle"</tt></li>
     *   <li>{@link #BOTH_IDLE} - <tt>"both idle"</tt></li>
     * </ul>
     * 
     * 返回当前状态的文字描述
	 */
	@Override
	public String toString() {
		return value;
	}
}
