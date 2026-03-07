package com.soogoino.huga.di;

import com.soogoino.huga.data.local.AppDatabase;
import com.soogoino.huga.data.local.DraftDao;
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
public final class DatabaseModule_ProvideDraftDaoFactory implements Factory<DraftDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideDraftDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DraftDao get() {
    return provideDraftDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDraftDaoFactory create(
      javax.inject.Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideDraftDaoFactory(Providers.asDaggerProvider(dbProvider));
  }

  public static DatabaseModule_ProvideDraftDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideDraftDaoFactory(dbProvider);
  }

  public static DraftDao provideDraftDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDraftDao(db));
  }
}
