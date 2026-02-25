package org.briarproject.briar.android.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.briarproject.briar.BuildConfig;
import org.briarproject.briar.R;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.util.logging.Logger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static android.content.Intent.ACTION_VIEW;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.briar.android.util.UiUtils.tryToStartActivity;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class AboutFragment extends Fragment {

	final static String TAG = AboutFragment.class.getName();
	private static final Logger LOG = getLogger(TAG);

	private TextView nasakaWeweVersion;
	private TextView torVersion;
	private TextView nasakaWeweWebsite;
	private TextView nasakaWeweSourceCode;
	private TextView nasakaWeweChangelog;
	private TextView nasakaWewePrivacyPolicy;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_about, container,
				false);
	}

	@Override
	public void onStart() {
		super.onStart();
		requireActivity().setTitle(R.string.about_title);
		nasakaWeweVersion = requireActivity().findViewById(R.id.nasakaWeweVersion);
		nasakaWeweVersion.setText(
				getString(R.string.nasaka_wewe_version, BuildConfig.VERSION_NAME));
		torVersion = requireActivity().findViewById(R.id.torVersion);
		torVersion.setText(
				getString(R.string.tor_version, BuildConfig.TorVersion));
		nasakaWeweWebsite = requireActivity().findViewById(R.id.nasakaWeweWebsite);
		nasakaWeweSourceCode = requireActivity().findViewById(R.id.nasakaWeweSourceCode);
		nasakaWeweChangelog = requireActivity().findViewById(R.id.nasakaWeweChangelog);
		nasakaWewePrivacyPolicy =
				requireActivity().findViewById(R.id.nasakaWewePrivacyPolicy);
		nasakaWeweWebsite.setOnClickListener(View -> {
			String url = "https://www.civiceducationkenya.com/";
			goToUrl(url);
		});
		nasakaWeweSourceCode.setOnClickListener(View -> {
			String url = "https://www.civiceducationkenya.com/source";
			goToUrl(url);
		});
		nasakaWeweChangelog.setOnClickListener(View -> {
			String url =
					"https://www.civiceducationkenya.com/changelog";
			goToUrl(url);
		});
		nasakaWewePrivacyPolicy.setOnClickListener(View -> {
			String url =
					"https://www.civiceducationkenya.com/privacy-policy";
			goToUrl(url);
		});
	}

	private void goToUrl(String url) {
		Intent i = new Intent(ACTION_VIEW);
		i.setData(Uri.parse(url));
		tryToStartActivity(requireActivity(), i);
	}

}