package com.zebra.spatialcomputingsample

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.zebra.SpatialComputingFragment.SpatialComputingFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.main_activity)
        val drawerLayout: DrawerLayout = findViewById(R.id.main_drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // set the app version name
        navView.getHeaderView(0)
            .findViewById<TextView>(R.id.nav_header_app_version_text_view).text =
            "V " + packageManager.getPackageInfo(packageName, 0).versionName
        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.generate_planogram, R.id.restore_planogram, R.id.restore_planogram),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = when (item.itemId) {
                R.id.basic_arcore -> SpatialComputingFragment()
                R.id.generate_planogram -> WorkflowFragment(getString(R.string.generate_planogram))
                R.id.restore_planogram -> WorkflowFragment(getString(R.string.restore_planogram))
                else -> {
                    TODO("Workflow not implemented")
                }
            }
            supportFragmentManager.popBackStack(
                R.id.nav_host_fragment,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, selectedFragment)
                    .commitNow()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }
        if (savedInstanceState == null) {
            var selectedFragment: Fragment =  WorkflowFragment(getString(R.string.generate_planogram))
            supportFragmentManager.popBackStack(
                R.id.nav_host_fragment,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            supportFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, selectedFragment)
                .commitNow()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}