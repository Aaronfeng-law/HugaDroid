package com.soogoino.huga.ui.posts;

import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.domain.CreatePostUseCase;
import com.soogoino.huga.domain.DeletePostUseCase;
import com.soogoino.huga.domain.ObservePostsUseCase;
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
public final class PostsViewModel_Factory implements Factory<PostsViewModel> {
  private final Provider<ObservePostsUseCase> observePostsUseCaseProvider;

  private final Provider<ScanPostsUseCase> scanPostsUseCaseProvider;

  private final Provider<CreatePostUseCase> createPostUseCaseProvider;

  private final Provider<DeletePostUseCase> deletePostUseCaseProvider;

  private final Provider<SyncRepoUseCase> syncRepoUseCaseProvider;

  private final Provider<GitRepository> gitRepositoryProvider;

  private final Provider<AppPreferences> prefsProvider;

  public PostsViewModel_Factory(Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      Provider<CreatePostUseCase> createPostUseCaseProvider,
      Provider<DeletePostUseCase> deletePostUseCaseProvider,
      Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    this.observePostsUseCaseProvider = observePostsUseCaseProvider;
    this.scanPostsUseCaseProvider = scanPostsUseCaseProvider;
    this.createPostUseCaseProvider = createPostUseCaseProvider;
    this.deletePostUseCaseProvider = deletePostUseCaseProvider;
    this.syncRepoUseCaseProvider = syncRepoUseCaseProvider;
    this.gitRepositoryProvider = gitRepositoryProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public PostsViewModel get() {
    return newInstance(observePostsUseCaseProvider.get(), scanPostsUseCaseProvider.get(), createPostUseCaseProvider.get(), deletePostUseCaseProvider.get(), syncRepoUseCaseProvider.get(), gitRepositoryProvider.get(), prefsProvider.get());
  }

  public static PostsViewModel_Factory create(
      javax.inject.Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      javax.inject.Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      javax.inject.Provider<CreatePostUseCase> createPostUseCaseProvider,
      javax.inject.Provider<DeletePostUseCase> deletePostUseCaseProvider,
      javax.inject.Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      javax.inject.Provider<GitRepository> gitRepositoryProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new PostsViewModel_Factory(Providers.asDaggerProvider(observePostsUseCaseProvider), Providers.asDaggerProvider(scanPostsUseCaseProvider), Providers.asDaggerProvider(createPostUseCaseProvider), Providers.asDaggerProvider(deletePostUseCaseProvider), Providers.asDaggerProvider(syncRepoUseCaseProvider), Providers.asDaggerProvider(gitRepositoryProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static PostsViewModel_Factory create(
      Provider<ObservePostsUseCase> observePostsUseCaseProvider,
      Provider<ScanPostsUseCase> scanPostsUseCaseProvider,
      Provider<CreatePostUseCase> createPostUseCaseProvider,
      Provider<DeletePostUseCase> deletePostUseCaseProvider,
      Provider<SyncRepoUseCase> syncRepoUseCaseProvider,
      Provider<GitRepository> gitRepositoryProvider, Provider<AppPreferences> prefsProvider) {
    return new PostsViewModel_Factory(observePostsUseCaseProvider, scanPostsUseCaseProvider, createPostUseCaseProvider, deletePostUseCaseProvider, syncRepoUseCaseProvider, gitRepositoryProvider, prefsProvider);
  }

  public static PostsViewModel newInstance(ObservePostsUseCase observePostsUseCase,
      ScanPostsUseCase scanPostsUseCase, CreatePostUseCase createPostUseCase,
      DeletePostUseCase deletePostUseCase, SyncRepoUseCase syncRepoUseCase,
      GitRepository gitRepository, AppPreferences prefs) {
    return new PostsViewModel(observePostsUseCase, scanPostsUseCase, createPostUseCase, deletePostUseCase, syncRepoUseCase, gitRepository, prefs);
  }
}
