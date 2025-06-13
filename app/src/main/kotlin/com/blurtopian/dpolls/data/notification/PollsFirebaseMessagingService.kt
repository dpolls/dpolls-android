package com.blurtopian.dpolls.data.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class PollsFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // TODO: Send this token to your server
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.data}")
        // TODO: Handle incoming messages
    }

    companion object {
        private const val TAG = "PollsFirebaseMsgService"
    }
} 