package org.bluez;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface GattCharacteristic1 extends DBusInterface {

    byte[] ReadValue(Map<String, Variant<?>> options);

    void WriteValue(byte[] value, Map<String, Variant<?>> options);

    AcquireWriteTuple<FileDescriptor, UInt16> AcquireWrite(Map<String, Variant<?>> options);

    AcquireNotifyTuple<FileDescriptor, UInt16> AcquireNotify(Map<String, Variant<?>> options);

    void StartNotify();

    void StopNotify();

    @DBusBoundProperty
    UInt16 getHandle();

    @DBusBoundProperty
    String getUUID();

    @DBusBoundProperty
    DBusPath getService();

    @DBusBoundProperty(type = PropertyValueType.class)
    List<Byte> getValue();

    @DBusBoundProperty
    boolean isNotifying();

    @DBusBoundProperty(type = PropertyFlagsType.class)
    List<String> getFlags();

    @DBusBoundProperty
    boolean isWriteAcquired();

    @DBusBoundProperty
    boolean isNotifyAcquired();

    @DBusBoundProperty
    UInt16 getMTU();

    public static interface PropertyValueType extends TypeRef<List<Byte>> {

    }

    public static interface PropertyFlagsType extends TypeRef<List<String>> {

    }

}
