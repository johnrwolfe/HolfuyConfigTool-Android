package com.holfuy.configtool.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DeviceRepository
{
    private val _state =
        MutableStateFlow(
            DeviceState()
        )

    val stateFlow: StateFlow<DeviceState> =
        _state.asStateFlow()

    var state: DeviceState
        get() = _state.value
        set(value)
        {
            _state.value = value
        }

    fun setAttached(
        attached: Boolean
    )
    {
        _state.update {
            it.copy(
                attached = attached
            )
        }
    }

    fun setPermissionGranted(
        granted: Boolean
    )
    {
        _state.update {
            it.copy(
                permissionGranted = granted
            )
        }
    }

    fun setConnected(
        connected: Boolean
    )
    {
        _state.update {
            it.copy(
                connected = connected
            )
        }
    }

    fun setUpdateInProgress(
        inProgress: Boolean
    )
    {
        _state.update {
            it.copy(
                updateInProgress = inProgress
            )
        }
    }

    fun setUpdateProgress(
        progress: Int
    )
    {
        _state.update {
            it.copy(
                updateProgress = progress
            )
        }
    }

    fun clearConnectionState()
    {
        state = state.copy(
            attached = false,
            permissionGranted = false,
            connected = false,
            updateInProgress = false,
            updateProgress = 0
        )
    }
}
