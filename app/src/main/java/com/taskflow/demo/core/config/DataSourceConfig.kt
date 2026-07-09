package com.taskflow.demo.core.config

import com.taskflow.demo.BuildConfig

enum class DataSourceMode {
    Local,
    Supabase;

    companion object {
        fun from(raw: String): DataSourceMode {
            return when (raw.trim().lowercase()) {
                "supabase", "remote", "online" -> Supabase
                else -> Local
            }
        }
    }
}

object DataSourceConfig {
    val mode: DataSourceMode = DataSourceMode.from(BuildConfig.DATA_SOURCE_MODE)
    val isLocal: Boolean
        get() = mode == DataSourceMode.Local
}
