package com.daclink.mydemoapplication.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.daclink.mydemoapplication.Database.entities.Flashcard;

import java.util.List;

/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: FlashcardDAO interface
 */
@Dao
public interface FlashcardDAO {

    @Insert
    void insert(Flashcard flashcard);

    @Update
    void update(Flashcard flashcard);

    @Delete
    void delete(Flashcard flashcard);

    @Query("SELECT * FROM flashcardTable WHERE courseId = :courseId ORDER BY flashcardId ASC")
    List<Flashcard> getFlashcardsForCourse(int courseId);

    @Query("DELETE FROM flashcardTable")
    void deleteAll();
}
