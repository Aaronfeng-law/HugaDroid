package com.soogoino.huga.ui.sync;

import com.soogoino.huga.data.local.CommitDao;
import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.domain.ScanPostsUseCase;
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
public final class SyncViewModel_Factory implements Factory<SyncViewModel> {
  private final Provider<SyncRepoUseCase> syncRepoUseCaseProvider;

  private final Provider<ScanPostsUseCase> scanPostsUseCaseProvider;

  private final Provider<GitRepository> gitRepositoryProvider;

  private final Provider<CommitDao> commitDaoProvider;

  private final Provider<AppPreferences> prefsProvider;

  public SyncViewModel_Factory(Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<CommitDao> commitDaoProvider,
      Provider<AppPreferences> prefsProvider) {
    this.syncRepoUseCaseProvider = syncRepoUseCaseProvider;
    this.scanPostsUseCaseProvider = scanPostsUseCaseProvider;
    this.gitRepositoryProvider = gitRepositoryProvider;
    this.commitDaoProvider = commitDaoProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public SyncViewModel get() {
    return newInstance(syncRepoUseCaseProvider.get(), scanPostsUseCaseProvider.get(), gitRepositoryProvider.get(), commitDaoProvider.get(), prefsProvider.get());
  }

  public static SyncViewModel_Factory create(
      javax.inject.Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      javax.inject.Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      javax.inject.Provider<GitRepository> gitRepositoryProvider,
      javax.inject.Provider<CommitDao> commitDaoProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new SyncViewModel_Factory(Providers.asDaggerProvider(syncRepoUseCaseProvider), Providers.asDaggerProvider(scanPostsUseCaseProvider), Providers.asDaggerProvider(gitRepositoryProvider), Providers.asDaggerProvider(commitDaoProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static SyncViewModel_Factory create(Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<CommitDao> commitDaoProvider,
      Provider<AppPreferences> prefsProvider) {
    return new SyncViewModel_Factory(syncRepoUseCaseProvider, scanPostsUseCaseProvider, gitRepositoryProvider, commitDaoProvider, prefsProvider);
  }

  public static SyncViewModel newInstance(SyncRepoUseCase syncRepoUseCase,
      ScanPostsUseCase scanPostsUseCase, GitRepository gitRepository, CommitDao commitDao,
      AppPreferences prefs) {
    return new SyncViewModel(syncRepoUseCase, scanPostsUseCase, gitRepository, commitDao, prefs);
  }
}
