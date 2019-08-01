package com.example.androidmediapractice

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        val taskActivities = resources.getStringArray(R.array.task_activities)
        recyclerView.adapter = MainAdapter(resources.getStringArray(R.array.tasks).toList()) {
            startActivity(Intent(this, Class.forName(taskActivities[it])))
        }
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL))
    }
}
