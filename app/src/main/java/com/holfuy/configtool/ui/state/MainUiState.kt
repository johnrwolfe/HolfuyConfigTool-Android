package com.holfuy.configtool.ui.state

import com.holfuy.configtool.firmware.FirmwareFile

enum class FirmwareSelectionSource
{
    REPOSITORY,
    BROWSE
}

data class SelectedFirmware(
    val file: FirmwareFile,
    val source: FirmwareSelectionSource,
    val modem: String? = null
)

data class MainUiState(
    val connecting: Boolean = false,
    val selectedFirmware: SelectedFirmware? = null,
    val firmwareSelectionError: String? = null,
    val errorMessage: String? = null,
    val updateCompleted: Boolean = false,
    val firmwareUpdateInterrupted: Boolean = false
)