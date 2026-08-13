package com.holfuy.configtool.ui.state

import com.holfuy.configtool.firmware.FirmwareFile

data class MainUiState(
    val connecting: Boolean = false,
    val selectedFirmware: FirmwareFile? = null,
    val errorMessage: String? = null,
    val updateCompleted: Boolean = false
)