package com.holfuy.configtool.ui.state

import com.holfuy.configtool.firmware.RefreshResult

data class MainUiState(
    val connecting: Boolean = false,
    val firmwareFileName: String? = null,
    val firmwareSize: Int? = null,
    val errorMessage: String? = null,
    val updateCompleted: Boolean = false,
    val refreshResult: RefreshResult? = null
)