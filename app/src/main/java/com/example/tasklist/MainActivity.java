package com.example.tasklist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EDIT_TASK = 1;
    private GoogleSignInManager googleSignInManager;
    private TextView welcomeText;
    private Button loginButton;
    private ImageView hamburgerMenu;
    private DrawerLayout drawerLayout;
    private boolean isMenuOpen = false;
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<UserTask> tasks;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleSignInManager = new GoogleSignInManager(this);
        welcomeText = findViewById(R.id.welcomeText);
        loginButton = findViewById(R.id.loginButton);
        hamburgerMenu = findViewById(R.id.hamburgerMenu);
        drawerLayout = findViewById(R.id.drawer_layout);
        FloatingActionButton addTaskButton = findViewById(R.id.addTaskButton);
        recyclerView = findViewById(R.id.recyclerView);

        tasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, tasks);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

        FirebaseUser user = googleSignInManager.getCurrentUser();
        updateUI(user);

        loadTasks();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser currentUser = googleSignInManager.getCurrentUser();
                if (currentUser != null) {
                    logout();
                } else {
                    login();
                }
            }
        });

        hamburgerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });

        ImageView closeMenu = findViewById(R.id.closeMenu);
        closeMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
            }
        });


        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                startActivity(intent);
            }
        });

        taskAdapter.setOnItemClickListener(new TaskAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(UserTask task) {
                Intent intent = new Intent(MainActivity.this, EditTaskActivity.class);
                intent.putExtra("taskId", task.getId());
                intent.putExtra("taskTitle", task.getTaskTitle());
                intent.putExtra("taskDescription", task.getTaskDescription());
                startActivity(intent);
            }
        });
    }

    private void loadTasks() {
        AppDatabase db = AppDatabase.getInstance(this);
        db.userTaskDao().getAllTasks().observe(this, new Observer<List<UserTask>>() {
            @Override
            public void onChanged(List<UserTask> userTasks) {
                tasks.clear();
                tasks.addAll(userTasks);
                taskAdapter.notifyDataSetChanged();

                FirebaseUser user = googleSignInManager.getCurrentUser();
                if (user != null) {
                    syncTasksWithFirestore(); //Isso sincroniza com Firestore apenas se o usuário estiver logado não retirar
                    loadFirestoreTasks(user);
                }
            }
        });
    }


    private void syncTasksWithFirestore() {
        FirebaseUser user = googleSignInManager.getCurrentUser();
        if (user != null) {
            FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();

            AppDatabase.getInstance(this).userTaskDao().getAllTasks().observe(this, localTasks -> {
                if (localTasks != null && !localTasks.isEmpty()) {
                    for (UserTask localTask : localTasks) {
                        String taskIdAsString = localTask.getId();

                        dbFirestore.collection("tasks")
                                .document(user.getUid())
                                .collection("user_tasks")
                                .document(taskIdAsString)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        dbFirestore.collection("tasks").document(user.getUid())
                                                .collection("user_tasks").document(taskIdAsString)
                                                .set(localTask)
                                                .addOnSuccessListener(documentReference -> {
                                                    Executors.newSingleThreadExecutor().execute(() -> {
                                                        AppDatabase.getInstance(this).userTaskDao().update(localTask);
                                                    });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("MainActivity", "Erro ao adicionar tarefa: ", e);
                                                });
                                    } else {
                                        Log.d("MainActivity", "Tarefa já existe no Firestore: " + taskIdAsString);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("MainActivity", "Erro ao verificar tarefa no Firestore: ", e);
                                });
                    }
                } else {
                    Log.d("MainActivity", "Nenhuma tarefa local encontrada.");
                }
            });
        }
    }

    private void toggleMenu() {
        if (!isMenuOpen) {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            welcomeText.setText("Bem-vindo, " + user.getDisplayName());
            loginButton.setText("Logout");
            loadFirestoreTasks(user);
        } else {
            welcomeText.setText("Bem-vindo ao Notes SNP");
            loginButton.setText("Login com Google");
        }
    }

    private void loadFirestoreTasks(FirebaseUser user) {
        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();

        // Primeiro, carregue as tarefas uma vez
        dbFirestore.collection("tasks")
                .document(user.getUid())
                .collection("user_tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            UserTask firestoreTask = document.toObject(UserTask.class);
                            if (firestoreTask != null) {
                                // Verifica se a tarefa já está na lista pelo ID
                                boolean exists = false;
                                for (UserTask task : tasks) {
                                    if (task.getId().equals(firestoreTask.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    tasks.add(firestoreTask);
                                }
                            }
                        }
                        taskAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("MainActivity", "Nenhuma tarefa encontrada no Firestore.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Erro ao carregar tarefas do Firestore: ", e);
                });

        dbFirestore.collection("tasks")
                .document(user.getUid())
                .collection("user_tasks")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w("MainActivity", "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        tasks.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            UserTask firestoreTask = document.toObject(UserTask.class);
                            if (firestoreTask != null) {
                                tasks.add(firestoreTask);
                            }
                        }
                        taskAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("MainActivity", "Nenhuma tarefa encontrada no Firestore.");
                    }
                });
    }

    private void login() {
        googleSignInManager.signIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GoogleSignInManager.RC_SIGN_IN) {
            googleSignInManager.handleSignInResult(data, this::updateUI);
        } else if (requestCode == REQUEST_CODE_EDIT_TASK && resultCode == RESULT_OK) {
            loadTasks();
        }
    }

    private void logout() {
        googleSignInManager.signOut();
        updateUI(null);
        loadTasks();
        Toast.makeText(this, "Logout realizado com sucesso.", Toast.LENGTH_SHORT).show();
    }
}