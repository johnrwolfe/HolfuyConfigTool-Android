package com.holfuy.configtool.firmware

import java.time.Instant

data class RepositoryStatus(
    val configured: Boolean = false,
    val displayName: String? = null,
    val configuring: Boolean = false,
    val refreshing: Boolean = false,
    val lastSuccessfullyChecked: Instant? = null,
    val lastCheckFailed: Instant? = null,
    val firmware: List<FirmwareStatus> = emptyList()
)