package com.daclink.mydemoapplication.Database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: Flashcard class
 */

@Entity(
        tableName = "flashcardTable",
        foreignKeys = @ForeignKey(
                entity = Course.class,
                parentColumns = "courseId",
                childColumns = "courseId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("courseId")}
)
public class Flashcard {

    @PrimaryKey(autoGenerate = true)
    private int flashcardId;

    // FK -> Course.courseId
    private int courseId;

    private String question;
    private String answer;

    public Flashcard(int courseId, String question, String answer) {
        this.courseId = courseId;
        this.question = question;
        this.answer = answer;
    }

    public int getFlashcardId() {
        return flashcardId;
    }

    public void setFlashcardId(int flashcardId) {
        this.flashcardId = flashcardId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
