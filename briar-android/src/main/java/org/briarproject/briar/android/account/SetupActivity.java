package org.briarproject.briar.android.account;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;

import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;

import static org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY;
import static org.briarproject.briar.android.util.UiUtils.setInputStateHidden;

import androidx.activity.compose.ComponentActivityKt;
import androidx.compose.runtime.Composable;
import org.briarproject.briar.android.ui.screens.OnboardingScreensKt;
import org.briarproject.briar.android.ui.theme.ThemeKt;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetupActivity extends BaseActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	private SetupViewModel viewModel;

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(SetupViewModel.class);
		viewModel.getState().observeEvent(this, this::onStateChanged);
	}

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

		ComponentActivityKt.setContent(this, null, (composer, i) -> {
			OnboardingScreensKt.OnboardingFlow((name, password, role) -> {
				viewModel.createAccount(name, password != null ? password : "", (Integer) role);
				return null;
			}, composer, 0);
			return null;
		});
	}

	private void onStateChanged(SetupViewModel.State state) {
		if (state == SetupViewModel.State.CREATED || state == SetupViewModel.State.FAILED) {
			// TODO: Show an error if failed
			showApp();
		}
	}

	private void showPasswordFragment() {
		showNextFragment(SetPasswordFragment.newInstance());
	}

	@TargetApi(23)
	private void showDozeFragment() {
		showNextFragment(DozeFragment.newInstance());
	}

	private void showApp() {
		Intent i = new Intent(this, ENTRY_ACTIVITY);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME |
				Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		supportFinishAfterTransition();
		overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out);
	}

	@Override
	@Deprecated
	public void runOnDbThread(Runnable runnable) {
		throw new RuntimeException("Don't use this deprecated method here.");
	}

}
