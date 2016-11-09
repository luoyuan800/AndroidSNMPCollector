package cn.gavin.snmp.db;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.gavin.snmp.MainController;
import cn.gavin.snmp.core.model.DeviceImp;
import cn.gavin.snmp.core.model.Protocol;
import cn.gavin.snmp.core.model.SNMPParameter;
import cn.gavin.snmp.core.model.SNMPVersion;

/**
 * Created by gluo on 11/7/2016.
 */
public class DeviceManager extends cn.gavin.snmp.core.service.DeviceManager {
    private Context context;
    private DBHelper dbHelper;

    public DeviceManager(Context context) {
        this.context = context;
        dbHelper = DBHelper.getDbHelper(context);
    }

    @Override
    public DeviceImp getDeviceByName(String name) {
        String sql = "select * from device join community on device.community_id = community.community_id where device_name = '" + name+ "'";
        return buildDeviceImpFromSQL(sql);
    }

    private DeviceImp buildDeviceImpFromSQL(String sql) {
        Cursor cursor = dbHelper.query(sql);
        if (!cursor.isAfterLast()) {
            return buildDeviceImpFromCursor(cursor);
        }
        return null;
    }

    @NonNull
    private DeviceImp buildDeviceImpFromCursor(Cursor cursor) {
        DeviceImp device = new DeviceImp(cursor.getString(cursor.getColumnIndex("device_ip")));
        device.setId(cursor.getString(cursor.getColumnIndex("device_id")));
        device.setName(cursor.getString(cursor.getColumnIndex("device_name")));
        cn.gavin.snmp.core.service.GroupManager groupManager = MainController.getGroupManager();
        device.setGroupNames(groupManager.getGroupNamesByDevice(device));
        int retry = cursor.getInt(cursor.getColumnIndex("retry"));
        long timeOut = cursor.getLong(cursor.getColumnIndex("timeout"));
        int version = cursor.getInt(cursor.getColumnIndex("community_version"));
        device.setCommunity(cursor.getString(cursor.getColumnIndex("community_id")));
        SNMPParameter p = new SNMPParameter();
        p.setTimeout(timeOut);
        p.setRetry(retry);
        p.setPort(cursor.getInt(cursor.getColumnIndex("port")));
        p.setTrapPort(cursor.getInt(cursor.getColumnIndex("trap_port")));
        switch (version) {
            case 1:
                p.setVersion(SNMPVersion.V1);
                p.setCommunity(cursor.getString(cursor.getColumnIndex("community_string")));
                break;
            case 2:
                p.setVersion(SNMPVersion.V2C);
                p.setCommunity(cursor.getString(cursor.getColumnIndex("community_string")));
                break;
            case 3:
                p.setVersion(SNMPVersion.V3);
                p.setAuthentication(cursor.getString(cursor.getColumnIndex("authentucation")));
                p.setAuthProtocol(Protocol.valueOf(cursor.getString(cursor.getColumnIndex("authProtocol"))));
                p.setPrivacy(cursor.getString(cursor.getColumnIndex("privacy")));
                p.setPrivacyProtocol(Protocol.valueOf(cursor.getString(cursor.getColumnIndex("privacyProtocol"))));
                break;
        }
        cursor.close();
        cursor = dbHelper.query("select oid,oid_value,value_time from oid join oid_value on oid.oid_id = oid_value.oid_id where oid.oid='" + DeviceImp.SYSOBJECTID + "' and oid_value.device_id = '" + device.getId() + "' order by value_time");
        while (!cursor.isAfterLast()) {
            device.addSysIdValue(cursor.getLong(cursor.getColumnIndex("value_time")), cursor.getString(cursor.getColumnIndex("oid_value")));
            cursor.moveToNext();
        }
        cursor.close();
        return device;
    }

    @Override
    public DeviceImp getDeviceByIP(String ip) {
        String sql = "select * from device join community on device.community_id = community.community_id where device_ip = '" + ip+ "'";
        return buildDeviceImpFromSQL(sql);
    }

    @Override
    public List<DeviceImp> getAllDevices() {
        String sql = "select * from device";
        Cursor cursor = dbHelper.query(sql);
        List<DeviceImp> devices = new ArrayList<>();
        while (!cursor.isAfterLast()){
            devices.add(buildDeviceImpFromCursor(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return devices;
    }

    @Override
    public List<String> getAllDeviceNames() {
        String sql = "select device_name from device";
        Cursor cursor = dbHelper.query(sql);
        List<String> devices = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            devices.add(cursor.getString(cursor.getColumnIndex("device_name")));
            cursor.moveToNext();
        }
        cursor.close();
        return devices;
    }

    @Override
    public DeviceImp buildDeviceHistoryData(DeviceImp device) {
        String sql = "select * from oid_value JOIN oid ON oid_value.oid_id = oid.oid_id where oid_value.device_id = '" + device.getId() + "'";
        return null;
    }

    @Override
    public DeviceImp save(DeviceImp device) {
        if(device.getId() == null){
            StringBuilder insert = new StringBuilder("insert into device (device_id,device_ip,device_name,community_id,retry,timeout, port, trap_port) values (");
            device.setId(UUID.randomUUID().toString());
            insert.append("'").append(device.getId()).append("',");
            insert.append("'").append(device.getIp()).append("',");
            insert.append("'").append(device.getName()).append("',");
            insert.append("'").append(device.getCommunity()).append("',");
            insert.append(device.getSnmpParameter().getRetry()).append(",");
            insert.append(device.getSnmpParameter().getTimeout()).append(",");
            insert.append(device.getSnmpParameter().getPort()).append(",");
            insert.append(device.getSnmpParameter().getTrapPort()).append(")");
            dbHelper.execSQL(insert.toString());
        }else{
            StringBuilder update = new StringBuilder("update device set ");
            update.append("device_name='").append(device.getName()).append("',");
            update.append("community_id='").append(device.getCommunity()).append("',");
            update.append("retry=").append(device.getSnmpParameter().getRetry()).append(",");
            update.append("timeout=").append(device.getSnmpParameter().getTimeout()).append(",");
            update.append("port=").append(device.getSnmpParameter().getPort()).append(",");
            update.append("trap_port=").append(device.getSnmpParameter().getTrapPort()).append(",");
            update.append(" where device_id = '").append(device.getId());
            dbHelper.execSQL(update.toString());
        }
        return device;
    }


}
