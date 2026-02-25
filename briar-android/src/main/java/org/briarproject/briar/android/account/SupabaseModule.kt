package org.briarproject.briar.android.account

import dagger.Module
import dagger.Provides
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.postgrest.Postgrest
import org.briarproject.briar.BuildConfig
import javax.inject.Singleton

@Module
class SupabaseModule {

    companion object {
        @Provides
        @Singleton
        @JvmStatic
        fun provideSupabaseClient(): SupabaseClient {
            return createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(ComposeAuth)
            }
        }
    }
}

