package com.hadat.stickman.ui.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.hadat.stickman.R
import com.hadat.stickman.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "Inflating layout...")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "Layout inflated successfully")

            val navController = findNavController(R.id.nav_host_fragment_content_main)
            Log.d(TAG, "NavController found: ${navController.currentDestination?.label}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            Log.d(TAG, "Navigate up from ${navController.currentDestination?.label}")
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onSupportNavigateUp: ${e.message}", e)
            super.onSupportNavigateUp()
        }
    }
}
