package org.apache.mina.core.service;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * A base implementation of {@link IoAcceptor}.
 * 
 * IoAcceptor的基础实现
 * 
 * @date	2016年6月17日 下午2:03:24	completed
 */
public abstract class AbstractIoAcceptor extends AbstractIoService implements IoAcceptor {

	/** bind()方法不带参数时，默认绑定的本地Socket地址 */
	private final List<SocketAddress> defaultLocalAddresses = new ArrayList<SocketAddress>();
	
	/** bind()方法不带参数时，默认绑定的本地Socket地址：该列表不可变，包装了defaultLocalAddresses，主要供外部查看 */
	private final List<SocketAddress> unmodifiableDefaultLocalAddresses = 
			Collections.unmodifiableList(defaultLocalAddresses);
	
	/** 已绑定的本地Socket地址集合 */
	private final Set<SocketAddress> boundAddresses = new HashSet<SocketAddress>();
	
	/** unbind()方法掉用后，当前acceptor上已连接的session是否关闭，默认true表示要关闭 */
	private boolean disconnectOnUnbind = true;
	
	/**
     * The lock object which is acquired while bind or unbind operation is performed.
     * Acquire this lock in your property setters which shouldn't be changed while
     * the service is bound.
     * 
     * 当执行bind()、unbind()方法时需要先获取这个锁，保证安全。
     */
    protected final Object bindLock = new Object();
    
    /**
     * Constructor for {@link AbstractIoAcceptor}. You need to provide a default
     * session configuration and an {@link Executor} for handling I/O events. If
     * null {@link Executor} is provided, a default one will be created using
     * {@link Executors#newCachedThreadPool()}.
     * 
     * 构造方法：需要提供一个默认的session配置类，一个处理I/O事件的Executor实例。
     * 如果提供的Executor实例为null，默认使用Executors.newCachedThreadPool()方法来创建一个。
     * 
     * @see AbstractIoService#AbstractIoService(IoSessionConfig, Executor)
     * 
     * @param sessionConfig
     *            the default configuration for the managed {@link IoSession}
     * @param executor
     *            the {@link Executor} used for handling execution of I/O
     *            events. Can be <code>null</code>.
     */
    protected AbstractIoAcceptor(IoSessionConfig sessionConfig, Executor executor) {
        super(sessionConfig, executor);
        defaultLocalAddresses.add(null);
    }
    
