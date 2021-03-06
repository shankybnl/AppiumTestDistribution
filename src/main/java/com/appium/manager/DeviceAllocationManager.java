package com.appium.manager;

import com.appium.utils.AppiumDevice;
import com.appium.utils.DevicesByHost;
import com.appium.utils.HostMachineDeviceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DeviceAllocationManager - Handles device initialisation, allocation and de-allocattion
 */
public class DeviceAllocationManager {

    private static DeviceAllocationManager instance;
    private static final Logger LOGGER = Logger.getLogger(Class.class.getName());
    private HostMachineDeviceManager hostMachineDeviceManager;
    private List<AppiumDevice> allDevices;
    private List<Thread> suspendedThreads;

    private DeviceAllocationManager() throws Exception {
        try {
            suspendedThreads = new ArrayList<>();
            hostMachineDeviceManager = HostMachineDeviceManager.getInstance();
            ArtifactsUploader.getInstance().initializeArtifacts();
            DevicesByHost appiumDeviceByHost = hostMachineDeviceManager.getDevicesByHost();
            allDevices = appiumDeviceByHost.getAllDevices();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static DeviceAllocationManager getInstance() throws Exception {
        if (instance == null) {
            instance = new DeviceAllocationManager();
        }
        return instance;
    }


    public List<AppiumDevice> getDevices() {
        LOGGER.info("All devices connected");
        return hostMachineDeviceManager.getDevicesByHost().getAllDevices();
    }

    public synchronized AppiumDevice getNextAvailableDevice() {
        int i = 0;
        for (AppiumDevice device : allDevices) {
            Thread t = Thread.currentThread();
            t.setName("Thread_" + i);
            i++;
            if (device.isAvailable()) {
                device.blockDevice();
                return device;
            }
        }
        if (Thread.activeCount() > allDevices.size()) {
            suspendedThreads.add(Thread.currentThread());
            Thread.currentThread().suspend();
        }
        return null;
    }

    public synchronized void freeDevice() {
        AppiumDeviceManager.getAppiumDevice().freeDevice();
        LOGGER.info("DeAllocated Device " + AppiumDeviceManager.getAppiumDevice().getDevice()
                .getUdid()
                + " from execution list");
        if (suspendedThreads.size() > 0) {
            suspendedThreads.get(0).resume();
            suspendedThreads.remove(0);
        }
    }

    public void allocateDevice(AppiumDevice appiumDevice) {
        LOGGER.info("Allocated Device " + appiumDevice + " for Execution");
        AppiumDeviceManager.setDevice(appiumDevice);
    }

}
