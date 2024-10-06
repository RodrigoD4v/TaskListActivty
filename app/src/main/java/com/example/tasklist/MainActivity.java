package com.example.tasklist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
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
    private TaskDeletionManager taskDeletionManager;
    private boolean isUserAuthenticated = false;


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
        ImageView deleteIcon = findViewById(R.id.deleteIcon);

        tasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(this, tasks);
        taskDeletionManager = new TaskDeletionManager(this, tasks, taskAdapter);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);

        FirebaseUser user = googleSignInManager.getCurrentUser();

        updateUI(user);
        loadTasks(user);

        loginButton.setOnClickListener(v -> {
            FirebaseUser currentUser = googleSignInManager.getCurrentUser();
            if (currentUser != null) {
                logout();
            } else {
                login();
            }
        });

        hamburgerMenu.setOnClickListener(v -> toggleMenu());
        ImageView closeMenu = findViewById(R.id.closeMenu);
        closeMenu.setOnClickListener(v -> drawerLayout.closeDrawers());

        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
        });

        taskAdapter.setOnItemClickListener(task -> {
            Intent intent = new Intent(MainActivity.this, EditTaskActivity.class);
            intent.putExtra("taskId", task.getId());
            intent.putExtra("taskTitle", task.getTaskTitle());
            intent.putExtra("taskDescription", task.getTaskDescription());
            startActivity(intent);
        });

        taskAdapter.setOnItemLongClickListener(taskDeletionManager::onTaskLongClick);

        deleteIcon.setOnClickListener(v -> {
            FirebaseUser user1 = googleSignInManager.getCurrentUser();
            if (user1 != null) {
                taskDeletionManager.deleteSelectedTasks();
            } else {
                Toast.makeText(this, "Por favor, faça login para deletar tarefas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTasks(FirebaseUser user) {
        AppDatabase db = AppDatabase.getInstance(this);
        db.userTaskDao().getAllTasks().observe(this, userTasks -> {
            tasks.clear();
            tasks.addAll(userTasks);
            taskAdapter.notifyDataSetChanged();

            if (user != null) {
                loadFirestoreTasks(user);
                syncTasksWithFirestore(user);
            }
        });
    }

    private void syncTasksWithFirestore(FirebaseUser user) {
        if (user == null) {
            Log.w("MainActivity", "Usuário não autenticado. Sincronização não realizada.");
            isUserAuthenticated = false;
            return;
        }

        Log.d("UID de usuário", user.getUid());
        Log.d("MainActivity", "Usuário autenticado: " + user.getUid());
        isUserAuthenticated = true;

        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();

        // Carregar tarefas locais e sincronizar apenas se o usuário estiver autenticado
        AppDatabase.getInstance(this).userTaskDao().getAllTasks().observe(this, localTasks -> {
            if (localTasks != null && !localTasks.isEmpty() && isUserAuthenticated) { // Verifique a autenticação
                Handler handler = new Handler(Looper.getMainLooper());
                for (UserTask localTask : localTasks) {
                    String taskIdAsString = localTask.getId();

                    handler.postDelayed(() -> {
                        dbFirestore.collection("tasks")
                                .document(user.getUid())
                                .collection("user_tasks")
                                .document(taskIdAsString)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        // Adicionar tarefa ao Firestore se não existir
                                        dbFirestore.collection("tasks").document(user.getUid())
                                                .collection("user_tasks").document(taskIdAsString)
                                                .set(localTask)
                                                .addOnSuccessListener(documentReference -> {
                                                    Executors.newSingleThreadExecutor().execute(() -> {
                                                        AppDatabase.getInstance(this).userTaskDao().update(localTask);
                                                    });
                                                })
                                                .addOnFailureListener(e -> Log.e("MainActivity", "Erro ao adicionar tarefa: ", e));
                                    } else {
                                        Log.d("MainActivity", "Tarefa já existe no Firestore para o usuário: " + user.getUid());
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("MainActivity", "Erro ao verificar tarefa no Firestore: ", e));
                    }, 1000);
                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            welcomeText.setText("Bem-vindo, " + user.getDisplayName());
            loginButton.setText("Logout");
            loadFirestoreTasks(user);
            syncTasksWithFirestore(user);

        } else {
            welcomeText.setText("Bem-vindo ao Notes SNP");
            loginButton.setText("Login com Google");
        }
    }

    private void loadFirestoreTasks(FirebaseUser user) {
        if (user == null) {
            return;
        }

        FirebaseFirestore dbFirestore = FirebaseFirestore.getInstance();
        dbFirestore.collection("tasks")
                .document(user.getUid())
                .collection("user_tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            UserTask firestoreTask = document.toObject(UserTask.class);
                            if (firestoreTask != null) {
                                boolean exists = tasks.stream().anyMatch(task -> task.getId().equals(firestoreTask.getId()));
                                if (!exists) {
                                    tasks.add(firestoreTask);
                                }
                            }
                        }
                        taskAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Erro ao carregar tarefas do Firestore: ", e));

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
            loadTasks(googleSignInManager.getCurrentUser());
        }
    }

    private void logout() {
        googleSignInManager.signOut();
        isUserAuthenticated = false;
        updateUI(null);
        loadTasks(null);
    }


    private void toggleMenu() {
        if (!isMenuOpen) {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    public GoogleSignInManager getGoogleSignInManager() {
        return googleSignInManager;
    }
}
