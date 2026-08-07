package com.holfuy.configtool.firmware

import java.time.Instant

data class RefreshResult(
    val updated: List<String>,
    val stale: List<String>,
    val unavailable: List<String>,
    val verifiedAt: Instant? = null
)