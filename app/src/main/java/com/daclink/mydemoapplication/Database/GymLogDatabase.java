package com.daclink.mydemoapplication.Database;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.daclink.mydemoapplication.Database.entities.GymLog;
import com.daclink.mydemoapplication.Database.entities.User;
import com.daclink.mydemoapplication.Database.typeConverters.LocalDateTypeConverter;
import com.daclink.mydemoapplication.MainActivity;
import com.daclink.mydemoapplication.Database.entities.Course;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/*
 * Author: France Zhang
 * Created on: 12/02/2025
 * Description: GymLogDatabase class
 */
@TypeConverters(LocalDateTypeConverter.class)
@Database(entities = {GymLog.class, User.class, Course.class}, version = 4, exportSchema = false)
public abstract class GymLogDatabase extends RoomDatabase {
    public static final String USER_TABLE = "usertable";
    private static final String DATABASE_NAME = "GymLogdatabase";

    public static final String GYM_LOG_TABLE = "gymLogTable";

    private static volatile GymLogDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
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
                            .addCallback(addDefaultValues)
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);

                                    databaseWriteExecutor.execute(() -> {
                                        UserDAO userDAO = INSTANCE.userDAO();        // ✅ use your abstract method
                                        CourseDAO courseDAO = INSTANCE.getCourseDAO();

                                        User admin = userDAO.getUserByUsernameSync("admin1");
                                        if (admin == null) {
                                            User adminUser = new User("admin1", "admin1");
                                            adminUser.setAdmin(true);
                                            userDAO.insert(adminUser);
                                            admin = userDAO.getUserByUsernameSync("admin1");
                                        }

                                        if (admin != null) {
                                            courseDAO.insert(new Course(
                                                    "Introduction to Programming",
                                                    "Learn basic programming concepts and problem solving.",
                                                    admin.getId()
                                            ));
                                            courseDAO.insert(new Course(
                                                    "Operating Systems",
                                                    "Processes, memory, scheduling, and file systems.",
                                                    admin.getId()
                                            ));
                                            courseDAO.insert(new Course(
                                                    "Data Structures & Algorithms",
                                                    "Lists, trees, graphs, sorting, and algorithm analysis.",
                                                    admin.getId()
                                            ));
                                        }
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback addDefaultValues = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.i(MainActivity.TAG, "DATABASE CREATED!");
            databaseWriteExecutor.execute(() -> {
                UserDAO dao = INSTANCE.userDAO();
                dao.deleteAll();
                User admin = new User("admin1", "admin1");
                admin.setAdmin(true);
                dao.insert(admin);
                User testUser1 = new User("testuser1", "testuser1");
                dao.insert(testUser1);
            });
        }
    };

    public abstract GymLogDAO gymLogDAO();
    public abstract UserDAO userDAO();
    public abstract CourseDAO getCourseDAO();

}
