package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SensorDeviceDao {
    @Query("SELECT * from sensordevice")
    List<SensorDevice> getSensorList();

    @Query("SELECT * from sensordevice WHERE uid=:id")
    SensorDevice getSensorById(String id);

    @Insert
    void insertSensor(SensorDevice sensor);

    @Query("UPDATE sensordevice SET friendly_name=:fname WHERE uid = :id")
    void updateFriendlyName(String fname, String id);

    @Query("UPDATE sensordevice SET connecting=:connect WHERE uid = :id")
    void updateSensorConnectionStatus(boolean connect, String id);

    @Query("UPDATE sensordevice SET total_duration=:total, off_duration=:off, on_duration=:on WHERE uid = :id")
    void updateHaptic(int on, int off, int total, String id);

    @Query("UPDATE sensordevice SET x_location=:x, y_location=:y WHERE uid = :id")
    void updateXYCoord(int x, int y, String id);

    @Update
    void updateSensor(SensorDevice sensor);

    @Delete
    void deleteSensor(SensorDevice sensor);

    @Query("DELETE FROM sensordevice WHERE uid = :id")
    void deleteSensorById(String id);
}

