package com.soogoino.huga.ui.editor;

import android.content.Context;
import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.domain.AutoSaveUseCase;
import com.soogoino.huga.domain.ReadPostUseCase;
import com.soogoino.huga.domain.SavePostUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class EditorViewModel_Factory implements Factory<EditorViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<ReadPostUseCase> readPostUseCaseProvider;

  private final Provider<SavePostUseCase> savePostUseCaseProvider;

  private final Provider<AutoSaveUseCase> autoSaveUseCaseProvider;

  private final Provider<AppPreferences> prefsProvider;

  public EditorViewModel_Factory(Provider<Context> contextProvider,
      Provider<ReadPostUseCase> readPostUseCaseProvider,
      Provider<SavePostUseCase> savePostUseCaseProvider,
      Provider<AutoSaveUseCase> autoSaveUseCaseProvider, Provider<AppPreferences> prefsProvider) {
    this.contextProvider = contextProvider;
    this.readPostUseCaseProvider = readPostUseCaseProvider;
    this.savePostUseCaseProvider = savePostUseCaseProvider;
    this.autoSaveUseCaseProvider = autoSaveUseCaseProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public EditorViewModel get() {
    return newInstance(contextProvider.get(), readPostUseCaseProvider.get(), savePostUseCaseProvider.get(), autoSaveUseCaseProvider.get(), prefsProvider.get());
  }

  public static EditorViewModel_Factory create(javax.inject.Provider<Context> contextProvider,
      javax.inject.Provider<ReadPostUseCase> readPostUseCaseProvider,
      javax.inject.Provider<SavePostUseCase> savePostUseCaseProvider,
      javax.inject.Provider<AutoSaveUseCase> autoSaveUseCaseProvider,
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new EditorViewModel_Factory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(readPostUseCaseProvider), Providers.asDaggerProvider(savePostUseCaseProvider), Providers.asDaggerProvider(autoSaveUseCaseProvider), Providers.asDaggerProvider(prefsProvider));
  }

  public static EditorViewModel_Factory create(Provider<Context> contextProvider,
      Provider<ReadPostUseCase> readPostUseCaseProvider,
      Provider<SavePostUseCase> savePostUseCaseProvider,
      Provider<AutoSaveUseCase> autoSaveUseCaseProvider, Provider<AppPreferences> prefsProvider) {
    return new EditorViewModel_Factory(contextProvider, readPostUseCaseProvider, savePostUseCaseProvider, autoSaveUseCaseProvider, prefsProvider);
  }

  public static EditorViewModel newInstance(Context context, ReadPostUseCase readPostUseCase,
      SavePostUseCase savePostUseCase, AutoSaveUseCase autoSaveUseCase, AppPreferences prefs) {
    return new EditorViewModel(context, readPostUseCase, savePostUseCase, autoSaveUseCase, prefs);
  }
}
