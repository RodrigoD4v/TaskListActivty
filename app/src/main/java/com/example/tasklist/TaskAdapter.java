package com.example.tasklist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<UserTask> tasks;
    private Context context;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private List<UserTask> selectedTasks = new ArrayList<>();

    public TaskAdapter(Context context, List<UserTask> tasks) {
        this.context = context;
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        UserTask task = tasks.get(position);
        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDescription.setText(task.getTaskDescription());


        if (selectedTasks.contains(task)) {
            holder.itemView.setBackgroundResource(R.drawable.shape_selected); // Fundo destacado
        } else {
            holder.itemView.setBackgroundResource(R.drawable.rounded_background); // Fundo padrão
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(task);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(task);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void toggleSelection(UserTask task) {
        if (selectedTasks.contains(task)) {
            selectedTasks.remove(task);
        } else {
            selectedTasks.add(task);
        }
        notifyDataSetChanged();
    }

    public List<UserTask> getSelectedTasks() {
        return selectedTasks;
    }

    public void clearSelection() {
        selectedTasks.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(UserTask task);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(UserTask task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        TextView taskDescription;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
        }
    }
}
