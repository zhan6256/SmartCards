package com.daclink.mydemoapplication.Database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.daclink.mydemoapplication.Database.entities.GymLog;
import com.daclink.mydemoapplication.Database.entities.User;
import com.daclink.mydemoapplication.MainActivity;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/*
 * Author: France Zhang
 * Created on: 12/02/2025
 * Description: GymLogRepository class
 */

public class GymLogRepository {
    private final GymLogDAO gymLogDAO;
    private final UserDAO userDAO;
    private ArrayList<GymLog> allLogs;

    private static GymLogRepository repository;

    private GymLogRepository(Application application){
        GymLogDatabase db = GymLogDatabase.getDatabase(application);
        this.gymLogDAO = db.gymLogDAO();
        this.userDAO = db.userDAO();
        this.allLogs = (ArrayList<GymLog>) this.gymLogDAO.getAllRecords();
    }

    public static GymLogRepository getRepository(Application application){
        if(repository != null){
            return repository;
        }
        Future<GymLogRepository> future = GymLogDatabase.databaseWriteExecutor.submit(
                new Callable<GymLogRepository>() {
                    @Override
                    public GymLogRepository call() throws Exception {
                        return new GymLogRepository(application);
                    }
                }
        );
        try{
            return future.get();
        }catch (InterruptedException | ExecutionException e){
            Log.d(MainActivity.TAG, "Problem getting GymLogRepository, thread error.");
        }
        return null;
    }
    public ArrayList<GymLog> getAllLogs() {
        Future<ArrayList<GymLog>> future = GymLogDatabase.databaseWriteExecutor.submit(
                new Callable<ArrayList<GymLog>>(){
                    @Override
                    public ArrayList<GymLog> call() throws Exception{
                        return (ArrayList<GymLog>) gymLogDAO.getAllRecords();
                    }
                });
        try{
            return future.get();
        }catch (InterruptedException | ExecutionException e){
            Log.i(MainActivity.TAG, "Problem when getting all GymLogs in the repository");
        }
        return null;
    }
    public void insertGymLog(GymLog gymLog){
        GymLogDatabase.databaseWriteExecutor.execute(()->
        {
            gymLogDAO.insert(gymLog);
        });
    }

    public void insertUser(User... user){
        GymLogDatabase.databaseWriteExecutor.execute(()->
        {
            userDAO.insert(user);
        });
    }

    public LiveData<User> getUserByUserName(String username) {

        return userDAO.getUserByUserName(username);
    }

    public LiveData<User> getUserByUserId(int userId) {

        return userDAO.getUserByUserId(userId);
    }
}
