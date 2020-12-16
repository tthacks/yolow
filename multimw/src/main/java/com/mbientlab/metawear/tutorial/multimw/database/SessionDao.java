package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM sessions")
    List<Session> loadAllSessions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Session s);
}
