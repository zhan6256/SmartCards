package com.daclink.mydemoapplication.Database;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.room.Room;

import com.daclink.mydemoapplication.Database.entities.Course;
import com.daclink.mydemoapplication.Database.entities.Flashcard;
import com.daclink.mydemoapplication.Database.entities.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
/*
 * Author: France Zhang
 * Created on: 12/17/2025
 * Description: GymLogDatabaseTest class
 */

@RunWith(AndroidJUnit4.class)
public class GymLogDatabaseTest {

    private GymLogDatabase db;
    private UserDAO userDAO;
    private CourseDAO courseDAO;
    private FlashcardDAO flashcardDAO;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();

        db = Room.inMemoryDatabaseBuilder(context, GymLogDatabase.class)
                // allow DAO calls on test thread (OK for tests)
                .allowMainThreadQueries()
                .build();

        userDAO = db.userDAO();
        courseDAO = db.getCourseDAO();
        flashcardDAO = db.flashcardDAO();
    }

    @After
    public void closeDb() {
        db.close();
    }

    // ---------------------------------------------------------
    // 1) User insert + read back by username (sync)
    // ---------------------------------------------------------
    @Test
    public void insertUser_thenGetByUsernameSync_returnsUser() {
        User u = new User("alice", "pw");
        u.setAdmin(false);
        userDAO.insert(u);

        User loaded = userDAO.getUserByUsernameSync("alice");
        assertNotNull(loaded);
        assertEquals("alice", loaded.getUsername());
        assertEquals("pw", loaded.getPassword());
    }

    // ---------------------------------------------------------
    // 2) Admin flag is saved/retrieved
    // ---------------------------------------------------------
    @Test
    public void insertAdminUser_adminFlagPersists() {
        User admin = new User("adminX", "pw");
        admin.setAdmin(true);
        userDAO.insert(admin);

        User loaded = userDAO.getUserByUsernameSync("adminX");
        assertNotNull(loaded);
        assertTrue(loaded.isAdmin());
    }

    // ---------------------------------------------------------
    // 3) Insert courses and verify getAllCourses returns them
    // ---------------------------------------------------------
    @Test
    public void insertCourses_getAllCourses_returnsAll() {
        User admin = new User("admin1", "admin1");
        admin.setAdmin(true);
        userDAO.insert(admin);

        int adminId = userDAO.getUserByUsernameSync("admin1").getId();

        courseDAO.insert(new Course("Operating Systems", "OS", adminId));
        courseDAO.insert(new Course("Data Structures", "DSA", adminId));

        List<Course> all = courseDAO.getAllCourses();
        assertNotNull(all);
        assertEquals(2, all.size());
    }

    // ---------------------------------------------------------
    // 4) Flashcards are filtered by courseId
    // ---------------------------------------------------------
    @Test
    public void getFlashcardsForCourse_filtersCorrectly() {
        User admin = new User("admin1", "admin1");
        admin.setAdmin(true);
        userDAO.insert(admin);
        int adminId = userDAO.getUserByUsernameSync("admin1").getId();

        Course c1 = new Course("Course1", "Desc1", adminId);
        Course c2 = new Course("Course2", "Desc2", adminId);
        courseDAO.insert(c1);
        courseDAO.insert(c2);

        // reload to get generated IDs
        List<Course> courses = courseDAO.getAllCourses();
        int course1Id = courses.get(0).getCourseId();
        int course2Id = courses.get(1).getCourseId();

        flashcardDAO.insert(new Flashcard(course1Id, "Q1", "A1"));
        flashcardDAO.insert(new Flashcard(course1Id, "Q2", "A2"));
        flashcardDAO.insert(new Flashcard(course2Id, "Q3", "A3"));

        List<Flashcard> course1Cards = flashcardDAO.getFlashcardsForCourse(course1Id);
        List<Flashcard> course2Cards = flashcardDAO.getFlashcardsForCourse(course2Id);

        assertEquals(2, course1Cards.size());
        assertEquals(1, course2Cards.size());
        assertEquals("Q3", course2Cards.get(0).getQuestion());
    }

    // ---------------------------------------------------------
    // 5) Cascade delete: deleting a course deletes its flashcards
    //    (requires ForeignKey.CASCADE on Flashcard -> Course)
    // ---------------------------------------------------------
    @Test
    public void deleteCourse_cascadesToFlashcards() {
        User admin = new User("admin1", "admin1");
        admin.setAdmin(true);
        userDAO.insert(admin);
        int adminId = userDAO.getUserByUsernameSync("admin1").getId();

        Course course = new Course("Course1", "Desc1", adminId);
        courseDAO.insert(course);

        int courseId = courseDAO.getAllCourses().get(0).getCourseId();

        flashcardDAO.insert(new Flashcard(courseId, "Q1", "A1"));
        flashcardDAO.insert(new Flashcard(courseId, "Q2", "A2"));
        assertEquals(2, flashcardDAO.getFlashcardsForCourse(courseId).size());

        // delete course -> should cascade delete flashcards
        Course loadedCourse = courseDAO.getAllCourses().get(0);
        courseDAO.delete(loadedCourse);

        List<Flashcard> after = flashcardDAO.getFlashcardsForCourse(courseId);
        assertTrue(after.isEmpty());
    }

    // ---------------------------------------------------------
    // 6) Update flashcard changes persisted values
    // ---------------------------------------------------------
    @Test
    public void updateFlashcard_persistsChanges() {
        User admin = new User("admin1", "admin1");
        admin.setAdmin(true);
        userDAO.insert(admin);
        int adminId = userDAO.getUserByUsernameSync("admin1").getId();

        courseDAO.insert(new Course("Course1", "Desc1", adminId));
        int courseId = courseDAO.getAllCourses().get(0).getCourseId();

        flashcardDAO.insert(new Flashcard(courseId, "OldQ", "OldA"));

        Flashcard card = flashcardDAO.getFlashcardsForCourse(courseId).get(0);
        card.setQuestion("NewQ");
        card.setAnswer("NewA");
        flashcardDAO.update(card);

        Flashcard updated = flashcardDAO.getFlashcardsForCourse(courseId).get(0);
        assertEquals("NewQ", updated.getQuestion());
        assertEquals("NewA", updated.getAnswer());
    }
}

