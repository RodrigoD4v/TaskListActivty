package com.example.tasklist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditTaskActivity extends AppCompatActivity {
    private EditText taskTitleInput;
    private EditText taskDescriptionInput;
    private Button updateTaskButton;
    private ImageView backButton;
    private UserTaskDao userTaskDao;
    private String taskId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        taskTitleInput = findViewById(R.id.taskTitleInput);
        taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        updateTaskButton = findViewById(R.id.updateTaskButton);
        backButton = findViewById(R.id.backButton);

        Intent intent = getIntent();
        taskId = intent.getStringExtra("taskId"); // Obtemos o ID da tarefa
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskDescription = intent.getStringExtra("taskDescription");

        taskTitleInput.setText(taskTitle);
        taskDescriptionInput.setText(taskDescription);

        AppDatabase db = AppDatabase.getInstance(this);
        userTaskDao = db.userTaskDao();

        updateTaskButton.setOnClickListener(v -> updateTask());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void updateTask() {
        String newTitle = taskTitleInput.getText().toString().trim();
        String newDescription = taskDescriptionInput.getText().toString().trim();

        if (newTitle.isEmpty() || newDescription.isEmpty()) {
            Toast.makeText(this, "Título e descrição não podem estar vazios", Toast.LENGTH_SHORT).show();
            return;
        }

        UserTask updatedTask = new UserTask(newTitle, newDescription);
        updatedTask.setId(taskId);

        new Thread(() -> {
            userTaskDao.update(updatedTask);
            runOnUiThread(() -> {
                Toast.makeText(EditTaskActivity.this, "Tarefa atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();

        syncTaskWithFirestore(updatedTask);
    }

    private void syncTaskWithFirestore(UserTask updatedTask) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
            dbFirestore.collection("tasks").document(user.getUid())
                    .collection("user_tasks")
                    .document(updatedTask.getId())
                    .set(updatedTask)
                    .addOnSuccessListener(aVoid -> Log.d("EditTaskActivity", "Tarefa sincronizada com Firestore."))
                    .addOnFailureListener(e -> Log.e("EditTaskActivity", "Erro ao sincronizar tarefa: ", e));
        } else {
            Toast.makeText(this, "Usuário não autenticado. Sincronização falhou.", Toast.LENGTH_SHORT).show();
        }
    }
}
