package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

public class TestMina {

	public static void main(String[] args) {
		ExecutorFilter executorfilter = new ExecutorFilter(1, 2);
		SocketAcceptor acceptor = new NioSocketAcceptor();
		acceptor.getFilterChain().addLast("executor", executorfilter);
		acceptor.getFilterChain().addLast("test", new TestFilter());
		
		acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 60);
		acceptor.setReuseAddress(true);
		acceptor.setBacklog(512);
		
		acceptor.setHandler(new TestHandler());
		
		try {
			acceptor.bind(new InetSocketAddress(6222)); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("System listening in port : " + 6222);
		System.out.println(acceptor.getLocalAddress());
		System.out.println(acceptor.getLocalAddress().getHostName());
		System.out.println(acceptor.getLocalAddress().getHostString());
		System.out.println(acceptor.getLocalAddress().getAddress());
		System.out.println(acceptor.getLocalAddress().getAddress().getHostName());
		System.out.println(acceptor.getLocalAddress().getAddress().getHostAddress());
		
		System.out.println(acceptor.getDefaultLocalAddress());
		System.out.println("2: " + acceptor.getLocalAddresses());
		System.out.println(acceptor.getDefaultLocalAddresses());
		
		System.out.println();
		
		Set<SocketAddress> set = acceptor.getLocalAddresses();
		for (SocketAddress socketAddress : set) {
			InetSocketAddress address = (InetSocketAddress) socketAddress;
			System.out.println(address.getHostName());
		}
	}
}

class TestFilter extends IoFilterAdapter {
	public void init() throws Exception {
    }

    public void destroy() throws Exception {
    }

    public void onPreAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    public void onPostAdd(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    public void onPreRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    public void onPostRemove(IoFilterChain parent, String name, NextFilter nextFilter) throws Exception {
    }

    public void sessionCreated(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionCreated(session);
    }

    public void sessionOpened(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionOpened(session);
    }

    public void sessionClosed(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.sessionClosed(session);
    }

    public void sessionIdle(NextFilter nextFilter, IoSession session, IdleStatus status) throws Exception {
        nextFilter.sessionIdle(session, status);
    }

    public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
        nextFilter.exceptionCaught(session, cause);
    }

    public void messageReceived(NextFilter nextFilter, IoSession session, Object message) throws Exception {
    	System.out.println("receive message from " + session.getRemoteAddress() 
    			+ ", message: " + message);
    	System.out.println("message class: " + message.getClass());
    	System.out.println("message instanceof IoBuffer：" + (message instanceof IoBuffer));
    	System.out.println("message instanceof ByteBuffer：" + (message instanceof ByteBuffer));
    	session.write("message recived");
        nextFilter.messageReceived(session, message);
    }

    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        nextFilter.messageSent(session, writeRequest);
    }

    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        nextFilter.filterWrite(session, writeRequest);
    }

    public void filterClose(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.filterClose(session);
    }

    public void inputClosed(NextFilter nextFilter, IoSession session) throws Exception {
        nextFilter.inputClosed(session);
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }
}

class TestHandler extends IoHandlerAdapter {
	
	public void sessionCreated(IoSession session) throws Exception {
        // Empty handler
    }

    public void sessionOpened(IoSession session) throws Exception {
        // Empty handler
    }

    public void sessionClosed(IoSession session) throws Exception {
        // Empty handler
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        // Empty handler
    }

    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        
    }

    public void messageReceived(IoSession session, Object message) throws Exception {
    	System.out.println("receive message from " + session.getRemoteAddress() 
    			+ ", message: " + message);
    	System.out.println("message class: " + message.getClass());
    	System.out.println("message instanceof IoBuffer：" + (message instanceof IoBuffer));
    	System.out.println("message instanceof ByteBuffer：" + (message instanceof ByteBuffer));
    	session.write("message recived");
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        // Empty handler
    }

    public void inputClosed(IoSession session) throws Exception {
        session.closeOnFlush();
    }
}
