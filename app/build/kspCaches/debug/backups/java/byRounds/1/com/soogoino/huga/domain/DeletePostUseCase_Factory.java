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
public final class DeletePostUseCase_Factory implements Factory<DeletePostUseCase> {
  private final Provider<PostRepository> postRepositoryProvider;

  public DeletePostUseCase_Factory(Provider<PostRepository> postRepositoryProvider) {
    this.postRepositoryProvider = postRepositoryProvider;
  }

  @Override
  public DeletePostUseCase get() {
    return newInstance(postRepositoryProvider.get());
  }

  public static DeletePostUseCase_Factory create(
      javax.inject.Provider<PostRepository> postRepositoryProvider) {
    return new DeletePostUseCase_Factory(Providers.asDaggerProvider(postRepositoryProvider));
  }

  public static DeletePostUseCase_Factory create(Provider<PostRepository> postRepositoryProvider) {
    return new DeletePostUseCase_Factory(postRepositoryProvider);
  }

  public static DeletePostUseCase newInstance(PostRepository postRepository) {
    return new DeletePostUseCase(postRepository);
  }
}
