package org.briarproject.briar.android.account

import android.app.Application
import android.util.Log
import dagger.Module
import dagger.Provides
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import org.briarproject.briar.BuildConfig
import javax.inject.Singleton

@Module
class SupabaseModule {

    private val TAG = "NUCLEAR_DEBUG_SUPABASE"

    @Provides
    @Singleton
    fun provideSupabaseClient(app: Application): SupabaseClient {
        Log.i(TAG, "Initializing SupabaseClient (Nuclear Fail-safe v4 - Explicit Engine)")
        
        return try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                httpEngine = OkHttp.create()
                
                // Try to install Auth plugins one by one
                try {
                    install(Auth) {
                        scheme = "nasakawewe"
                        host = "auth-callback"
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "NUCLEAR: Failed to install Auth plugin", t)
                }
                
                try {
                    install(Postgrest)
                } catch (t: Throwable) {
                    Log.e(TAG, "NUCLEAR: Failed to install Postgrest plugin", t)
                }
            }
        } catch (t: Throwable) {
            val diag = "DIAGNOSTIC: Supabase creation failed! Type: ${t.javaClass.name}, Message: ${t.message}"
            Log.e(TAG, diag, t)
            
            // EMERGENCY FALLBACK
            try {
                Log.i(TAG, "Attempting fallback creation without engine...")
                createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                ) {}
            } catch (t2: Throwable) {
                val diag2 = "DIAGNOSTIC: Ultimate fallback failed! Type: ${t2.javaClass.name}, Message: ${t2.message}"
                Log.e(TAG, diag2, t2)
                throw t
            }
        }
    }
}
