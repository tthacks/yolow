package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM sessions")
    List<Session> loadAllSessions();

    @Query("SELECT * FROM sessions WHERE name = :name")
    Session getSession(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Session s);

    @Update
    void update(Session s);
}
