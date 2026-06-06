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
package eu.doppelhelix.test.dbus.java.test;

import eu.doppelhelix.govee.GVH5075Main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.bluez.Adapter1;
import org.bluez.Device1;
import org.bluez.GattCharacteristic1;
import org.bluez.GattService1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.Variant;

public class ManualScanAndDumpServices {
    private static final String BLUEZ_BUSNAME = "org.bluez";
    private static final System.Logger LOG = System.getLogger(ManualScanAndDumpServices.class.getName());

    public static void main(String[] args) throws Exception {
        MethodCall.setDefaultTimeout(60_000);
        try (DBusConnection connection = DBusConnectionBuilder.forSystemBus().build()) {
            DBusPath path = findGoveeDevices(connection);

            Device1 dev = connection.getRemoteObject(BLUEZ_BUSNAME, path, Device1.class);

            dev.Connect();

            ObjectManager objectManager = connection.getRemoteObject(BLUEZ_BUSNAME, "/", ObjectManager.class);

            Map<String, Map<String, Variant<?>>> services = new HashMap<>();
            Map<String, Map<String, Map<String, Variant<?>>>> characteristics = new HashMap<>();

            for (int i = 0; i < 1000; i++) {
                Map<DBusPath, Map<String, Map<String, Variant<?>>>> objects = objectManager.GetManagedObjects();
                for (Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objects.entrySet()) {
                    if (e.getValue().containsKey(GattCharacteristic1.class.getName())) {
                        Map<String, Variant<?>> characteristicData = e.getValue().get(GattCharacteristic1.class.getName());
                        DBusPath servicePath = (DBusPath) characteristicData.get("Service").getValue();
                        Map<String, Map<String, Variant<?>>> serviceProperties = objects
                                .entrySet()
                                .stream()
                                .filter(f -> servicePath.getPath().equals(f.getKey().getPath()))
                                .findFirst()
                                .map(f -> f.getValue())
                                .orElse(null);
                        DBusPath devicePath = (DBusPath) serviceProperties.get(GattService1.class.getName()).get("Device").getValue();
                        if (path.getPath().equals(devicePath.getPath())) {
                            Map<String, Variant<?>> serviceProps = serviceProperties.get(GattService1.class.getName());
                            String serviceUUID = serviceProps.get("UUID").toString();
                            services.put(serviceUUID, serviceProps);
                            characteristics
                                    .computeIfAbsent(serviceUUID, (s) -> new HashMap<String, Map<String, Variant<?>>>())
                                    .put(characteristicData.get("UUID").toString(), characteristicData);
                        }
                    }
                }
            }

            services.entrySet().forEach(e -> {
                System.out.println(e.getKey());
                System.out.println("\t" + e.getValue());
                characteristics.get(e.getKey()).entrySet().forEach(e2 -> {
                    System.out.println("\t\t" + e2.getKey());
                });
            });
        }
    }

    private static DBusPath findGoveeDevices(final DBusConnection dbusConn) throws DBusException, InterruptedException {

        ObjectManager objectManager = dbusConn.getRemoteObject(BLUEZ_BUSNAME, "/", ObjectManager.class);

        List<Adapter1> bluetoothAdapter = new ArrayList<>();
        for (Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objectManager.GetManagedObjects().entrySet()) {
            if (e.getValue().containsKey(Adapter1.class.getName())) {
                bluetoothAdapter.add(dbusConn.getRemoteObject(BLUEZ_BUSNAME, e.getKey(), Adapter1.class));
            }
        }

        bluetoothAdapter.forEach(ad -> {
            LOG.log(System.Logger.Level.DEBUG, () -> "Started discovery for adapter: " + ad.getName() + " / " + ad.getAddress());
            ad.StartDiscovery();
        });

        while (bluetoothAdapter.stream().allMatch(ad -> ad.isDiscovering())) {
            Thread.sleep(100);
            for (Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> e : objectManager.GetManagedObjects().entrySet()) {
                if (e.getValue().containsKey(Device1.class.getName())) {
                    Variant<?> name = e.getValue().get(Device1.class.getName()).get("Name");
                    Variant<?> address = e.getValue().get(Device1.class.getName()).get("Address");
                    if (name != null) {
                        Object value = name.getValue();
                        if (value instanceof String nameString) {
                            if (nameString.startsWith("GVH5075")) {
                                LOG.log(System.Logger.Level.DEBUG, () -> "Found GVH5075 device " + address.getValue() + " => " + nameString);
                                return e.getKey();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

}
