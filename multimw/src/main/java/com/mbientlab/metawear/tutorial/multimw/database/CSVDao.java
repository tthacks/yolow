package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
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

    @Query("SELECT * FROM csv WHERE _id = :id")
    HapticCSV loadCSVFileById(int id);

    @Query("SELECT * FROM csv WHERE filename = :filename")
    HapticCSV loadCSVFileByName(String filename);
}
