package org.bluez;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class AcquireWriteTuple<A, B> extends Tuple {
    @Position(0)
    private A fd;
    @Position(1)
    private B mtu;

    public AcquireWriteTuple(A fd, B mtu) {
        this.fd = fd;
        this.mtu = mtu;
    }

    public void setFd(A arg) {
        fd = arg;
    }

    public A getFd() {
        return fd;
    }

    public void setMtu(B arg) {
        mtu = arg;
    }

    public B getMtu() {
        return mtu;
    }

}
