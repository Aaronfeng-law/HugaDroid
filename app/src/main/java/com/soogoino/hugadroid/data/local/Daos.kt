package com.soogoino.hugadroid.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY date DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY date DESC")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE filePath = :filePath")
    suspend fun getByPath(filePath: String): PostEntity?

    @Upsert
    suspend fun upsertAll(posts: List<PostEntity>)

    @Upsert
    suspend fun upsert(post: PostEntity)

    @Query("DELETE FROM posts WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}

@Dao
interface CommitDao {
    @Query("SELECT * FROM commits ORDER BY time DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<CommitEntity>>

    @Upsert
    suspend fun upsertAll(commits: List<CommitEntity>)

    @Query("DELETE FROM commits")
    suspend fun deleteAll()
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE filePath = :filePath")
    suspend fun get(filePath: String): DraftEntity?

    @Upsert
    suspend fun upsert(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE filePath = :filePath")
    suspend fun delete(filePath: String)

    @Query("UPDATE drafts SET isDirty = 0 WHERE filePath = :filePath")
    suspend fun markClean(filePath: String)
}
