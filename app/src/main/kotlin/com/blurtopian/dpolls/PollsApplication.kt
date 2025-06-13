package com.blurtopian.dpolls

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PollsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
        initializeLogging()
        initializeFirebase()
    }
    
    private fun initializeLogging() {
        // Initialize logging framework if needed
    }
    
    private fun initializeFirebase() {
        // Firebase initialization is automatic with google-services plugin
    }
}