    /**
     * {@inheritDoc}
     */
    public SocketAddress getLocalAddress() {
        Set<SocketAddress> localAddresses = getLocalAddresses();
        if (localAddresses.isEmpty()) {
            return null;
        }
        return localAddresses.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public final Set<SocketAddress> getLocalAddresses() {
        Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
        synchronized (boundAddresses) {
        	// 这里重新包装一份再返回给调用方，这样调用方拿到数据并修改也不会有影响。
            localAddresses.addAll(boundAddresses);
        }
        return localAddresses;
    }
    
    /**
     * {@inheritDoc}
     */
    public SocketAddress getDefaultLocalAddress() {
        if (defaultLocalAddresses.isEmpty()) {
            return null;
        }
        return defaultLocalAddresses.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public final void setDefaultLocalAddress(SocketAddress localAddress) {
        setDefaultLocalAddresses(localAddress);
    }

    /**
     * {@inheritDoc}
     */
    public final List<SocketAddress> getDefaultLocalAddresses() {
        return unmodifiableDefaultLocalAddresses;
    }

    /**
     * {@inheritDoc}
     * @org.apache.xbean.Property nestedType="java.net.SocketAddress"
     */
    public final void setDefaultLocalAddresses(List<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        setDefaultLocalAddresses((Iterable<? extends SocketAddress>) localAddresses);
    }
    
    /**
     * {@inheritDoc}
     * 
     * 所有的setDefaultLocalAddress()、setDefaultLocalAddresses()方法最后都调用这个方法。
     */
    public final void setDefaultLocalAddresses(Iterable<? extends SocketAddress> localAddresses) {
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        synchronized (bindLock) {
            synchronized (boundAddresses) {
                if (!boundAddresses.isEmpty()) {
                    throw new IllegalStateException("localAddress can't be set while the acceptor is bound.");
                }
                Collection<SocketAddress> newLocalAddresses = new ArrayList<SocketAddress>();
                for (SocketAddress a : localAddresses) {
                    checkAddressType(a);
                    newLocalAddresses.add(a);
                }
                if (newLocalAddresses.isEmpty()) {
                    throw new IllegalArgumentException("empty localAddresses");
                }
                this.defaultLocalAddresses.clear();
                this.defaultLocalAddresses.addAll(newLocalAddresses);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @org.apache.xbean.Property nestedType="java.net.SocketAddress"
     */
    public final void setDefaultLocalAddresses(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses) {
        if (otherLocalAddresses == null) {
            otherLocalAddresses = new SocketAddress[0];
        }
        Collection<SocketAddress> newLocalAddresses = new ArrayList<SocketAddress>(otherLocalAddresses.length + 1);
        newLocalAddresses.add(firstLocalAddress);
        for (SocketAddress a : otherLocalAddresses) {
            newLocalAddresses.add(a);
        }
        setDefaultLocalAddresses(newLocalAddresses);
    }
    
    /**
     * {@inheritDoc}
     */
    public final boolean isCloseOnDeactivation() {
        return disconnectOnUnbind;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void setCloseOnDeactivation(boolean disconnectClientsOnUnbind) {
        this.disconnectOnUnbind = disconnectClientsOnUnbind;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void bind() throws IOException {
        bind(getDefaultLocalAddresses());
    }
    
    /**
     * {@inheritDoc}
     */
    public final void bind(SocketAddress localAddress) throws IOException {
        if (localAddress == null) {
            throw new IllegalArgumentException("localAddress");
        }
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        bind(localAddresses);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void bind(SocketAddress... addresses) throws IOException {
        if ((addresses == null) || (addresses.length == 0)) {
            bind(getDefaultLocalAddresses());
            return;
        }
        // 这里为什么不使用：Arrays.asList(addresses) ？
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(2);
        for (SocketAddress address : addresses) {
            localAddresses.add(address);
        }
        bind(localAddresses);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void bind(SocketAddress firstLocalAddress, SocketAddress... addresses) throws IOException {
        if (firstLocalAddress == null) {
            bind(getDefaultLocalAddresses());
        }
        if ((addresses == null) || (addresses.length == 0)) {
            bind(getDefaultLocalAddresses());
            return;
        }
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(2);
        localAddresses.add(firstLocalAddress);
        for (SocketAddress address : addresses) {
            localAddresses.add(address);
        }
        bind(localAddresses);
    }

    /**
     * {@inheritDoc}
     * 
     * 所有的bind()方法最后都调用这个方法。
     */
    public final void bind(Iterable<? extends SocketAddress> localAddresses) throws IOException {
    	// 1. 判断当前service是否正在清理
        if (isDisposing()) {
            throw new IllegalStateException("The Accpetor disposed is being disposed.");
        }
        
        // 2. 判断要绑定的地址列表是否为null
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }
        
        // 3. 判断要绑定的地址列表中，每个Socket地址类型是否正确
        List<SocketAddress> localAddressesCopy = new ArrayList<SocketAddress>();
        for (SocketAddress a : localAddresses) {
            checkAddressType(a);
            localAddressesCopy.add(a);
        }
        
        // 4. 判断要绑定的地址列表是否为空
        if (localAddressesCopy.isEmpty()) {
            throw new IllegalArgumentException("localAddresses is empty.");
        }

        // 5. 执行绑定
        boolean activate = false;
        synchronized (bindLock) {
            synchronized (boundAddresses) {
                if (boundAddresses.isEmpty()) {
                    activate = true;
                }
            }
            if (getHandler() == null) {
                throw new IllegalStateException("handler is not set.");
            }
            try {
                Set<SocketAddress> addresses = bindInternal(localAddressesCopy);
                synchronized (boundAddresses) {
                    boundAddresses.addAll(addresses);
                }
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeIoException("Failed to bind to: " + getLocalAddresses(), e);
            }
        }

        // 6. 当绑定前的已绑定本地Socket地址集合为空时，绑定成功后通知所有IoServiceListener。
        // 为什么要这样做？因为绑定前的已绑定本地Socket地址集合不为空，表示做本次bind()操作前service已经是活跃状态，本次bind()操作没有改变service的状态。
        if (activate) {
            getListeners().fireServiceActivated();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public final void unbind() {
        unbind(getLocalAddresses());
    }

    /**
     * {@inheritDoc}
     */
    public final void unbind(SocketAddress localAddress) {
        if (localAddress == null) {
            throw new IllegalArgumentException("localAddress");
        }
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>(1);
        localAddresses.add(localAddress);
        unbind(localAddresses);
    }
    
    /**
     * {@inheritDoc}
     */
    public final void unbind(SocketAddress firstLocalAddress, SocketAddress... otherLocalAddresses) {
        if (firstLocalAddress == null) {
            throw new IllegalArgumentException("firstLocalAddress");
        }
        if (otherLocalAddresses == null) {
            throw new IllegalArgumentException("otherLocalAddresses");
        }
        List<SocketAddress> localAddresses = new ArrayList<SocketAddress>();
        localAddresses.add(firstLocalAddress);
        Collections.addAll(localAddresses, otherLocalAddresses);
        unbind(localAddresses);
    }
    
    /**
     * {@inheritDoc}
     * 
     * 所有的unbind()方法最后都调用这个方法。
     */
    public final void unbind(Iterable<? extends SocketAddress> localAddresses) {
    	// 1. 各种验证
        if (localAddresses == null) {
            throw new IllegalArgumentException("localAddresses");
        }

        boolean deactivate = false;
        synchronized (bindLock) {
            synchronized (boundAddresses) {
                if (boundAddresses.isEmpty()) {
                    return;
                }
                List<SocketAddress> localAddressesCopy = new ArrayList<SocketAddress>();
                int specifiedAddressCount = 0;
                for (SocketAddress a : localAddresses) {
                    specifiedAddressCount++;
                    if ((a != null) && boundAddresses.contains(a)) {
                        localAddressesCopy.add(a);
                    }
                }
                // 这里使用specifiedAddressCount变量是不是多次一举？因为可以使用localAddressesCopy.isEmpty()来判断。
                if (specifiedAddressCount == 0) {
                    throw new IllegalArgumentException("localAddresses is empty.");
                }

                // 2. 做解绑操作。
                if (!localAddressesCopy.isEmpty()) {
                    try {
                        unbind0(localAddressesCopy);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeIoException("Failed to unbind from: " + getLocalAddresses(), e);
                    }
                    boundAddresses.removeAll(localAddressesCopy);
                    if (boundAddresses.isEmpty()) {
                        deactivate = true;
                    }
                }
            }
        }

        // 3. 如果解绑操作完成后没有已绑定的本地Socke地址了，表示service已经不活跃了，通知所有IoServiceListener。
        if (deactivate) {
            getListeners().fireServiceDeactivated();
        }
    }
    
    /**
     * Starts the acceptor, and register the given addresses
     * 
     * 绑定给定的本地Socket地址列表，开启本acceptor。
     * 
     * @param localAddresses The address to bind to
     * @return the {@link Set} of the local addresses which is bound actually
     * @throws Exception If the bind failed
     */
    protected abstract Set<SocketAddress> bindInternal(List<? extends SocketAddress> localAddresses) throws Exception;

    /**
     * Implement this method to perform the actual unbind operation.
     * 
     * 解绑给定的本地Socket地址列表。
     * 
     * @param localAddresses The address to unbind from
     * @throws Exception If the unbind failed
     */
    protected abstract void unbind0(List<? extends SocketAddress> localAddresses) throws Exception;
    
    @Override
    public String toString() {
        TransportMetadata m = getTransportMetadata();
        return '('
                + m.getProviderName()
                + ' '
                + m.getName()
                + " acceptor: "
                + (isActive() ? "localAddress(es): " + getLocalAddresses() + ", managedSessionCount: "
                        + getManagedSessionCount() : "not bound") + ')';
    }
    
    /**
     * 检测参数的类型是否符合TransportMetadata.getAddressType()规定的类型。
     * @param a
     */
    private void checkAddressType(SocketAddress a) {
        if (a != null && !getTransportMetadata().getAddressType().isAssignableFrom(a.getClass())) {
            throw new IllegalArgumentException("localAddress type: " + a.getClass().getSimpleName() + " (expected: "
                    + getTransportMetadata().getAddressType().getSimpleName() + ")");
        }
    }
    
    /**
     * 一个代表acceptor操作结果的类
     * 
     * @date	2016年6月17日 下午3:46:35
     */
    public static class AcceptorOperationFuture extends ServiceOperationFuture {
    	
    	private final List<SocketAddress> localAddresses;
    	
    	public AcceptorOperationFuture(List<? extends SocketAddress> localAddresses) {
            this.localAddresses = new ArrayList<SocketAddress>(localAddresses);
        }

        public final List<SocketAddress> getLocalAddresses() {
            return Collections.unmodifiableList(localAddresses);
        }
        
        /**
         * @see Object#toString()
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Acceptor operation : ");
            if (localAddresses != null) {
                boolean isFirst = true;
                for (SocketAddress address : localAddresses) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(address);
                }
            }
            return sb.toString();
        }
    }
}
