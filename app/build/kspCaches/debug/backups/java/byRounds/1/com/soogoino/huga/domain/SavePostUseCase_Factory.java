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
public final class SavePostUseCase_Factory implements Factory<SavePostUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  public SavePostUseCase_Factory(Provider<PostRepository> postRepositoryProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
  }

  @Override
  public SavePostUseCase get() {
    return newInstance(postRepositoryProvider.get());
  }

  public static SavePostUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider) {
    return new SavePostUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider));
  }

  public static SavePostUseCase_Factory create(Provider<PostRepository> postRepositoryProvider) {
    return new SavePostUseCase_Factory(postRepositoryProvider);
  }

  public static SavePostUseCase newInstance(PostRepository postRepository) {
    return new SavePostUseCase(postRepository);
  }
}
