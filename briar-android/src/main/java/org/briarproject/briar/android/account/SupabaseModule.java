package org.briarproject.briar.android.account;

import dagger.Module;
import dagger.Provides;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.SupabaseClientKt;
import io.github.jan.supabase.gotrue.Auth;
import io.github.jan.supabase.compose.auth.ComposeAuth;
import io.github.jan.supabase.postgrest.Postgrest;
import org.briarproject.briar.BuildConfig;

import javax.inject.Singleton;

@Module
public class SupabaseModule {

    @Provides
    @Singleton
    static SupabaseClient provideSupabaseClient() {
        return SupabaseClientKt.createSupabaseClient(
                BuildConfig.SUPABASE_URL,
                BuildConfig.SUPABASE_ANON_KEY,
                builder -> {
                    builder.install(Auth.Companion, authConfig -> {});
                    builder.install(Postgrest.Companion, postgrestConfig -> {});
                    builder.install(ComposeAuth.Companion, composeAuthConfig -> {});
                    return null;
                }
        );
    }
}
