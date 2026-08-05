package com.holfuy.configtool.firmware

data class RefreshResult(
    val updated: List<String>,
    val stale: List<String>,
    val unavailable: List<String>
)