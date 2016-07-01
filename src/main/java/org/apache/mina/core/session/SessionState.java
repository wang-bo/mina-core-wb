package org.apache.mina.core.session;

/**
 * The session state. A session can be in three different state :
 * <ul>
 *   <li>OPENING : The session has not been fully created</li>
 *   <li>OPENED : The session is opened</li>
 *   <li>CLOSING :  The session is closing</li>
 * </ul>
 * 
 * session的状态，3种：
 * 	a. OPENING：session还在创建过程中。
 * 	b. OPENED：session创建完毕，已打开。
 * 	c. CLOSING：session在关闭过程中。
 * 
 * @date	2016年6月21日 上午9:14:24
 */
public enum SessionState {

	/** The session has not been fully created. 创建过程中 */
	OPENING, 
	
	/** The session is opened. 创建完毕 */
	OPENED, 
	
	/** The session is closing. 关闭过程中 */
	CLOSING
}
