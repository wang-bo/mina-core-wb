package org.apache.mina.core.session;

import org.apache.mina.core.write.WriteRequest;

/**
 * An I/O event or an I/O request that MINA provides.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 * 
 * 代表一个I/O事件或一个I/O请求：执行时会触发对应session的过滤器链上的事件。
 * 一般只在Mina内部组件需要保存I/O事件时使用。
 * 
 * @date	2016年6月27日 下午3:49:59	completed
 */
public class IoEvent implements Runnable {
	
	private final IoEventType type;
	
	private final IoSession session;
	
	private final Object parameter;
	
	public IoEvent(IoEventType type, IoSession session, Object parameter) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (session == null) {
            throw new IllegalArgumentException("session");
        }
        this.type = type;
        this.session = session;
        this.parameter = parameter;
    }
	
	public IoEventType getType() {
        return type;
    }

    public IoSession getSession() {
        return session;
    }

    public Object getParameter() {
        return parameter;
    }

	public void run() {
		fire();
	}

	public void fire() {
		switch (getType()) {
		case MESSAGE_RECEIVED:
			getSession().getFilterChain().fireMessageReceived(getParameter());
			break;

		case MESSAGE_SENT:
            getSession().getFilterChain().fireMessageSent((WriteRequest) getParameter());
            break;
            
        case WRITE:
            getSession().getFilterChain().fireFilterWrite((WriteRequest) getParameter());
            break;
            
        case CLOSE:
            getSession().getFilterChain().fireFilterClose();
            break;
            
        case EXCEPTION_CAUGHT:
            getSession().getFilterChain().fireExceptionCaught((Throwable) getParameter());
            break;
            
        case SESSION_IDLE:
            getSession().getFilterChain().fireSessionIdle((IdleStatus) getParameter());
            break;
            
        case SESSION_OPENED:
            getSession().getFilterChain().fireSessionOpened();
            break;
            
        case SESSION_CREATED:
            getSession().getFilterChain().fireSessionCreated();
            break;
            
        case SESSION_CLOSED:
            getSession().getFilterChain().fireSessionClosed();
            break;
            
        default:
            throw new IllegalArgumentException("Unknown event type: " + getType());
		}
	}
	
	@Override
    public String toString() {
        if (getParameter() == null) {
            return "[" + getSession() + "] " + getType().name();
        }
        return "[" + getSession() + "] " + getType().name() + ": " + getParameter();
    }
}
