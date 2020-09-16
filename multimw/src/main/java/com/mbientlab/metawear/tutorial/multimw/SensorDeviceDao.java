package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;
import android.hardware.SensorAdditionalInfo;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
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

    @Update
    public void updateSensor(SensorDevice sensor);

    @Query("DELETE FROM sensordevice WHERE uid = :id")
    void deleteSensorById(String id);
}

