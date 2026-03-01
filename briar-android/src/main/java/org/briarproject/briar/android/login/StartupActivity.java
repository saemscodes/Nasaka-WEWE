package org.briarproject.briar.android.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.briarproject.bramble.api.crypto.DecryptionResult;
import org.briarproject.briar.R;
import org.briarproject.briar.android.BriarService;
import org.briarproject.briar.android.account.CekaAuthActivity;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener;
import org.briarproject.briar.android.login.StartupViewModel.State;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static org.briarproject.bramble.api.crypto.DecryptionResult.SUCCESS;
import static org.briarproject.briar.android.login.StartupViewModel.State.SIGNED_IN;
import static org.briarproject.briar.android.login.StartupViewModel.State.SIGNED_OUT;
import static org.briarproject.briar.android.login.StartupViewModel.State.STARTED;
import static org.briarproject.briar.android.login.StartupViewModel.State.STARTING;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class StartupActivity extends BaseActivity implements
		BaseFragmentListener {

	private static final String TAG = "StartupActivity";
	private static final int AUTO_SIGN_IN_TIMEOUT_MS = 5000;

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private StartupViewModel viewModel;
	private boolean cekaAutoSignInAttempted = false;
	private boolean passwordFragmentShown = false;
	private final Handler handler = new Handler(Looper.getMainLooper());

	/**
	 * Safety net: if auto-sign-in doesn't complete within the timeout,
	 * fall back to showing PasswordFragment so the user is never stuck
	 * on a blank screen.
	 */
	private final Runnable autoSignInTimeoutRunnable = () -> {
		Log.w(TAG, "Auto-sign-in timeout reached, falling back to PasswordFragment");
		showPasswordFragmentIfNeeded();
	};

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(StartupViewModel.class);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		Log.d(TAG, "onCreate started");
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

		setContentView(R.layout.activity_fragment_container);

		if (!viewModel.accountExists()) {
			Log.d(TAG, "No account exists, redirecting to CekaAuthActivity");
			viewModel.deleteAccount();
			onAccountDeleted();
			return;
		}

		// Observe password validation results to catch auto-sign-in failures
		viewModel.getPasswordValidated().observeEvent(this, result -> {
			Log.d(TAG, "Password validation result: " + result);
			if (result != SUCCESS) {
				Log.w(TAG, "Auto-sign-in failed (result=" + result +
						"), falling back to PasswordFragment");
				handler.removeCallbacks(autoSignInTimeoutRunnable);
				showPasswordFragmentIfNeeded();
			} else {
				Log.i(TAG, "Auto-sign-in succeeded!");
				handler.removeCallbacks(autoSignInTimeoutRunnable);
			}
		});

		viewModel.getAccountDeleted().observeEvent(this, deleted -> {
			if (deleted)
				onAccountDeleted();
		});
		viewModel.getState().observe(this, this::onStateChanged);
	}

	@Override
	public void onStart() {
		super.onStart();
		viewModel.clearSignInNotification();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		handler.removeCallbacks(autoSignInTimeoutRunnable);
	}

	@Override
	@SuppressLint("MissingSuperCall")
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	private void onStateChanged(State state) {
		Log.d(TAG, "onStateChanged: " + state);
		if (state == SIGNED_OUT) {
			// Try CEKA auto-sign-in before showing PasswordFragment
			if (!cekaAutoSignInAttempted) {
				cekaAutoSignInAttempted = true;
				if (attemptCekaAutoSignIn()) {
					Log.d(TAG, "CEKA auto-sign-in initiated, " +
							"waiting for result...");
					// Set a safety timeout so we never get stuck
					handler.postDelayed(autoSignInTimeoutRunnable,
							AUTO_SIGN_IN_TIMEOUT_MS);
					return;
				}
			}
			showPasswordFragmentIfNeeded();
		} else if (state == SIGNED_IN || state == STARTING) {
			handler.removeCallbacks(autoSignInTimeoutRunnable);
			startService(new Intent(this, BriarService.class));
			showNextFragment(new OpenDatabaseFragment());
		} else if (state == STARTED) {
			handler.removeCallbacks(autoSignInTimeoutRunnable);
			setResult(RESULT_OK);
			supportFinishAfterTransition();
			overridePendingTransition(R.anim.screen_new_in,
					R.anim.screen_old_out);
		}
	}

	/**
	 * Shows the PasswordFragment if it hasn't been shown yet.
	 * This ensures the user always has a way to interact with the app,
	 * even if auto-sign-in fails.
	 */
	private void showPasswordFragmentIfNeeded() {
		if (!passwordFragmentShown) {
			passwordFragmentShown = true;
			Log.d(TAG, "Showing PasswordFragment");
			showInitialFragment(new PasswordFragment());
		}
	}

	/**
	 * Attempts to automatically sign in using cached CEKA (Supabase)
	 * credentials. When a user authenticates via Supabase, the Supabase
	 * userId is used as the Briar password. This method reads that cached
	 * userId and uses it to decrypt the database, bypassing the manual
	 * password entry screen entirely.
	 *
	 * @return true if auto-sign-in was attempted, false if no cached
	 *         credentials were available.
	 */
	private boolean attemptCekaAutoSignIn() {
		try {
			SharedPreferences prefs = getSharedPreferences("ceka_auth", MODE_PRIVATE);
			boolean isCekaMember = prefs.getBoolean("is_ceka_member", false);
			String cachedUserId = prefs.getString("cached_user_id", null);

			Log.d(TAG, "CEKA prefs: isCekaMember=" + isCekaMember +
					", hasUserId=" + (cachedUserId != null &&
							!cachedUserId.isEmpty()));

			if (isCekaMember && cachedUserId != null &&
					!cachedUserId.isEmpty()) {
				Log.i(TAG, "CEKA member detected, auto-signing in");
				viewModel.validatePassword(cachedUserId);
				return true;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error attempting CEKA auto-sign-in", e);
		}
		Log.d(TAG, "No CEKA credentials found");
		return false;
	}

	private void onAccountDeleted() {
		setResult(RESULT_CANCELED);
		finish();
		Intent i = new Intent(this, CekaAuthActivity.class);
		i.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP |
				FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_TASK_ON_HOME);
		startActivity(i);
	}

	@Override
	public void runOnDbThread(Runnable runnable) {
		throw new UnsupportedOperationException();
	}

}
