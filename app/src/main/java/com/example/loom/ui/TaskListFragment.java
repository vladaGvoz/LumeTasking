package com.example.loom.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loom.R;
import com.example.loom.model.Task;
import com.example.loom.viewmodel.TaskViewModel;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class TaskListFragment extends Fragment {

    private final static  String TAG = "TaskListFragment";

    private TaskViewModel taskViewModel;
    private TaskAdapter taskAdapter;
    private NavController navController;

    private TextView tvEmpty;
    private View selectionToolbar;
    private TextView tvSelectedCount;
    private FloatingActionButton fab;

    public TaskListFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        MobileAds.initialize(requireContext(), initializationStatus -> {});

        AdView adView = view.findViewById(R.id.ad_banner);
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        } else {
            Log.e(TAG, "AdView is null! Check your fragment layout.");
        }

        return view;



    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.task_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        selectionToolbar = view.findViewById(R.id.selectionToolbar);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
        View btnCancelSelection = view.findViewById(R.id.btnCancelSelection);
        View btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        fab = view.findViewById(R.id.fab_add_task);

        taskAdapter = new TaskAdapter();
        recyclerView.setAdapter(taskAdapter);

        tvEmpty = view.findViewById(R.id.tvEmpty);

        View rootLayout = view.findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(v -> {
            if (taskAdapter.isSelectionMode()) {
                taskAdapter.clearSelection();
            }
        });

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child == null && taskAdapter.isSelectionMode()) {
                        taskAdapter.clearSelection();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
        });

        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            taskAdapter.submitList(tasks);
            tvEmpty.setVisibility((tasks == null || tasks.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        taskAdapter.setOnItemClickListener(new TaskAdapter.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSelectionCountChanged(int count) {
                if (count > 0) {
                    selectionToolbar.setVisibility(View.VISIBLE);
                    tvSelectedCount.setText(count + " selected");
                    fab.hide();
                } else {
                    selectionToolbar.setVisibility(View.GONE);
                    fab.show();
                }
            }

            @Override
            public void onItemCheckChanged(Task task, boolean isChecked) {
                Task updated = new Task(task.getTitle(), task.getDescription(), isChecked, task.getDueDate());
                updated.setId(task.getId());
                taskViewModel.update(updated);
            }

            @Override
            public void onItemClick(Task task) {
                if (!taskAdapter.isSelectionMode()) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("taskId", task.getId());
                    navController.navigate(R.id.action_taskListFragment_to_taskDetailFragment, bundle);
                }
            }

            @Override
            public void onItemLongClick(Task task) { }
        });

        fab.setOnClickListener(v -> navController.navigate(R.id.action_taskListFragment_to_taskDetailFragment));

        btnCancelSelection.setOnClickListener(v -> taskAdapter.clearSelection());

        btnDeleteSelected.setOnClickListener(v -> {
            List<Task> deletedTasks = taskAdapter.getSelectedTasks();
            if (deletedTasks.isEmpty()) return;

            for (Task task : deletedTasks) {
                taskViewModel.deleteById(task.getId());
            }

            taskAdapter.clearSelection();

            Snackbar snackbar = Snackbar.make(view, "Deleted " + deletedTasks.size() + " tasks", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v1 -> {
                        for (Task t : deletedTasks) {
                            taskViewModel.insert(t);
                        }
                    });

            snackbar.setAnchorView(fab);

            View snackbarView = snackbar.getView();
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
            int extraMarginPx = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
            params.bottomMargin += extraMarginPx;
            snackbarView.setLayoutParams(params);

            snackbar.show();
        });

    }
}
