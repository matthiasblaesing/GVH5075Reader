package org.bluez;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface Device1 extends DBusInterface {

    void Disconnect();

    void Connect();

    void ConnectProfile(String UUID);

    void DisconnectProfile(String UUID);

    void Pair();

    void CancelPairing();

    @DBusBoundProperty
    String getAddress();

    @DBusBoundProperty
    String getAddressType();

    @DBusBoundProperty
    String getName();

    @DBusBoundProperty
    String getAlias();

    @DBusBoundProperty
    void setAlias(String alias);

    @DBusBoundProperty(name = "Class")
    UInt32 getBluetoothClass();

    @DBusBoundProperty
    UInt16 getAppearance();

    @DBusBoundProperty
    String getIcon();

    @DBusBoundProperty
    boolean isPaired();

    @DBusBoundProperty
    boolean isBonded();

    @DBusBoundProperty
    boolean isTrusted();

    @DBusBoundProperty
    void setTrusted(boolean trusted);

    @DBusBoundProperty
    boolean isBlocked();

    @DBusBoundProperty
    void setBlocked(boolean blocked);

    @DBusBoundProperty
    boolean isLegacyPairing();

    @DBusBoundProperty
    boolean isCablePairing();

    @DBusBoundProperty
    short getRSSI();

    @DBusBoundProperty
    boolean isConnected();

    @DBusBoundProperty(type = PropertyUUIDsType.class)
    List<String> getUUIDs();

    @DBusBoundProperty
    String getModalias();

    @DBusBoundProperty
    DBusPath getAdapter();

    @DBusBoundProperty(type = PropertyManufacturerDataType.class)
    Map<UInt16, Variant<?>> getManufacturerData();

    @DBusBoundProperty(type = PropertyServiceDataType.class)
    Map<String, Variant<?>> getServiceData();

    @DBusBoundProperty
    short getTxPower();

    @DBusBoundProperty
    boolean isServicesResolved();

    @DBusBoundProperty(type = PropertyAdvertisingFlagsType.class)
    byte[] getAdvertisingFlags();

    @DBusBoundProperty(type = PropertyAdvertisingDataType.class)
    Map<Byte, Variant<?>> getAdvertisingData();

    @DBusBoundProperty
    boolean isWakeAllowed();

    @DBusBoundProperty
    void setWakeAllowed(boolean wakeAllowed);

    @DBusBoundProperty(type = PropertySetsType.class)
    Map<DBusPath, Map<String, Variant<?>>> getSets();

    public static class Disconnected extends DBusSignal {

        private final String name;
        private final String message;

        public Disconnected(String path, String name, String message) throws DBusException {
                super(path, name, message);        this.name = name;
                this.message = message;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }

    }

    public static interface PropertyUUIDsType extends TypeRef<List<String>> {

    }

    public static interface PropertyManufacturerDataType extends TypeRef<Map<UInt16, Variant>> {

    }

    public static interface PropertyServiceDataType extends TypeRef<Map<String, Variant>> {

    }

    public static interface PropertyAdvertisingFlagsType extends TypeRef<byte[]> {

    }

    public static interface PropertyAdvertisingDataType extends TypeRef<Map<Byte, Variant>> {

    }

    public static interface PropertySetsType extends TypeRef<Map<DBusPath, Map<String, Variant>>> {

    }

}
