package com.example.tasklist;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "user_tasks")
public class UserTask {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "task_id")
    private String id;

    private String taskTitle;
    private String taskDescription;

    public UserTask() {
        this.id = UUID.randomUUID().toString();
    }

    public UserTask(String taskTitle, String taskDescription) {
        this.id = UUID.randomUUID().toString();
        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }
}
