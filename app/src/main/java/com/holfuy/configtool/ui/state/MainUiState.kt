package com.holfuy.configtool.ui.state

import com.holfuy.configtool.firmware.FirmwareFile

enum class FirmwareSelectionSource
{
    REPOSITORY,
    BROWSE
}

data class MainUiState(
    val connecting: Boolean = false,
    val selectedFirmware: FirmwareFile? = null,
    val selectedFirmwareSource: FirmwareSelectionSource? = null,
    val errorMessage: String? = null,
    val updateCompleted: Boolean = false
)