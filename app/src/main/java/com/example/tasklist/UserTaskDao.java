package com.example.tasklist;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserTaskDao {

    @Insert
    long insert(UserTask userTask);

    @Update
    void update(UserTask userTask);

    @Query("SELECT * FROM user_tasks")
    LiveData<List<UserTask>> getAllTasks();

    @Query("SELECT * FROM user_tasks WHERE task_id = :taskId")
    UserTask getTaskById(String taskId);

    @Query("DELETE FROM user_tasks WHERE task_id = :taskId")
    void deleteTask(String taskId);
}
