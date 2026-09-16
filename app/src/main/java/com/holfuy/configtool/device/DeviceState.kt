package com.holfuy.configtool.device

data class DeviceState(
    val attached: Boolean = false,
    val permissionGranted: Boolean = false,
    val connected: Boolean = false,
    val updateInProgress: Boolean = false,
    val updateProgress: Int = 0,
    val updateCompleted: Boolean = false,
    val firmwareUpdateError: String? = null
)
