package cn.gavin.snmp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.gavin.snmp.core.exception.IPAddressFormatError;
import cn.gavin.snmp.core.model.DeviceImp;
import cn.gavin.snmp.core.model.Protocol;
import cn.gavin.snmp.core.model.SNMPParameter;
import cn.gavin.snmp.core.model.SNMPVersion;
import cn.gavin.snmp.core.service.DeviceManager;
import cn.gavin.snmp.core.service.GroupManager;
import cn.gavin.snmp.core.service.OIDManager;
import cn.gavin.snmp.core.snmputil.trap.SnmpTrapSender;

/**
 * Created by gluo on 11/9/2016.
 */
public class MainController {
    private static MainController mainController;
    private Context context;
    private DeviceManager deviceManager;
    private GroupManager groupManager;
    private OIDManager oidManager;

    private MainController(Context context){
        this.context = context;
        deviceManager = new cn.gavin.snmp.db.DeviceManager(context);
        groupManager = new cn.gavin.snmp.db.GroupManager(context);
        oidManager = new cn.gavin.snmp.db.OIDManager(context);
    }
    public synchronized static MainController init(Context context) {
        if (mainController == null) {
            mainController = new MainController(context);
        }
        return mainController;
    }

    public synchronized static DeviceManager getDeviceManager() {
        return mainController.deviceManager;
    }

    public synchronized static GroupManager getGroupManager() {
        return mainController.groupManager;
    }

    public synchronized static OIDManager getOIDManger() {
        return mainController.oidManager;
    }

    public DeviceImp addV1Device(String ip, String community) {
        return createDevice(ip, createParameter(SNMPVersion.V1, community, null, null, null, null));
    }

    public DeviceImp createDevice(String ip, SNMPParameter parameter) {
        DeviceImp device = new DeviceImp(ip);
        try {
            device.initDevice(parameter);
            device.setName(ip);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return device;
    }

    public DeviceImp addV2cDevice(String ip, String community) {
        return createDevice(ip, createParameter(SNMPVersion.V2C, community, null, null, null, null));
    }

    public DeviceImp addV3Device(String ip, Protocol authProc, String authString, Protocol privProc, String privString) {
        return createDevice(ip, createParameter(SNMPVersion.V3, null, authProc, authString, privProc, privString));
    }

    public SNMPParameter createParameter(@NonNull SNMPVersion version, @Nullable String community, @Nullable Protocol authProc, @Nullable String authString, @Nullable Protocol privProc, @Nullable String privString) {
        SNMPParameter parameter = new SNMPParameter();
        parameter.setVersion(version);
        parameter.setAuthProtocol(authProc);
        parameter.setAuthentication(authString);
        parameter.setPrivacyProtocol(privProc);
        parameter.setPrivacy(privString);
        parameter.setCommunity(community);
        return parameter;
    }

    public List<DeviceImp> discoveryDevices(String from, String to, SNMPParameter parameter) {
        String[] froms = from.split("\\.");
        String[] tos = to.split("\\.");
        int fa0 = Integer.parseInt(froms[0]);
        int ta0 = Integer.parseInt(tos[0]);
        if (fa0 > ta0) {
            throw new IPAddressFormatError("The start address " + froms[0] + " is larger than end address " + tos[0]);
        }
        int fa1 = Integer.parseInt(froms[1]);
        int ta1 = Integer.parseInt(tos[1]);
        if (fa1 > ta1) {
            throw new IPAddressFormatError("The start address " + froms[1] + " is larger than end address " + tos[1]);
        }
        int fa2 = Integer.parseInt(froms[2]);
        int ta2 = Integer.parseInt(tos[2]);
        if (fa2 > ta2) {
            throw new IPAddressFormatError("The start address " + froms[2] + " is larger than end address " + tos[2]);
        }
        int fa3 = Integer.parseInt(froms[3]);
        int ta3 = Integer.parseInt(tos[3]);
        if (fa3 > ta3) {
            throw new IPAddressFormatError("The start address " + froms[3] + " is larger than end address " + tos[3]);
        }
        if (fa0 > 255 || fa1 > 255 || fa2 > 255 || fa3 > 255 || ta0 > 255 || ta1 > 255 || ta2 > 255 || ta3 > 255) {
            throw new IPAddressFormatError("There are some ip address larger than 255");
        }
        if (fa0 < 0 || fa1 < 0 || fa2 < 0 || fa3 < 0 || ta0 < 0 || ta1 < 0 || ta2 < 0 || ta3 < 0) {
            throw new IPAddressFormatError("There are some ip address less than 0");
        }
        List<String> ips = new ArrayList<>();
        String ipFormat = "%i.%j.%k.%l";
        for (int i = fa0; i < ta0; i++) {
            for (int j = fa1; j < ta1; j++) {
                for (int k = fa2; k < ta2; k++) {
                    for (int l = fa3; l < ta3; l++) {
                        ips.add(String.format(ipFormat, i, j, k, l));
                    }
                }
            }
        }
        List<DeviceImp> devices = new ArrayList<>(ips.size());
        for (String ip : ips) {
            DeviceImp device = createDevice(ip, parameter);
            device.discovery();
            devices.add(device);
        }
        return devices;
    }

    public Context getContext() {
        return context;
    }

    public void sendTrap(String ip, SNMPParameter parameter, Map<String, Object> values){
        SnmpTrapSender snmpTrapSender = new SnmpTrapSender(ip, parameter.getTrapPort());
        try {
            snmpTrapSender.initSNMP(parameter);
            snmpTrapSender.setVariableBinding(values);
            switch (parameter.getVersion()){
                case V1:
                case V2C:
                    snmpTrapSender.sendV1PDU();
                    break;
                case V3:
                    snmpTrapSender.sendV3PDU();
            }
            snmpTrapSender.sendV1PDU();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
