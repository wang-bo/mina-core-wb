package org.apache.mina.core.filterchain;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoEvent;
import org.apache.mina.core.session.IoEventType;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An I/O event or an I/O request that MINA provides for {@link IoFilter}s.
 * Most users won't need to use this class.  It is usually used by internal
 * components to store I/O events.
 * 
 * 代表一个I/O事件或一个I/O请求：比IoEvent多了个NextFilter，执行时会触发这个NextFilter对应的事件。
 * 一般只在Mina内部组件需要保存I/O事件时使用。
 * 
 * @date	2016年6月27日 下午4:02:33	completed
 */
public class IoFilterEvent extends IoEvent {

	/** A logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(IoFilterEvent.class);

    /** A speedup for logs */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();
    
    private final NextFilter nextFilter;

    public IoFilterEvent(NextFilter nextFilter, IoEventType type, IoSession session, Object parameter) {
        super(type, session, parameter);
        if (nextFilter == null) {
            throw new IllegalArgumentException("nextFilter must not be null");
        }
        this.nextFilter = nextFilter;
    }
    
    public NextFilter getNextFilter() {
        return nextFilter;
    }
    
    @Override
    public void fire() {
    	IoSession session = getSession();
    	NextFilter nextFilter = getNextFilter();
    	IoEventType type = getType();
    	
    	if (DEBUG) {
            LOGGER.debug("Firing a {} event for session {}", type, session.getId());
        }
    	
    	switch (type) {
        case MESSAGE_RECEIVED:
            Object parameter = getParameter();
            nextFilter.messageReceived(session, parameter);
            break;

        case MESSAGE_SENT:
            WriteRequest writeRequest = (WriteRequest) getParameter();
            nextFilter.messageSent(session, writeRequest);
            break;

        case WRITE:
            writeRequest = (WriteRequest) getParameter();
            nextFilter.filterWrite(session, writeRequest);
            break;

        case CLOSE:
            nextFilter.filterClose(session);
            break;

        case EXCEPTION_CAUGHT:
            Throwable throwable = (Throwable) getParameter();
            nextFilter.exceptionCaught(session, throwable);
            break;

        case SESSION_IDLE:
            nextFilter.sessionIdle(session, (IdleStatus) getParameter());
            break;

        case SESSION_OPENED:
            nextFilter.sessionOpened(session);
            break;

        case SESSION_CREATED:
            nextFilter.sessionCreated(session);
            break;

        case SESSION_CLOSED:
            nextFilter.sessionClosed(session);
            break;

        default:
            throw new IllegalArgumentException("Unknown event type: " + type);
        }

        if (DEBUG) {
            LOGGER.debug("Event {} has been fired for session {}", type, session.getId());
        }
    }
}
