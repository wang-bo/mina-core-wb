package org.apache.mina.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

/**
 * A utility class that provides various convenience methods related with
 * {@link IoSession} and {@link IoFuture}.
 * 
 * 提供多种便利方法操作IoSession和IoFuture的工具类
 * 	a. 给指定的一组session发送message
 * 	b. 给指定的一组future调用await()、awaitUninterruptibly()
 * 
 * @date	2016年6月15日 下午1:51:01	completed
 */
public final class IoUtil {

	/** 空的IoSession数组 */
	private static final IoSession[] EMPTY_SESSIONS = new IoSession[0];
	
	/**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * 把指定的message写到指定的所有session中。
     * 如果message是一个IoBuffer，会自动使用IoBuffer.duplicate()复制一份。
     * 
     * @param message The message to broadcast
     * @param sessions The sessions that will receive the message
     * @return The list of WriteFuture created for each broadcasted message
     */
    public static List<WriteFuture> broadcast(Object message, Collection<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>(sessions.size());
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * 把指定的message写到指定的所有session中。
     * 如果message是一个IoBuffer，会自动使用IoBuffer.duplicate()复制一份。
     * 
     * @param message The message to broadcast
     * @param sessions The sessions that will receive the message
     * @return The list of WriteFuture created for each broadcasted message
     */
    public static List<WriteFuture> broadcast(Object message, Iterable<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>();
        broadcast(message, sessions.iterator(), answer);
        return answer;
    }

    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * 把指定的message写到指定的所有session中。
     * 如果message是一个IoBuffer，会自动使用IoBuffer.duplicate()复制一份。
     * 
     * @param message The message to write
     * @param sessions The sessions the message has to be written to
     * @return The list of {@link WriteFuture} for the written messages
     */
    public static List<WriteFuture> broadcast(Object message, Iterator<IoSession> sessions) {
        List<WriteFuture> answer = new ArrayList<WriteFuture>();
        broadcast(message, sessions, answer);
        return answer;
    }
    
    /**
     * Writes the specified {@code message} to the specified {@code sessions}.
     * If the specified {@code message} is an {@link IoBuffer}, the buffer is
     * automatically duplicated using {@link IoBuffer#duplicate()}.
     * 
     * 把指定的message写到指定的所有session中。
     * 如果message是一个IoBuffer，会自动使用IoBuffer.duplicate()复制一份。
     * 
     * @param message The message to write
     * @param sessions The sessions the message has to be written to
     * @return The list of {@link WriteFuture} for the written messages
     */
    public static List<WriteFuture> broadcast(Object message, IoSession... sessions) {
        if (sessions == null) {
            sessions = EMPTY_SESSIONS;
        }
        List<WriteFuture> answer = new ArrayList<WriteFuture>(sessions.length);
        if (message instanceof IoBuffer) {
            for (IoSession session : sessions) {
                answer.add(session.write(((IoBuffer) message).duplicate()));
            }
        } else {
            for (IoSession session : sessions) {
                answer.add(session.write(message));
            }
        }
        return answer;
    }
    
    private static void broadcast(Object message, Iterator<IoSession> sessions, Collection<WriteFuture> answer) {
        if (message instanceof IoBuffer) {
            while (sessions.hasNext()) {
                IoSession session = sessions.next();
                answer.add(session.write(((IoBuffer) message).duplicate()));
            }
        } else {
            while (sessions.hasNext()) {
                IoSession session = sessions.next();
                answer.add(session.write(message));
            }
        }
    }
    
    /**
     * 调用所有future的await()方法
     * @param futures
     * @throws InterruptedException
     */
    public static void await(Iterable<? extends IoFuture> futures) throws InterruptedException {
        for (IoFuture future : futures) {
        	future.await();
        }
    }
    
    /**
     * 调用所有future的awaitUninterruptibly()方法
     * @param futures
     */
    public static void awaitUninterruptably(Iterable<? extends IoFuture> futures) {
        for (IoFuture future : futures) {
        	future.awaitUninterruptibly();
        }
    }
    
    /**
     * 在指定时间内调用所有future的await(timeout)方法，全部成功调用返回true，否则返回false
     * @param futures
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public static boolean await(Iterable<? extends IoFuture> futures, long timeout, TimeUnit unit)
            throws InterruptedException {
        return await(futures, unit.toMillis(timeout));
    }
    
    /**
     * 在指定时间内调用所有future的await(timeout)方法，全部成功调用返回true，否则返回false
     * @param futures
     * @param timeoutMillis
     * @return
     * @throws InterruptedException
     */
    public static boolean await(Iterable<? extends IoFuture> futures, long timeoutMillis) throws InterruptedException {
        return await0(futures, timeoutMillis, true);
    }
    
    /**
     * 在指定时间内调用所有future的awaitUninterruptibly(timeout)方法，全部成功调用返回true，否则返回false
     * @param futures
     * @param timeout
     * @param unit
     * @return
     */
    public static boolean awaitUninterruptibly(Iterable<? extends IoFuture> futures, long timeout, TimeUnit unit) {
        return awaitUninterruptibly(futures, unit.toMillis(timeout));
    }

    /**
     * 在指定时间内调用所有future的awaitUninterruptibly(timeout)方法，全部成功调用返回true，否则返回false
     * @param futures
     * @param timeoutMillis
     * @return
     */
    public static boolean awaitUninterruptibly(Iterable<? extends IoFuture> futures, long timeoutMillis) {
        try {
            return await0(futures, timeoutMillis, false);
        } catch (InterruptedException e) {
            throw new InternalError();
        }
    }
    
    private static boolean await0(Iterable<? extends IoFuture> futures, long timeoutMillis, boolean interruptable)
            throws InterruptedException {
        long startTime = timeoutMillis <= 0 ? 0 : System.currentTimeMillis();
        long waitTime = timeoutMillis;

        boolean lastComplete = true;
        Iterator<? extends IoFuture> iterator = futures.iterator();
        while (iterator.hasNext()) {
            IoFuture future = iterator.next();
            do {
                if (interruptable) {
                    lastComplete = future.await(waitTime);
                } else {
                    lastComplete = future.awaitUninterruptibly(waitTime);
                }

                waitTime = timeoutMillis - (System.currentTimeMillis() - startTime);

                if (lastComplete || waitTime <= 0) {
                    break;
                }
            } while (!lastComplete);

            if (waitTime <= 0) {
                break;
            }
        }

        return lastComplete && !iterator.hasNext();
    }
}
