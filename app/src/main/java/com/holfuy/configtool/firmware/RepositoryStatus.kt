package com.holfuy.configtool.firmware

import java.time.Instant

data class RepositoryStatus(
    val configured: Boolean = false,
    val displayName: String? = null,
    val configuring: Boolean = false,
    val refreshing: Boolean = false,
    val lastVerified: Instant? = null,
    val updated: List<String> = emptyList(),
    val stale: List<String> = emptyList(),
    val unavailable: List<String> = emptyList()
)