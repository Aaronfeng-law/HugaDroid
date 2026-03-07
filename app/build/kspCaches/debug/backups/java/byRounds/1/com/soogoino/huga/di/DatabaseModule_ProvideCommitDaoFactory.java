package com.soogoino.huga.di;

import com.soogoino.huga.data.local.AppDatabase;
import com.soogoino.huga.data.local.CommitDao;
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
public final class DatabaseModule_ProvideCommitDaoFactory implements Factory<CommitDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideCommitDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public CommitDao get() {
    return provideCommitDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideCommitDaoFactory create(
      javax.inject.Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideCommitDaoFactory(Providers.asDaggerProvider(dbProvider));
  }

  public static DatabaseModule_ProvideCommitDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideCommitDaoFactory(dbProvider);
  }

  public static CommitDao provideCommitDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCommitDao(db));
  }
}
