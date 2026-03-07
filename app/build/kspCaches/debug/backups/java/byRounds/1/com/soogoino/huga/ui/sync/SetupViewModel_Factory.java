package com.soogoino.huga.ui.sync;

import android.content.Context;
import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.data.repository.SecureTokenStore;
import com.soogoino.huga.git.GitRepository;
import com.soogoino.huga.git.SshKeyManager;
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
public final class SetupViewModel_Factory implements Factory<SetupViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<AppPreferences> prefsProvider;

  private final Provider<GitRepository> gitRepositoryProvider;

  private final Provider<SshKeyManager> sshKeyManagerProvider;

  private final Provider<SecureTokenStore> secureTokenStoreProvider;

  public SetupViewModel_Factory(Provider<Context> contextProvider,
      Provider<AppPreferences> prefsProvider, Provider<GitRepository> gitRepositoryProvider,
      Provider<SshKeyManager> sshKeyManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    this.contextProvider = contextProvider;
    this.prefsProvider = prefsProvider;
    this.gitRepositoryProvider = gitRepositoryProvider;
    this.sshKeyManagerProvider = sshKeyManagerProvider;
    this.secureTokenStoreProvider = secureTokenStoreProvider;
  }

  @Override
  public SetupViewModel get() {
    return newInstance(contextProvider.get(), prefsProvider.get(), gitRepositoryProvider.get(), sshKeyManagerProvider.get(), secureTokenStoreProvider.get());
  }

  public static SetupViewModel_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<AppPreferences> prefsProvider,
      javax.inject.Provider<GitRepository> gitRepositoryProvider,
      javax.inject.Provider<SshKeyManager> sshKeyManagerProvider,
      javax.inject.Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new SetupViewModel_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(prefsProvider), Providers.asDaggerProvider(gitRepositoryProvider), Providers.asDaggerProvider(sshKeyManagerProvider), Providers.asDaggerProvider(secureTokenStoreProvider));
  }

  public static SetupViewModel_Factory create(Provider<Context> contextProvider,
      Provider<AppPreferences> prefsProvider, Provider<GitRepository> gitRepositoryProvider,
      Provider<SshKeyManager> sshKeyManagerProvider,
      Provider<SecureTokenStore> secureTokenStoreProvider) {
    return new SetupViewModel_Factory(contextProvider, prefsProvider, gitRepositoryProvider, sshKeyManagerProvider, secureTokenStoreProvider);
  }

  public static SetupViewModel newInstance(Context context, AppPreferences prefs,
      GitRepository gitRepository, SshKeyManager sshKeyManager, SecureTokenStore secureTokenStore) {
    return new SetupViewModel(context, prefs, gitRepository, sshKeyManager, secureTokenStore);
  }
}
