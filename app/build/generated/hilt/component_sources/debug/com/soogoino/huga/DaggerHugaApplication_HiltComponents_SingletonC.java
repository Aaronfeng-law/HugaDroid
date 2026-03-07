package com.soogoino.huga;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.soogoino.huga.data.local.AppDatabase;
import com.soogoino.huga.data.local.CommitDao;
import com.soogoino.huga.data.local.DraftDao;
import com.soogoino.huga.data.local.PostDao;
import com.soogoino.huga.data.prefs.AppPreferences;
import com.soogoino.huga.data.repository.PostRepository;
import com.soogoino.huga.data.repository.SecureTokenStore;
import com.soogoino.huga.di.DatabaseModule_ProvideCommitDaoFactory;
import com.soogoino.huga.di.DatabaseModule_ProvideDatabaseFactory;
import com.soogoino.huga.di.DatabaseModule_ProvideDraftDaoFactory;
import com.soogoino.huga.di.DatabaseModule_ProvidePostDaoFactory;
import com.soogoino.huga.domain.AutoSaveUseCase;
import com.soogoino.huga.domain.CreatePostUseCase;
import com.soogoino.huga.domain.DeletePostUseCase;
import com.soogoino.huga.domain.ObservePostsUseCase;
import com.soogoino.huga.domain.ReadPostUseCase;
import com.soogoino.huga.domain.SavePostUseCase;
import com.soogoino.huga.domain.ScanPostsUseCase;
import com.soogoino.huga.domain.SyncRepoUseCase;
import com.soogoino.huga.git.JGitRepositoryImpl;
import com.soogoino.huga.git.SshKeyManager;
import com.soogoino.huga.ui.editor.EditorViewModel;
import com.soogoino.huga.ui.editor.EditorViewModel_HiltModules;
import com.soogoino.huga.ui.editor.EditorViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.editor.EditorViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.ui.files.FilesViewModel;
import com.soogoino.huga.ui.files.FilesViewModel_HiltModules;
import com.soogoino.huga.ui.files.FilesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.files.FilesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.ui.posts.PostsViewModel;
import com.soogoino.huga.ui.posts.PostsViewModel_HiltModules;
import com.soogoino.huga.ui.posts.PostsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.posts.PostsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.ui.settings.SettingsViewModel;
import com.soogoino.huga.ui.settings.SettingsViewModel_HiltModules;
import com.soogoino.huga.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.ui.sync.SetupViewModel;
import com.soogoino.huga.ui.sync.SetupViewModel_HiltModules;
import com.soogoino.huga.ui.sync.SetupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.sync.SetupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.ui.sync.SyncViewModel;
import com.soogoino.huga.ui.sync.SyncViewModel_HiltModules;
import com.soogoino.huga.ui.sync.SyncViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.soogoino.huga.ui.sync.SyncViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.soogoino.huga.worker.GitSyncWorker;
import com.soogoino.huga.worker.GitSyncWorker_AssistedFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerHugaApplication_HiltComponents_SingletonC {
  private DaggerHugaApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public HugaApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements HugaApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements HugaApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements HugaApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements HugaApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements HugaApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements HugaApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements HugaApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public HugaApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends HugaApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends HugaApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends HugaApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends HugaApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(EditorViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, EditorViewModel_HiltModules.KeyModule.provide()).put(FilesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, FilesViewModel_HiltModules.KeyModule.provide()).put(PostsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PostsViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(SetupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SetupViewModel_HiltModules.KeyModule.provide()).put(SyncViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SyncViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectPrefs(instance, singletonCImpl.appPreferencesProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends HugaApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<EditorViewModel> editorViewModelProvider;

    private Provider<FilesViewModel> filesViewModelProvider;

    private Provider<PostsViewModel> postsViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SetupViewModel> setupViewModelProvider;

    private Provider<SyncViewModel> syncViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private ReadPostUseCase readPostUseCase() {
      return new ReadPostUseCase(singletonCImpl.postRepositoryProvider.get());
    }

    private SavePostUseCase savePostUseCase() {
      return new SavePostUseCase(singletonCImpl.postRepositoryProvider.get());
    }

    private AutoSaveUseCase autoSaveUseCase() {
      return new AutoSaveUseCase(singletonCImpl.postRepositoryProvider.get());
    }

    private ObservePostsUseCase observePostsUseCase() {
      return new ObservePostsUseCase(singletonCImpl.postRepositoryProvider.get());
    }

    private ScanPostsUseCase scanPostsUseCase() {
      return new ScanPostsUseCase(singletonCImpl.postRepositoryProvider.get(), singletonCImpl.appPreferencesProvider.get());
    }

    private CreatePostUseCase createPostUseCase() {
      return new CreatePostUseCase(singletonCImpl.postRepositoryProvider.get(), singletonCImpl.appPreferencesProvider.get());
    }

    private DeletePostUseCase deletePostUseCase() {
      return new DeletePostUseCase(singletonCImpl.postRepositoryProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.editorViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.filesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.postsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.setupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.syncViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(EditorViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) editorViewModelProvider)).put(FilesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) filesViewModelProvider)).put(PostsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) postsViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(SetupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) setupViewModelProvider)).put(SyncViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) syncViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.soogoino.huga.ui.editor.EditorViewModel 
          return (T) new EditorViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), viewModelCImpl.readPostUseCase(), viewModelCImpl.savePostUseCase(), viewModelCImpl.autoSaveUseCase(), singletonCImpl.appPreferencesProvider.get());

          case 1: // com.soogoino.huga.ui.files.FilesViewModel 
          return (T) new FilesViewModel(singletonCImpl.appPreferencesProvider.get());

          case 2: // com.soogoino.huga.ui.posts.PostsViewModel 
          return (T) new PostsViewModel(viewModelCImpl.observePostsUseCase(), viewModelCImpl.scanPostsUseCase(), viewModelCImpl.createPostUseCase(), viewModelCImpl.deletePostUseCase(), singletonCImpl.syncRepoUseCase(), singletonCImpl.jGitRepositoryImplProvider.get(), singletonCImpl.appPreferencesProvider.get());

          case 3: // com.soogoino.huga.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.appPreferencesProvider.get());

          case 4: // com.soogoino.huga.ui.sync.SetupViewModel 
          return (T) new SetupViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.appPreferencesProvider.get(), singletonCImpl.jGitRepositoryImplProvider.get(), singletonCImpl.sshKeyManagerProvider.get(), singletonCImpl.secureTokenStoreProvider.get());

          case 5: // com.soogoino.huga.ui.sync.SyncViewModel 
          return (T) new SyncViewModel(singletonCImpl.syncRepoUseCase(), viewModelCImpl.scanPostsUseCase(), singletonCImpl.jGitRepositoryImplProvider.get(), singletonCImpl.commitDao(), singletonCImpl.appPreferencesProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends HugaApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends HugaApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends HugaApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<JGitRepositoryImpl> jGitRepositoryImplProvider;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<PostRepository> postRepositoryProvider;

    private Provider<AppPreferences> appPreferencesProvider;

    private Provider<GitSyncWorker_AssistedFactory> gitSyncWorker_AssistedFactoryProvider;

    private Provider<SshKeyManager> sshKeyManagerProvider;

    private Provider<SecureTokenStore> secureTokenStoreProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private PostDao postDao() {
      return DatabaseModule_ProvidePostDaoFactory.providePostDao(provideDatabaseProvider.get());
    }

    private DraftDao draftDao() {
      return DatabaseModule_ProvideDraftDaoFactory.provideDraftDao(provideDatabaseProvider.get());
    }

    private SyncRepoUseCase syncRepoUseCase() {
      return new SyncRepoUseCase(jGitRepositoryImplProvider.get(), postRepositoryProvider.get(), appPreferencesProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return Collections.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>singletonMap("com.soogoino.huga.worker.GitSyncWorker", ((Provider) gitSyncWorker_AssistedFactoryProvider));
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    private CommitDao commitDao() {
      return DatabaseModule_ProvideCommitDaoFactory.provideCommitDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.jGitRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<JGitRepositoryImpl>(singletonCImpl, 1));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 3));
      this.postRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PostRepository>(singletonCImpl, 2));
      this.appPreferencesProvider = DoubleCheck.provider(new SwitchingProvider<AppPreferences>(singletonCImpl, 4));
      this.gitSyncWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<GitSyncWorker_AssistedFactory>(singletonCImpl, 0));
      this.sshKeyManagerProvider = DoubleCheck.provider(new SwitchingProvider<SshKeyManager>(singletonCImpl, 5));
      this.secureTokenStoreProvider = DoubleCheck.provider(new SwitchingProvider<SecureTokenStore>(singletonCImpl, 6));
    }

    @Override
    public void injectHugaApplication(HugaApplication hugaApplication) {
      injectHugaApplication2(hugaApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private HugaApplication injectHugaApplication2(HugaApplication instance) {
      HugaApplication_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.soogoino.huga.worker.GitSyncWorker_AssistedFactory 
          return (T) new GitSyncWorker_AssistedFactory() {
            @Override
            public GitSyncWorker create(Context appContext, WorkerParameters workerParams) {
              return new GitSyncWorker(appContext, workerParams, singletonCImpl.syncRepoUseCase());
            }
          };

          case 1: // com.soogoino.huga.git.JGitRepositoryImpl 
          return (T) new JGitRepositoryImpl();

          case 2: // com.soogoino.huga.data.repository.PostRepository 
          return (T) new PostRepository(singletonCImpl.postDao(), singletonCImpl.draftDao());

          case 3: // com.soogoino.huga.data.local.AppDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 4: // com.soogoino.huga.data.prefs.AppPreferences 
          return (T) new AppPreferences(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.soogoino.huga.git.SshKeyManager 
          return (T) new SshKeyManager();

          case 6: // com.soogoino.huga.data.repository.SecureTokenStore 
          return (T) new SecureTokenStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
