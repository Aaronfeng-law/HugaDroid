package com.soogoino.huga.git;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SshKeyManager_Factory implements Factory<SshKeyManager> {
  @Override
  public SshKeyManager get() {
    return newInstance();
  }

  public static SshKeyManager_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SshKeyManager newInstance() {
    return new SshKeyManager();
  }

  private static final class InstanceHolder {
    static final SshKeyManager_Factory INSTANCE = new SshKeyManager_Factory();
  }
}
