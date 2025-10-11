package com.example.capstone2.delivery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.example.capstone2.R
import com.example.capstone2.adapter.DeliveryTrackAdapter
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.repository.RequestRepository

class DeliveryTrackActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var deliveryTrackAdapter: DeliveryTrackAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_track)

        recyclerView = findViewById(R.id.recyclerViewDeliveryTrack)
        tvNoRequests = findViewById(R.id.tvNoRequestsDelivery)

        recyclerView.layoutManager = LinearLayoutManager(this)
        deliveryTrackAdapter = DeliveryTrackAdapter(emptyList(), { _ -> }, { _ -> })
        recyclerView.adapter = deliveryTrackAdapter

        tvNoRequests.visibility = View.VISIBLE


    }






}


