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
public final class JGitRepositoryImpl_Factory implements Factory<JGitRepositoryImpl> {
  @Override
  public JGitRepositoryImpl get() {
    return newInstance();
  }

  public static JGitRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static JGitRepositoryImpl newInstance() {
    return new JGitRepositoryImpl();
  }

  private static final class InstanceHolder {
    static final JGitRepositoryImpl_Factory INSTANCE = new JGitRepositoryImpl_Factory();
  }
}
