package com.example.tasklist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class AddTaskActivity extends AppCompatActivity {
    private EditText taskTitleInput;
    private EditText taskDescriptionInput;
    private Button saveButton;
    private ImageView backButton;
    private AppDatabase db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        Log.d("AddTaskActivity", "Activity created");

        taskTitleInput = findViewById(R.id.taskTitleInput);
        taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        db = AppDatabase.getInstance(this);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();  // Chama finish() para voltar à tela anterior
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskTitle = taskTitleInput.getText().toString().trim();
                String taskDescription = taskDescriptionInput.getText().toString().trim();

                if (!taskTitle.isEmpty() && !taskDescription.isEmpty()) {
                    UserTask userTask = new UserTask(taskTitle, taskDescription);
                    saveTaskLocally(userTask);
                } else {
                    Toast.makeText(AddTaskActivity.this, "Título e descrição da tarefa não podem estar vazios", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveTaskLocally(UserTask userTask) {
        new Thread(() -> {
            try {
                db.userTaskDao().insert(userTask);
                runOnUiThread(() -> {
                    Toast.makeText(AddTaskActivity.this, "Tarefa salva com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e("AddTaskActivity", "Erro ao salvar tarefa", e);
                runOnUiThread(() -> {
                    Toast.makeText(AddTaskActivity.this, "Erro ao salvar a tarefa", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
