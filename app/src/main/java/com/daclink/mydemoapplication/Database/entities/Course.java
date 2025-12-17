package com.daclink.mydemoapplication.Database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: Course class
 */

@Entity(
        tableName = "course",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",              // MUST match User's PK field name
                childColumns = "createdByUserId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Course {

    @PrimaryKey(autoGenerate = true)
    private int courseId;

    private String courseName;
    private String courseDescription;

    // FK -> User.id
    private int createdByUserId;

    public Course(String courseName, String courseDescription, int createdByUserId) {
        this.courseName = courseName;
        this.courseDescription = courseDescription;
        this.createdByUserId = createdByUserId;
    }

    // ---- Getters/Setters (Room works best when these exist, since your User uses private fields) ----
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}

