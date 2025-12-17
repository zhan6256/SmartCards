package com.daclink.mydemoapplication.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.daclink.mydemoapplication.Database.entities.Course;

import java.util.List;

/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: CourseDAO interface
 */
@Dao
public interface CourseDAO {

    @Insert
    void insert(Course course);

    @Query("SELECT * FROM course ORDER BY courseName")
    List<Course> getAllCourses();

    @Query("SELECT * FROM course WHERE createdByUserId = :userId")
    List<Course> getCoursesForUser(int userId);

    // ✅ NEW: update a course (for Edit)
    @Update
    void update(Course course);

    // ✅ NEW: delete a single course (for Delete)
    @Delete
    void delete(Course course);

    // (Optional alternative if you prefer query instead of @Delete)
    // @Query("DELETE FROM course WHERE courseId = :courseId")
    // void deleteCourseById(int courseId);

    @Query("DELETE FROM course")
    void deleteAll();
}
