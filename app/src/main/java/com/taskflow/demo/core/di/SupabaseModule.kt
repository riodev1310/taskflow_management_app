package com.taskflow.demo.core.di

import com.taskflow.demo.BuildConfig
import com.taskflow.demo.core.config.DataSourceConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        if (!DataSourceConfig.isLocal) {
            require(BuildConfig.SUPABASE_URL.isNotBlank()) {
                "SUPABASE_URL is empty. Add it in local.properties"
            }
            require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
                "SUPABASE_ANON_KEY is empty. Add it in local.properties"
            }
        }

        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://example.supabase.co" },
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "local-mode-placeholder-key" }
        ) {
            install(Postgrest)
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "taskflow"
                host = "login"
            }
            install(Storage)
            install(Realtime)
        }
    }
}
