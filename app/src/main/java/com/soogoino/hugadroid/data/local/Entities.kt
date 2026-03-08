package com.soogoino.hugadroid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached post metadata for fast list display. */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val filePath: String,
    val relativePath: String,
    val slug: String,
    val title: String,
    val date: String,
    val draft: Boolean,
    val tags: String,         // JSON-encoded list
    val categories: String,   // JSON-encoded list
    val description: String,
    val lastModified: Long,
    val wordCount: Int = 0,   // pre-computed CJK-aware word count
)

/** Commit log cache. */
@Entity(tableName = "commits")
data class CommitEntity(
    @PrimaryKey val hash: String,
    val shortHash: String,
    val message: String,
    val authorName: String,
    val authorEmail: String,
    val time: Long,
)

/** Draft auto-save state. */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val filePath: String,
    val content: String,       // full raw file content (front matter + body)
    val savedAt: Long,         // epoch ms
    val isDirty: Boolean,      // true = unsaved to git
)
