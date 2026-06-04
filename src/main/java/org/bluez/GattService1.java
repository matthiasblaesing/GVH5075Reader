package org.bluez;

import java.util.List;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;

/**
 * Auto-generated class.
 */
public interface GattService1 extends DBusInterface {

    @DBusBoundProperty
    UInt16 getHandle();

    @DBusBoundProperty
    String getUUID();

    @DBusBoundProperty
    DBusPath getDevice();

    @DBusBoundProperty
    boolean isPrimary();

    @DBusBoundProperty(type = PropertyIncludesType.class)
    List<DBusPath> getIncludes();

    public static interface PropertyIncludesType extends TypeRef<List<DBusPath>> {

    }

}
