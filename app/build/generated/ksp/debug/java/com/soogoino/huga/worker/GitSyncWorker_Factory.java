package com.soogoino.huga.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.soogoino.huga.domain.SyncRepoUseCase;
import dagger.internal.DaggerGenerated;
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
public final class GitSyncWorker_Factory {
  private final Provider<SyncRepoUseCase> syncRepoUseCaseProvider;

  public GitSyncWorker_Factory(Provider<SyncRepoUseCase> syncRepoUseCaseProvider) {
    this.syncRepoUseCaseProvider = syncRepoUseCaseProvider;
  }

  public GitSyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, syncRepoUseCaseProvider.get());
  }

  public static GitSyncWorker_Factory create(
      javax.inject.Provider<SyncRepoUseCase> syncRepoUseCaseProvider) {
    return new GitSyncWorker_Factory(Providers.asDaggerProvider(syncRepoUseCaseProvider));
  }

  public static GitSyncWorker_Factory create(Provider<SyncRepoUseCase> syncRepoUseCaseProvider) {
    return new GitSyncWorker_Factory(syncRepoUseCaseProvider);
  }

  public static GitSyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      SyncRepoUseCase syncRepoUseCase) {
    return new GitSyncWorker(appContext, workerParams, syncRepoUseCase);
  }
}
