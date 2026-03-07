package com.soogoino.huga;

import com.soogoino.huga.data.prefs.AppPreferences;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AppPreferences> prefsProvider;

  public MainActivity_MembersInjector(Provider<AppPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<AppPreferences> prefsProvider) {
    return new MainActivity_MembersInjector(prefsProvider);
  }

  public static MembersInjector<MainActivity> create(
      javax.inject.Provider<AppPreferences> prefsProvider) {
    return new MainActivity_MembersInjector(Providers.asDaggerProvider(prefsProvider));
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPrefs(instance, prefsProvider.get());
  }

  @InjectedFieldSignature("com.soogoino.huga.MainActivity.prefs")
  public static void injectPrefs(MainActivity instance, AppPreferences prefs) {
    instance.prefs = prefs;
  }
}
