package com.soogoino.hugadroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class, CommitEntity::class, DraftEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commitDao(): CommitDao
    abstract fun draftDao(): DraftDao
}
