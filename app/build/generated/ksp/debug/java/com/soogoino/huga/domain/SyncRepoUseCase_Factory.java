package com.soogoino.huga.domain;

import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.data.repository.PostRepository;
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
public final class SyncRepoUseCase_Factory implements Factory<SyncRepoUseCase> {
  private final Provider<GitRepository> gitRepositoryProvider;

  private final Provider<PostRepository> postRepositoryProvider;

  private final Provider<AppPreferences> prefsProvider;

  public SyncRepoUseCase_Factory(Provider<GitRepository> gitRepositoryProvider,
      Provider<PostRepository> postRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    this.gitRepositoryProvider = gitRepositoryProvider;
    this.postRepositoryProvider = postRepositoryProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public SyncRepoUseCase get() {
    return newInstance(gitRepositoryProvider.get(), postRepositoryProvider.get(), prefsProvider.get());
  }

  public static SyncRepoUseCase_Factory create(
      javax.inject.Provider<GitRepository> gitRepositoryProvider,
      javax.inject.Provider<PostRepository> postRepositoryProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new SyncRepoUseCase_Factory(Providers.asDaggerProvider(gitRepositoryProvider), Providers.asDaggerProvider(postRepositoryProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static SyncRepoUseCase_Factory create(Provider<GitRepository> gitRepositoryProvider,
      Provider<PostRepository> postRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    return new SyncRepoUseCase_Factory(gitRepositoryProvider, postRepositoryProvider, prefsProvider);
  }

  public static SyncRepoUseCase newInstance(GitRepository gitRepository,
      PostRepository postRepository, AppPreferences prefs) {
    return new SyncRepoUseCase(gitRepository, postRepository, prefs);
  }
}
