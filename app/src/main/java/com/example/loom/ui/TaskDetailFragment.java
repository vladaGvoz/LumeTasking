package com.example.loom.ui;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.loom.R;
import com.example.loom.model.Task;
import com.example.loom.viewmodel.TaskViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Calendar;

public class TaskDetailFragment extends Fragment {

    private TaskViewModel taskViewModel;
    private NavController navController;

    private EditText editTextTitle;
    private EditText editTextDescription;
    private EditText editTextDate;
    private Button buttonSave;

    private AdView adView; // AdView reference

    private int taskId = -1;
    private long dueDate = 0;
    private boolean isTaskCompleted = false;

    public TaskDetailFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        editTextTitle = view.findViewById(R.id.edit_text_task_title);
        editTextDescription = view.findViewById(R.id.edit_text_task_description);
        editTextDate = view.findViewById(R.id.edit_text_task_date);
        buttonSave = view.findViewById(R.id.button_save_task);

        // Initialize Mobile Ads
        MobileAds.initialize(requireContext(), initializationStatus -> {});

        // Set up AdView
        adView = view.findViewById(R.id.ad_banner);
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }

        // Date picker
        editTextDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int y = calendar.get(Calendar.YEAR);
            int m = calendar.get(Calendar.MONTH);
            int d = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                        editTextDate.setText(date);

                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, dayOfMonth, 0, 0, 0);
                        dueDate = picked.getTimeInMillis();
                    },
                    y, m, d
            );

            dialog.show();
        });

        // Load existing task if editing
        if (getArguments() != null && getArguments().containsKey("taskId")) {
            int id = getArguments().getInt("taskId", -1);
            if (id != -1) {
                taskId = id;
                loadTaskDetails(taskId);
            }
        }

        buttonSave.setOnClickListener(v -> saveTask());
    }

    @SuppressLint("SetTextI18n")
    private void loadTaskDetails(int id) {
        taskViewModel.getTaskById(id).observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                editTextTitle.setText(task.getTitle());
                editTextDescription.setText(task.getDescription());

                dueDate = task.getDueDate();
                if (dueDate > 0) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(dueDate);
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    int month = c.get(Calendar.MONTH) + 1;
                    int year = c.get(Calendar.YEAR);
                    editTextDate.setText(day + "/" + month + "/" + year);
                }

                isTaskCompleted = task.isCompleted();
                buttonSave.setText(R.string.button_update);
            } else {
                Toast.makeText(getContext(), "Error loading task.", Toast.LENGTH_SHORT).show();
                navController.popBackStack();
            }
        });
    }

    private void saveTask() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate == 0) {
            Toast.makeText(getContext(), "Please select a due date", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task(title, description, isTaskCompleted, dueDate);

        if (taskId == -1) {
            // INSERT
            taskViewModel.insert(task);
            Toast.makeText(getContext(), "Task Added!", Toast.LENGTH_SHORT).show();
        } else {
            // UPDATE
            task.setId(taskId);
            taskViewModel.update(task);
            Toast.makeText(getContext(), "Task Updated!", Toast.LENGTH_SHORT).show();
        }

        navController.popBackStack();
    }
}
