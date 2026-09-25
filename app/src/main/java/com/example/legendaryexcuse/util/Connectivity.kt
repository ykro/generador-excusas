package com.example.legendaryexcuse.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Exposes "is there working internet?" (a validated network) as a StateFlow, updated by a NetworkCallback. */
class Connectivity(context: Context) {
  private val manager = context.getSystemService(ConnectivityManager::class.java)
  private val _isOnline = MutableStateFlow(currentlyOnline())
  val isOnline: StateFlow<Boolean> = _isOnline

  init {
    manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
      override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
        _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      }
      override fun onLost(network: Network) {
        _isOnline.value = false
      }
    })
  }

  private fun currentlyOnline(): Boolean =
    manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
}
