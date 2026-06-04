/* Copyright (c) 2026 Matthias Bläsing, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0.
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 */

package eu.doppelhelix.govee;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;

class ValueListener implements DBusSigHandler<Properties.PropertiesChanged> {

    private static final System.Logger LOG = System.getLogger(ValueListener.class.getName());
    private final String id;
    private final AtomicReference<CompletableFuture<Object>> nextValue = new AtomicReference<>();
    private final AtomicReference<List<Object>> buffer = new AtomicReference<>();

    public ValueListener(String id) {
        this.id = id;
    }

    @Override
    public void handle(Properties.PropertiesChanged _signal) {
        LOG.log(System.Logger.Level.DEBUG, () -> "Receive <<< " + id + " => " + _signal.getPropertiesChanged());
        if (_signal.getPropertiesChanged().containsKey("Value")) {
            Object value = _signal.getPropertiesChanged().get("Value").getValue();
            if (value instanceof List l && !l.isEmpty() && l.get(0) instanceof Byte) {
                LOG.log(System.Logger.Level.DEBUG, () -> "Receive <<< " + id + " => " + HexFormat.of().withDelimiter(" ").formatHex(Util.toByteArray((List<Byte>) l)));
            }
            CompletableFuture<Object> toComplete = this.nextValue.getAndSet(null);
            if (toComplete != null) {
                toComplete.complete(value);
            }
            synchronized (buffer) {
                List<Object> currentBuffer = buffer.get();
                if (currentBuffer != null) {
                    currentBuffer.add(value);
                }
            }
        }
    }

    public Future<Object> getNextValue() {
        return nextValue.updateAndGet(val -> val != null ? val : new CompletableFuture<>());
    }

    public void startBuffering() {
        synchronized (buffer) {
            buffer.set(new ArrayList<>());
        }
    }

    public List<Object> getBufferAndClear() {
        synchronized (buffer) {
            List<Object> data = buffer.get();
            buffer.set(null);
            return data;
        }
    }

}
