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
public final class ObservePostsUseCase_Factory implements Factory<ObservePostsUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  public ObservePostsUseCase_Factory(Provider<PostRepository> postRepositoryProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
  }

  @Override
  public ObservePostsUseCase get() {
    return newInstance(postRepositoryProvider.get());
  }

  public static ObservePostsUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider) {
    return new ObservePostsUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider));
  }

  public static ObservePostsUseCase_Factory create(
      Provider<PostRepository> postRepositoryProvider) {
    return new ObservePostsUseCase_Factory(postRepositoryProvider);
  }

  public static ObservePostsUseCase newInstance(PostRepository postRepository) {
    return new ObservePostsUseCase(postRepository);
  }
}
