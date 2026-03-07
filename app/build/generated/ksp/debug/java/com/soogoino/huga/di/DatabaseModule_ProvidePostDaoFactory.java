package com.soogoino.huga.di;

import com.soogoino.huga.data.local.AppDatabase;
import com.soogoino.huga.data.local.PostDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvidePostDaoFactory implements Factory<PostDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvidePostDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PostDao get() {
    return providePostDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePostDaoFactory create(
      javax.inject.Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvidePostDaoFactory(Providers.asDaggerProvider(dbProvider));
  }

  public static DatabaseModule_ProvidePostDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvidePostDaoFactory(dbProvider);
  }

  public static PostDao providePostDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePostDao(db));
  }
}
