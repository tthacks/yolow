package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CSVDao {

    @Query("SELECT filename FROM csv")
    List<String> loadAllCSVFileNames();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCSVFile(HapticCSV csv);

    @Delete
    void deleteCSVFile(HapticCSV csv);

    @Query("SELECT * FROM csv WHERE filename = :name")
    HapticCSV loadCSVFileByName(String name);
}
