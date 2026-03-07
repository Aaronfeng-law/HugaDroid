package com.soogoino.huga;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class HugaApplication_MembersInjector implements MembersInjector<HugaApplication> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public HugaApplication_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<HugaApplication> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HugaApplication_MembersInjector(workerFactoryProvider);
  }

  public static MembersInjector<HugaApplication> create(
      javax.inject.Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new HugaApplication_MembersInjector(Providers.asDaggerProvider(workerFactoryProvider));
  }

  @Override
  public void injectMembers(HugaApplication instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.soogoino.huga.HugaApplication.workerFactory")
  public static void injectWorkerFactory(HugaApplication instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
