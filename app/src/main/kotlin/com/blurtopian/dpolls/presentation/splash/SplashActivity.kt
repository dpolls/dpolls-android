package com.blurtopian.dpolls.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
        
        // Keep splash screen visible while loading
        splashScreen.setKeepOnScreenCondition { true }
        
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

