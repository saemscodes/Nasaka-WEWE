package org.briarproject.bramble.test;

import org.briarproject.bramble.api.system.ResourceConstraintManager;

import dagger.Module;
import dagger.Provides;

@Module
public class TestResourceConstraintModule {

    @Provides
    ResourceConstraintManager provideResourceConstraintManager() {
        return () -> false;
    }
}
