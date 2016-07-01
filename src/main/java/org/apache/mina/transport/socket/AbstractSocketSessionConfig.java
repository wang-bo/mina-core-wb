package org.apache.mina.transport.socket;

import org.apache.mina.core.session.AbstractIoSessionConfig;
import org.apache.mina.core.session.IoSessionConfig;

/**
 * 
 * 实现SocketSessionConfig接口的基类。
 * 
 * @date	2016年6月20日 上午11:25:11	completed
 */
public abstract class AbstractSocketSessionConfig extends AbstractIoSessionConfig implements SocketSessionConfig {

	protected AbstractSocketSessionConfig() {
        // Do nothing
    }
	
	/** 
     * {@inheritDoc}
     */
	@Override
    protected final void doSetAll(IoSessionConfig config) {
        if (!(config instanceof SocketSessionConfig)) {
            return;
        }
        if (config instanceof AbstractSocketSessionConfig) {
            // Minimize unnecessary system calls by checking all 'propertyChanged' properties.
        	// 检测propertyChanged属性，减少不必要的系统调用，因为这些TCP相关参数设置都是要配置到系统中去的。
            AbstractSocketSessionConfig cfg = (AbstractSocketSessionConfig) config;
            if (cfg.isKeepAliveChanged()) {
                setKeepAlive(cfg.isKeepAlive());
            }
            if (cfg.isOobInlineChanged()) {
                setOobInline(cfg.isOobInline());
            }
            if (cfg.isReceiveBufferSizeChanged()) {
                setReceiveBufferSize(cfg.getReceiveBufferSize());
            }
            if (cfg.isReuseAddressChanged()) {
                setReuseAddress(cfg.isReuseAddress());
            }
            if (cfg.isSendBufferSizeChanged()) {
                setSendBufferSize(cfg.getSendBufferSize());
            }
            if (cfg.isSoLingerChanged()) {
                setSoLinger(cfg.getSoLinger());
            }
            if (cfg.isTcpNoDelayChanged()) {
                setTcpNoDelay(cfg.isTcpNoDelay());
            }
            if (cfg.isTrafficClassChanged() && getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        } else {
            SocketSessionConfig cfg = (SocketSessionConfig) config;
            setKeepAlive(cfg.isKeepAlive());
            setOobInline(cfg.isOobInline());
            setReceiveBufferSize(cfg.getReceiveBufferSize());
            setReuseAddress(cfg.isReuseAddress());
            setSendBufferSize(cfg.getSendBufferSize());
            setSoLinger(cfg.getSoLinger());
            setTcpNoDelay(cfg.isTcpNoDelay());
            if (getTrafficClass() != cfg.getTrafficClass()) {
                setTrafficClass(cfg.getTrafficClass());
            }
        }
    }
	
	/**
     * @return <tt>true</tt> if and only if the <tt>keepAlive</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当keepAlive属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isKeepAliveChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>oobInline</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当oobInline属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isOobInlineChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>receiveBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当receiveBufferSize属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isReceiveBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>reuseAddress</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当reuseAddress属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isReuseAddressChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>sendBufferSize</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当sendBufferSize属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isSendBufferSizeChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>soLinger</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当soLinger属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isSoLingerChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>tcpNoDelay</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当tcpNoDelay属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isTcpNoDelayChanged() {
        return true;
    }

    /**
     * @return <tt>true</tt> if and only if the <tt>trafficClass</tt> property
     * has been changed by its setter method.  The system call related with
     * the property is made only when this method returns <tt>true</tt>.  By
     * default, this method always returns <tt>true</tt> to simplify implementation
     * of subclasses, but overriding the default behavior is always encouraged.
     * 
     * 当trafficClass属性被setter方法修改时，返回true。
     * 只有本方法返回true，才会去调用系统接口设置相关属性。
     * 这个方法默认返回true可以简化子类实现，但是建议子类要覆盖此方法，防止过多系统调用。
     */
    protected boolean isTrafficClassChanged() {
        return true;
    }
}
