package org.bluez;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface GattDescriptor1 extends DBusInterface {

    byte[] ReadValue(Map<String, Variant<?>> options);

    void WriteValue(byte[] value, Map<String, Variant<?>> options);

    @DBusBoundProperty
    UInt16 getHandle();

    @DBusBoundProperty
    String getUUID();

    @DBusBoundProperty
    DBusPath getCharacteristic();

    @DBusBoundProperty(type = PropertyValueType.class)
    byte[] getValue();

    public static interface PropertyValueType extends TypeRef<byte[]> {

    }

}
