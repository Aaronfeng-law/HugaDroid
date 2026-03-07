package com.soogoino.huga.domain;

import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.data.repository.PostRepository;
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
public final class CreatePostUseCase_Factory implements Factory<CreatePostUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  private final Provider<AppPreferences> prefsProvider;

  public CreatePostUseCase_Factory(Provider<PostRepository> postRepositoryProvider,
      Provider<AppPreferences> prefsProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public CreatePostUseCase get() {
    return newInstance(postRepositoryProvider.get(), prefsProvider.get());
  }

  public static CreatePostUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new CreatePostUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static CreatePostUseCase_Factory create(Provider<PostRepository> postRepositoryProvider,
      Provider<AppPreferences> prefsProvider) {
    return new CreatePostUseCase_Factory(postRepositoryProvider, prefsProvider);
  }

  public static CreatePostUseCase newInstance(PostRepository postRepository, AppPreferences prefs) {
    return new CreatePostUseCase(postRepository, prefs);
  }
}
