package com.example.legendaryexcuse

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

class LegendaryExcuseApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Firebase.initialize(this)
    // Debug builds use the App Check debug provider: register the token printed in logcat
    // ("DebugAppCheckProvider") in the Firebase console. Release builds would use Play Integrity.
    if (BuildConfig.DEBUG) {
      Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
  }
}
