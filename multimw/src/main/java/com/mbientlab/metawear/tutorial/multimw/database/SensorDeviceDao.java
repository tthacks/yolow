package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensorDeviceDao {
    @Query("SELECT * from sensordevice")
    List<SensorDevice> getSensorList();

    @Insert
    void insertSensor(SensorDevice sensor);

    @Query("UPDATE sensordevice SET friendly_name=:fname WHERE uid = :id")
    void updateFriendlyName(String fname, String id);

    @Query("UPDATE sensordevice SET connecting=:connect WHERE uid = :id")
    void updateSensorConnectionStatus(boolean connect, String id);

    @Query("UPDATE sensordevice SET total_cycles=:total WHERE uid = :id")
    void updateHapticCycle(int total, String id);

    @Query("UPDATE sensordevice SET on_duration=:on WHERE uid = :id")
    void updateOnDuration(float on, String id);

    @Query("UPDATE sensordevice SET off_duration=:off WHERE uid = :id")
    void updateOffDuration(float off, String id);

    @Delete
    void deleteSensor(SensorDevice sensor);
}

