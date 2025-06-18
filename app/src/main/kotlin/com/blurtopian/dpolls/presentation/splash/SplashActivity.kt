package com.blurtopian.dpolls.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blurtopian.dpolls.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Set the layout for the splash screen content.
        // This will be shown *during* the splash screen period.
        //setContentView(R.layout.splash_screen_layout)

        // Keep the splash screen visible for this Activity
        // You can use this to customize the exit animation
        splashScreen.setKeepOnScreenCondition { true }


        // Enable edge-to-edge display (optional, for modern look)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
            )
        )

        // Simulate loading or navigate to your MainActivity
        // For a real app, you'd perform initialization here.
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000) // Adjust delay as needed
        
        // Initialize app and navigate to main activity
        lifecycleScope.launch {
            initializeApp()
            navigateToMain()
        }
    }
    
    private suspend fun initializeApp() {
        // Simulate initialization time
        delay(2000)
        
        // Perform any necessary initialization here:
        // - Check network connectivity
        // - Initialize blockchain connection
        // - Load user preferences
        // - Check authentication status
    }
    
    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

