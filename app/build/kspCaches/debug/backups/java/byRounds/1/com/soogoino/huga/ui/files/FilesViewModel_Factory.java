package com.soogoino.huga.ui.files;

import com.soogoino.huga.data.prefs.AppPreferences;
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
public final class FilesViewModel_Factory implements Factory<FilesViewModel> {
  private final Provider<AppPreferences> prefsProvider;

  public FilesViewModel_Factory(Provider<AppPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public FilesViewModel get() {
    return newInstance(prefsProvider.get());
  }

  public static FilesViewModel_Factory create(javax.inject.Provider<AppPreferences> prefsProvider) {
    return new FilesViewModel_Factory(Providers.asDaggerProvider(prefsProvider));
  }

  public static FilesViewModel_Factory create(Provider<AppPreferences> prefsProvider) {
    return new FilesViewModel_Factory(prefsProvider);
  }

  public static FilesViewModel newInstance(AppPreferences prefs) {
    return new FilesViewModel(prefs);
  }
}
