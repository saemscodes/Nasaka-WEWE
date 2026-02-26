package org.briarproject.briar.android.navdrawer;

import org.briarproject.briar.android.contact.ComposeContactListViewModel;
import org.briarproject.briar.android.util.ResourceManager;
import org.briarproject.briar.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class NavDrawerModule {

	@Binds
	@IntoMap
	@ViewModelKey(NavDrawerViewModel.class)
	abstract ViewModel bindNavDrawerViewModel(
			NavDrawerViewModel navDrawerViewModel);

	@Binds
	@IntoMap
	@ViewModelKey(PluginViewModel.class)
	abstract ViewModel bindPluginViewModel(PluginViewModel pluginViewModel);

	@Binds
	@IntoMap
	@ViewModelKey(ComposeContactListViewModel.class)
	abstract ViewModel bindComposeContactListViewModel(ComposeContactListViewModel viewModel);
}
