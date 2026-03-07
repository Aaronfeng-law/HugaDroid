package com.soogoino.huga.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class GitSyncWorker_AssistedFactory_Impl implements GitSyncWorker_AssistedFactory {
  private final GitSyncWorker_Factory delegateFactory;

  GitSyncWorker_AssistedFactory_Impl(GitSyncWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public GitSyncWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<GitSyncWorker_AssistedFactory> create(
      GitSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new GitSyncWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<GitSyncWorker_AssistedFactory> createFactoryProvider(
      GitSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new GitSyncWorker_AssistedFactory_Impl(delegateFactory));
  }
}
