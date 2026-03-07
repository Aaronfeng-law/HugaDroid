package com.soogoino.huga.data.repository;

import com.soogoino.huga.data.local.DraftDao;
import com.soogoino.huga.data.local.PostDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class PostRepository_Factory implements Factory<PostRepository> {
  private final Provider<PostDao> postDaoProvider;

  private final Provider<DraftDao> draftDaoProvider;

  public PostRepository_Factory(Provider<PostDao> postDaoProvider,
      Provider<DraftDao> draftDaoProvider) {
    this.postDaoProvider = postDaoProvider;
    this.draftDaoProvider = draftDaoProvider;
  }

  @Override
  public PostRepository get() {
    return newInstance(postDaoProvider.get(), draftDaoProvider.get());
  }

  public static PostRepository_Factory create(javax.inject.Provider<PostDao> postDaoProvider,
      javax.inject.Provider<DraftDao> draftDaoProvider) {
    return new PostRepository_Factory(Providers.asDaggerProvider(postDaoProvider), Providers.asDaggerProvider(draftDaoProvider));
  }

  public static PostRepository_Factory create(Provider<PostDao> postDaoProvider,
      Provider<DraftDao> draftDaoProvider) {
    return new PostRepository_Factory(postDaoProvider, draftDaoProvider);
  }

  public static PostRepository newInstance(PostDao postDao, DraftDao draftDao) {
    return new PostRepository(postDao, draftDao);
  }
}
