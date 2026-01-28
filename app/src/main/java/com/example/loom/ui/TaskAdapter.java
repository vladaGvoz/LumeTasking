package com.example.loom.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loom.R;
import com.example.loom.model.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("ClassEscapesDefinedScope")
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                            oldItem.getDescription().equals(newItem.getDescription()) &&
                            oldItem.isCompleted() == newItem.isCompleted() &&
                            oldItem.getDueDate() == newItem.getDueDate();
                }
            };
    private final SparseBooleanArray selectedItems = new SparseBooleanArray();
    private boolean selectionMode = false;
    private OnItemClickListener listener;

    public TaskAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }

        selectionMode = selectedItems.size() > 0;
        notifyItemChanged(position);

        if (listener != null) {
            listener.onSelectionCountChanged(selectedItems.size());
        }
    }

    // ---------------- Selection ----------------

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelection() {
        selectedItems.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionCountChanged(0);
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public List<Task> getSelectedTasks() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            tasks.add(getItem(selectedItems.keyAt(i)));
        }
        return tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_list_item, parent, false);
        return new TaskViewHolder(v);
    }

    // ---------------- Adapter ----------------

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);

        boolean isCompleted = task.isCompleted();
        boolean overdue = isOverdue(task);
        boolean isSelected = selectedItems.get(position, false);

        if (isSelected) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFF59D"));
        } else if (overdue) {
            holder.itemView.setBackgroundColor(Color.parseColor("#33D32F2F"));
        } else if (isCompleted) {
            holder.itemView.setBackgroundColor(Color.parseColor("#143A1568"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setAlpha(isCompleted ? 0.75f : 1f);

        // title
        holder.titleTextView.setText(task.getTitle());
        if (isCompleted) {
            holder.titleTextView.setPaintFlags(
                    holder.titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
        } else {
            holder.titleTextView.setPaintFlags(
                    holder.titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG
            );
        }


        // COLORI
        int normalTitleColor = holder.itemView.getContext().getColor(R.color.primaryText);
        int normalDescColor = holder.itemView.getContext().getColor(R.color.secondaryText);
        int overdueColor = holder.itemView.getContext().getColor(R.color.overdue_red);
        int fadedPurple = Color.parseColor("#AA9575CD"); // 67% opacity purple


        holder.titleTextView.setTextColor(
                isCompleted ? fadedPurple
                        : overdue ? overdueColor
                        : normalTitleColor
        );

        // desc
        String desc = task.getDescription();
        if (desc != null && !desc.isEmpty()) {
            holder.descriptionPreview.setVisibility(View.VISIBLE);
            holder.descriptionPreview.setText(desc);
            holder.descriptionPreview.setTextColor
                    (isCompleted ? fadedPurple
                        : overdue ? overdueColor
                        : normalDescColor);
        } else {
            holder.descriptionPreview.setVisibility(View.GONE);
        }

        DateFormat df = DateFormat.getDateInstance();
        holder.dueTextView.setText("Due: " + df.format(new Date(task.getDueDate())));
        holder.dueTextView.setTextColor(
                isCompleted ? fadedPurple
                        : overdue ? overdueColor
                        : normalDescColor
        );

        // checkbox
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(isCompleted);
        holder.checkBox.setOnCheckedChangeListener((b, checked) -> {
            if (!selectionMode && listener != null) {
                listener.onItemCheckChanged(task, checked);
            }
        });
    }

    private boolean isOverdue(Task task) {
        return !task.isCompleted() && task.getDueDate() < System.currentTimeMillis();
    }

    public interface OnItemClickListener {
        void onItemClick(Task task);

        void onItemCheckChanged(Task task, boolean isChecked);

        void onItemLongClick(Task task);

        void onSelectionCountChanged(int count);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, dueTextView, descriptionPreview;
        CheckBox checkBox;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.task_title);
            dueTextView = itemView.findViewById(R.id.task_due_date);
            descriptionPreview = itemView.findViewById(R.id.task_description_preview);
            checkBox = itemView.findViewById(R.id.task_checkbox);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                if (selectionMode) {
                    toggleSelection(pos);
                } else if (listener != null) {
                    listener.onItemClick(getItem(pos));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return true;

                if (!selectionMode) {
                    selectionMode = true;
                    toggleSelection(pos);
                    if (listener != null) listener.onItemLongClick(getItem(pos));
                }
                return true;
            });
        }
    }


}
