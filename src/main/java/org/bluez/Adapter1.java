package org.bluez;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt16;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface Adapter1 extends DBusInterface {

    void StartDiscovery();

    void SetDiscoveryFilter(Map<String, Variant<?>> properties);

    void StopDiscovery();

    void RemoveDevice(DBusPath device);

    List<String> GetDiscoveryFilters();

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
    boolean isConnectable();

    @DBusBoundProperty
    void setConnectable(boolean connectable);

    @DBusBoundProperty
    boolean isPowered();

    @DBusBoundProperty
    void setPowered(boolean powered);

    @DBusBoundProperty
    String getPowerState();

    @DBusBoundProperty
    boolean isDiscoverable();

    @DBusBoundProperty
    void setDiscoverable(boolean discoverable);

    @DBusBoundProperty
    UInt32 getDiscoverableTimeout();

    @DBusBoundProperty
    void setDiscoverableTimeout(UInt32 discoverableTimeout);

    @DBusBoundProperty
    boolean isPairable();

    @DBusBoundProperty
    void setPairable(boolean pairable);

    @DBusBoundProperty
    UInt32 getPairableTimeout();

    @DBusBoundProperty
    void setPairableTimeout(UInt32 pairableTimeout);

    @DBusBoundProperty
    boolean isDiscovering();

    @DBusBoundProperty(type = PropertyUUIDsType.class)
    List<String> getUUIDs();

    @DBusBoundProperty
    String getModalias();

    @DBusBoundProperty(type = PropertyRolesType.class)
    List<String> getRoles();

    @DBusBoundProperty(type = PropertyExperimentalFeaturesType.class)
    List<String> getExperimentalFeatures();

    @DBusBoundProperty
    UInt16 getManufacturer();

    @DBusBoundProperty
    byte getVersion();

    public static interface PropertyUUIDsType extends TypeRef<List<String>> {

    }

    public static interface PropertyRolesType extends TypeRef<List<String>> {

    }

    public static interface PropertyExperimentalFeaturesType extends TypeRef<List<String>> {

    }

}
