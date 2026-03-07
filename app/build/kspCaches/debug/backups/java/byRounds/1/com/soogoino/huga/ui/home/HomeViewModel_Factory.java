package com.soogoino.huga.ui.home;

import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.domain.ObservePostsUseCase;
import com.soogoino.huga.domain.SyncRepoUseCase;
import com.soogoino.huga.git.GitRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ObservePostsUseCase> observePostsUseCaseProvider;

  private final Provider<SyncRepoUseCase> syncRepoUseCaseProvider;

  private final Provider<GitRepository> gitRepositoryProvider;

  private final Provider<AppPreferences> prefsProvider;

  public HomeViewModel_Factory(Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    this.observePostsUseCaseProvider = observePostsUseCaseProvider;
    this.syncRepoUseCaseProvider = syncRepoUseCaseProvider;
    this.gitRepositoryProvider = gitRepositoryProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(observePostsUseCaseProvider.get(), syncRepoUseCaseProvider.get(), gitRepositoryProvider.get(), prefsProvider.get());
  }

  public static HomeViewModel_Factory create(
      javax.inject.Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      javax.inject.Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      javax.inject.Provider<GitRepository> gitRepositoryProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new HomeViewModel_Factory(Providers.asDaggerProvider(observePostsUseCaseProvider), Providers.asDaggerProvider(syncRepoUseCaseProvider), Providers.asDaggerProvider(gitRepositoryProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static HomeViewModel_Factory create(
      Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    return new HomeViewModel_Factory(observePostsUseCaseProvider, syncRepoUseCaseProvider, gitRepositoryProvider, prefsProvider);
  }

  public static HomeViewModel newInstance(ObservePostsUseCase observePostsUseCase,
      SyncRepoUseCase syncRepoUseCase, GitRepository gitRepository, AppPreferences prefs) {
    return new HomeViewModel(observePostsUseCase, syncRepoUseCase, gitRepository, prefs);
  }
}
