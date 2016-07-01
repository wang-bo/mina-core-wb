package org.apache.mina.core.session;

/**
 * An {@link Enum} that represents the type of I/O events and requests.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 * 
 * 代表I/O事件的枚举类。
 * 一般只在Mina内部组件需要保存I/O事件时使用。
 * 
 * @date	2016年6月27日 下午3:15:39	completed
 */
public enum IoEventType {
	
	SESSION_CREATED, 
	
	SESSION_OPENED, 
	
	SESSION_CLOSED, 
	
	MESSAGE_RECEIVED, 
	
	MESSAGE_SENT, 
	
	SESSION_IDLE, 
	
	EXCEPTION_CAUGHT, 
	
	WRITE, 
	
	CLOSE,
}
