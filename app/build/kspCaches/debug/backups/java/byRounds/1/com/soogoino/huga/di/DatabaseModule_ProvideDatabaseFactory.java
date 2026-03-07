package com.soogoino.huga.di;

import android.content.Context;
import com.soogoino.huga.data.local.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DatabaseModule_ProvideDatabaseFactory implements Factory<AppDatabase> {
  private final Provider<Context> ctxProvider;

  public DatabaseModule_ProvideDatabaseFactory(Provider<Context> ctxProvider) {
    this.ctxProvider = ctxProvider;
  }

  @Override
  public AppDatabase get() {
    return provideDatabase(ctxProvider.get());
  }

  public static DatabaseModule_ProvideDatabaseFactory create(
      javax.inject.Provider<Context> ctxProvider) {
    return new DatabaseModule_ProvideDatabaseFactory(Providers.asDaggerProvider(ctxProvider));
  }

  public static DatabaseModule_ProvideDatabaseFactory create(Provider<Context> ctxProvider) {
    return new DatabaseModule_ProvideDatabaseFactory(ctxProvider);
  }

  public static AppDatabase provideDatabase(Context ctx) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDatabase(ctx));
  }
}
