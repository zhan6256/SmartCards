package com.daclink.mydemoapplication.Database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.daclink.mydemoapplication.Database.entities.Course;

import java.util.List;

@Dao
public interface CourseDAO {

    @Insert
    void insert(Course course);

    @Query("SELECT * FROM course ORDER BY courseName")
    List<Course> getAllCourses();

    @Query("SELECT * FROM course WHERE createdByUserId = :userId")
    List<Course> getCoursesForUser(int userId);

    @Query("DELETE FROM course")
    void deleteAll();
}
