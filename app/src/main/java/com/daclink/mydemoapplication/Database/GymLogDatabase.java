package com.daclink.mydemoapplication.Database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.daclink.mydemoapplication.Database.entities.Course;
import com.daclink.mydemoapplication.Database.entities.Flashcard;
import com.daclink.mydemoapplication.Database.entities.GymLog;
import com.daclink.mydemoapplication.Database.entities.User;
import com.daclink.mydemoapplication.Database.typeConverters.LocalDateTypeConverter;
import com.daclink.mydemoapplication.MainActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@TypeConverters(LocalDateTypeConverter.class)
@Database(
        entities = {GymLog.class, User.class, Course.class, Flashcard.class},
        version = 5,
        exportSchema = false
)
public abstract class GymLogDatabase extends RoomDatabase {

    // ✅ KEEP THESE because your existing entities/DAOs reference them
    public static final String USER_TABLE = "usertable";
    public static final String GYM_LOG_TABLE = "gymLogTable";

    private static final String DATABASE_NAME = "GymLogdatabase";

    private static volatile GymLogDatabase INSTANCE;

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static GymLogDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (GymLogDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    GymLogDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(databaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Seeds Users → Courses → Flashcards
     */
    private static final RoomDatabase.Callback databaseCallback =
            new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                    super.onCreate(db);

                    Log.i(MainActivity.TAG, "DATABASE CREATED");

                    databaseWriteExecutor.execute(() -> {

                        UserDAO userDAO = INSTANCE.userDAO();
                        CourseDAO courseDAO = INSTANCE.getCourseDAO();
                        FlashcardDAO flashcardDAO = INSTANCE.flashcardDAO();

                        // --- USERS ---
                        userDAO.deleteAll();

                        User admin = new User("admin1", "admin1");
                        admin.setAdmin(true);
                        userDAO.insert(admin);

                        User testUser = new User("testuser1", "testuser1");
                        userDAO.insert(testUser);

                        User adminUser = userDAO.getUserByUsernameSync("admin1");
                        if (adminUser == null) return;

                        int adminId = adminUser.getId();

                        // --- COURSES ---
                        courseDAO.deleteAll();

                        courseDAO.insert(new Course(
                                "Introduction to Programming",
                                "Learn basic programming concepts.",
                                adminId
                        ));

                        courseDAO.insert(new Course(
                                "Operating Systems",
                                "Processes, memory, and scheduling.",
                                adminId
                        ));

                        courseDAO.insert(new Course(
                                "Data Structures & Algorithms",
                                "Lists, trees, graphs, and algorithms.",
                                adminId
                        ));

                        // --- FLASHCARDS ---
                        List<Course> courses = courseDAO.getAllCourses();

                        for (Course course : courses) {
                            String name = course.getCourseName();
                            int courseId = course.getCourseId();

                            if (name.equalsIgnoreCase("Introduction to Programming")) {
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "What is a variable?",
                                        "A named storage location for a value."
                                ));
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "What is a loop?",
                                        "A structure that repeats code."
                                ));
                            }

                            if (name.equalsIgnoreCase("Operating Systems")) {
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "What is a process?",
                                        "A program in execution."
                                ));
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "What is scheduling?",
                                        "Choosing which process runs next."
                                ));
                            }

                            if (name.equalsIgnoreCase("Data Structures & Algorithms")) {
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "What is Big-O?",
                                        "A measure of algorithm efficiency."
                                ));
                                flashcardDAO.insert(new Flashcard(
                                        courseId,
                                        "Stack vs Queue?",
                                        "Stack = LIFO, Queue = FIFO."
                                ));
                            }
                        }
                    });
                }
            };

    // ---- DAOs ----
    public abstract GymLogDAO gymLogDAO();
    public abstract UserDAO userDAO();
    public abstract CourseDAO getCourseDAO();
    public abstract FlashcardDAO flashcardDAO();
}
