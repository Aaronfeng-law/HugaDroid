package com.soogoino.huga.ui.settings;

import android.content.Context;
import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.data.repository.SecureTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppPreferences> prefsProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppPreferences> prefsProvider, Provider<SecureTokenStore> secureTokenStoreProvider) {
    this.contextProvider = contextProvider;
    this.prefsProvider = prefsProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), prefsProvider.get(), secureTokenStoreProvider.get());
  }

  public static SettingsViewModel_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<AppPreferences> prefsProvider,
      javax.inject.Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(prefsProvider), Providers.asDaggerProvider(secureTokenStoreProvider));
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppPreferences> prefsProvider, Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new SettingsViewModel_Factory(contextProvider, prefsProvider, secureTokenStoreProvider);
  }

  public static SettingsViewModel newInstance(Context context, AppPreferences prefs,
      SecureTokenStore secureTokenStore) {
    return new SettingsViewModel(context, prefs, secureTokenStore);
  }
}
