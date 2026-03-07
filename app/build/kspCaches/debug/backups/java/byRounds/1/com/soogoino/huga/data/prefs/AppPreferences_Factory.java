package com.soogoino.huga.data.prefs;

import android.content.Context;
import com.soogoino.huga.data.repository.SecureTokenStore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppPreferences_Factory implements Factory<AppPreferences> {
  private final Provider<Context> contextProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  public AppPreferences_Factory(Provider<Context> contextProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    this.contextProvider = contextProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
  }

  @Override
  public AppPreferences get() {
    return newInstance(contextProvider.get(), secureTokenStoreProvider.get());
  }

  public static AppPreferences_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new AppPreferences_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(secureTokenStoreProvider));
  }

  public static AppPreferences_Factory create(Provider<Context> contextProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new AppPreferences_Factory(contextProvider, secureTokenStoreProvider);
  }

  public static AppPreferences newInstance(Context context, SecureTokenStore secureTokenStore) {
    return new AppPreferences(context, secureTokenStore);
  }
}
