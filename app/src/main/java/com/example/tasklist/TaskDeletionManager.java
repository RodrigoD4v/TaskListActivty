package com.example.tasklist;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.Executors;

public class TaskDeletionManager {
    private final Context context;
    private final TaskAdapter taskAdapter;
    private final FirebaseFirestore dbFirestore;

    public TaskDeletionManager(Context context, List<UserTask> tasks, TaskAdapter taskAdapter) {
        this.context = context;
        this.taskAdapter = taskAdapter;
        this.dbFirestore = FirebaseFirestore.getInstance();
    }

    public void onTaskLongClick(UserTask task) {
        if (taskAdapter.getSelectedTasks().contains(task)) {
            taskAdapter.getSelectedTasks().remove(task);
        } else {
            taskAdapter.getSelectedTasks().add(task);
        }
        taskAdapter.notifyDataSetChanged();
    }

    public void deleteSelectedTasks() {
        List<UserTask> selectedTasks = taskAdapter.getSelectedTasks();

        if (!selectedTasks.isEmpty()) {
            FirebaseUser user = ((MainActivity) context).getGoogleSignInManager().getCurrentUser();

            for (UserTask task : selectedTasks) {
                dbFirestore.collection("tasks")
                        .document(user.getUid())
                        .collection("user_tasks")
                        .document(task.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            AppDatabase db = AppDatabase.getInstance(context);
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.userTaskDao().deleteTask(task.getId());
                                ((MainActivity) context).runOnUiThread(() -> {
                                    Toast.makeText(context, "Tarefa " + task.getTaskTitle() + " deletada!", Toast.LENGTH_SHORT).show();
                                });
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TaskDeletionManager", "Erro ao deletar tarefa: ", e);
                        });
            }
            taskAdapter.clearSelection();
        } else {
            Toast.makeText(context, "Nenhuma tarefa selecionada.", Toast.LENGTH_SHORT).show();
        }
    }
}
