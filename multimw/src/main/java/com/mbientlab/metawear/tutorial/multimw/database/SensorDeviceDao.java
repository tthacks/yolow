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
    public List<SensorDevice> getSensorList();

    @Query("Select friendly_name from sensordevice")
    public List<String> getNamesOfSensors();

    @Query("SELECT * from sensordevice WHERE uid=:id")
    public SensorDevice getSensorById(String id);

    @Insert
    public void insertSensor(SensorDevice sensor);

    @Query("UPDATE sensordevice SET friendly_name=:fname WHERE uid = :id")
    void updateFriendlyName(String fname, String id);

    @Query("UPDATE sensordevice SET connecting=:connect WHERE uid = :id")
    void updateSensorConnectionStatus(boolean connect, String id);

    @Update
    public void updateSensor(SensorDevice sensor);

    @Delete
    public void deleteSensor(SensorDevice sensor);

    @Query("DELETE FROM sensordevice WHERE uid = :id")
    void deleteSensorById(String id);
}

