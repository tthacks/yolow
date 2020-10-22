package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PresetDao {

    @Query("SELECT * FROM presets")
    List<Preset> loadAllPresets();

    @Query("Select * FROM presets WHERE isDefault")
    Preset getDefaultPreset();

    @Query("SELECT name FROM presets")
    List<String> loadAllPresetNames();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPreset(Preset p);

    @Delete
    void deletePreset(Preset p);

    @Update
    void updatePreset(Preset p);

    @Query("SELECT * FROM presets WHERE _id = :id")
    Preset loadPresetFromId(int id);

    @Query("SELECT _id FROM presets WHERE name = :name")
    int getIdFromPresetName(String name);
}
