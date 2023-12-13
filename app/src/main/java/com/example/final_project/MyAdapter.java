package com.example.final_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Define the MyAdapter class
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private List<Task> tasks;

    public MyAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override


    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for your item view
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_layout, parent, false);
        return

                new MyViewHolder(view);
    }

    @Override


    public void

    onBindViewHolder(MyViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Set the reminder text and location in the item view
        holder.reminderText.setText(task.getReminderText());
        holder.location.setText(task.getLocation());
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    // Define the MyViewHolder class
    public class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView reminderText;
        private TextView location;

        public MyViewHolder(View itemView) {
            super(itemView);

            reminderText = (TextView) itemView.findViewById(R.id.reminder_text);
            location = (TextView) itemView.findViewById(R.id.location);
        }
    }
}
