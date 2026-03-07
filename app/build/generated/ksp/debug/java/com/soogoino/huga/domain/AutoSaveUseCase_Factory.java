package com.soogoino.huga.domain;

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
public final class AutoSaveUseCase_Factory implements Factory<AutoSaveUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  public AutoSaveUseCase_Factory(Provider<PostRepository> postRepositoryProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
  }

  @Override
  public AutoSaveUseCase get() {
    return newInstance(postRepositoryProvider.get());
  }

  public static AutoSaveUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider) {
    return new AutoSaveUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider));
  }

  public static AutoSaveUseCase_Factory create(Provider<PostRepository> postRepositoryProvider) {
    return new AutoSaveUseCase_Factory(postRepositoryProvider);
  }

  public static AutoSaveUseCase newInstance(PostRepository postRepository) {
    return new AutoSaveUseCase(postRepository);
  }
}
