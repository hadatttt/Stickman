package com.hadat.stickman.ui.main

import android.os.Bundle
<<<<<<< HEAD
import android.view.Menu
import android.view.MenuItem
=======
import android.util.Log
import android.widget.Toast
>>>>>>> 6d571fd (feat : add ui)
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
<<<<<<< HEAD
import com.google.android.material.snackbar.Snackbar
=======
>>>>>>> 6d571fd (feat : add ui)
import com.hadat.stickman.R
import com.hadat.stickman.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

<<<<<<< HEAD
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
=======
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Gán Toolbar làm ActionBar
            setSupportActionBar(binding.toolbar)

            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_main) as androidx.navigation.fragment.NavHostFragment
            val navController = navHostFragment.navController

            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting MainActivity", e)
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
>>>>>>> 6d571fd (feat : add ui)
