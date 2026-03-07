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
public final class ReadPostUseCase_Factory implements Factory<ReadPostUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  public ReadPostUseCase_Factory(Provider<PostRepository> postRepositoryProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
  }

  @Override
  public ReadPostUseCase get() {
    return newInstance(postRepositoryProvider.get());
  }

  public static ReadPostUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider) {
    return new ReadPostUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider));
  }

  public static ReadPostUseCase_Factory create(Provider<PostRepository> postRepositoryProvider) {
    return new ReadPostUseCase_Factory(postRepositoryProvider);
  }

  public static ReadPostUseCase newInstance(PostRepository postRepository) {
    return new ReadPostUseCase(postRepository);
  }
}
