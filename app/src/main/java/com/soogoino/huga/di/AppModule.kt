package com.soogoino.huga.di

import android.content.Context
import androidx.room.Room
import com.soogoino.huga.data.local.AppDatabase
import com.soogoino.huga.data.local.CommitDao
import com.soogoino.huga.data.local.DraftDao
import com.soogoino.huga.data.local.PostDao
import com.soogoino.huga.git.GitRepository
import com.soogoino.huga.git.JGitRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "huga_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePostDao(db: AppDatabase): PostDao = db.postDao()

    @Provides
    fun provideCommitDao(db: AppDatabase): CommitDao = db.commitDao()

    @Provides
    fun provideDraftDao(db: AppDatabase): DraftDao = db.draftDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GitModule {
    @Binds
    @Singleton
    abstract fun bindGitRepository(impl: JGitRepositoryImpl): GitRepository
}
