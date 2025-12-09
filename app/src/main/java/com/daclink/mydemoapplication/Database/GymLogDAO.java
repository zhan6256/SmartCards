package com.daclink.mydemoapplication.Database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.daclink.mydemoapplication.Database.entities.GymLog;

import java.util.List;

/*
 * Author: France Zhang
 * Created on: 12/02/2025
 * Description: GymLogDAO interface
 */


@Dao
public interface GymLogDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GymLog gymlog);
    @Query("SELECT * FROM " + GymLogDatabase.GYM_LOG_TABLE + " ORDER BY date DESC")
    List<GymLog> getAllRecords();
}
