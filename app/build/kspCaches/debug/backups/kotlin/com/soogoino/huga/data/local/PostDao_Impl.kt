package com.soogoino.huga.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PostDao_Impl(
  __db: RoomDatabase,
) : PostDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfPostEntity: EntityUpsertAdapter<PostEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfPostEntity = EntityUpsertAdapter<PostEntity>(object :
        EntityInsertAdapter<PostEntity>() {
      protected override fun createQuery(): String =
          "INSERT INTO `posts` (`filePath`,`relativePath`,`slug`,`title`,`date`,`draft`,`tags`,`categories`,`description`,`lastModified`,`wordCount`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PostEntity) {
        statement.bindText(1, entity.filePath)
        statement.bindText(2, entity.relativePath)
        statement.bindText(3, entity.slug)
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.date)
        val _tmp: Int = if (entity.draft) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.tags)
        statement.bindText(8, entity.categories)
        statement.bindText(9, entity.description)
        statement.bindLong(10, entity.lastModified)
        statement.bindLong(11, entity.wordCount.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<PostEntity>() {
      protected override fun createQuery(): String =
          "UPDATE `posts` SET `filePath` = ?,`relativePath` = ?,`slug` = ?,`title` = ?,`date` = ?,`draft` = ?,`tags` = ?,`categories` = ?,`description` = ?,`lastModified` = ?,`wordCount` = ? WHERE `filePath` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PostEntity) {
        statement.bindText(1, entity.filePath)
        statement.bindText(2, entity.relativePath)
        statement.bindText(3, entity.slug)
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.date)
        val _tmp: Int = if (entity.draft) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindText(7, entity.tags)
        statement.bindText(8, entity.categories)
        statement.bindText(9, entity.description)
        statement.bindLong(10, entity.lastModified)
        statement.bindLong(11, entity.wordCount.toLong())
        statement.bindText(12, entity.filePath)
      }
    })
  }

  public override suspend fun upsertAll(posts: List<PostEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __upsertAdapterOfPostEntity.upsert(_connection, posts)
  }

  public override suspend fun upsert(post: PostEntity): Unit = performSuspending(__db, false, true)
      { _connection ->
    __upsertAdapterOfPostEntity.upsert(_connection, post)
  }

  public override fun observeAll(): Flow<List<PostEntity>> {
    val _sql: String = "SELECT * FROM posts ORDER BY date DESC"
    return createFlow(__db, false, arrayOf("posts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRelativePath: Int = getColumnIndexOrThrow(_stmt, "relativePath")
        val _columnIndexOfSlug: Int = getColumnIndexOrThrow(_stmt, "slug")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDraft: Int = getColumnIndexOrThrow(_stmt, "draft")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfCategories: Int = getColumnIndexOrThrow(_stmt, "categories")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfLastModified: Int = getColumnIndexOrThrow(_stmt, "lastModified")
        val _columnIndexOfWordCount: Int = getColumnIndexOrThrow(_stmt, "wordCount")
        val _result: MutableList<PostEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PostEntity
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRelativePath: String
          _tmpRelativePath = _stmt.getText(_columnIndexOfRelativePath)
          val _tmpSlug: String
          _tmpSlug = _stmt.getText(_columnIndexOfSlug)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpDraft: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDraft).toInt()
          _tmpDraft = _tmp != 0
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpCategories: String
          _tmpCategories = _stmt.getText(_columnIndexOfCategories)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpLastModified: Long
          _tmpLastModified = _stmt.getLong(_columnIndexOfLastModified)
          val _tmpWordCount: Int
          _tmpWordCount = _stmt.getLong(_columnIndexOfWordCount).toInt()
          _item =
              PostEntity(_tmpFilePath,_tmpRelativePath,_tmpSlug,_tmpTitle,_tmpDate,_tmpDraft,_tmpTags,_tmpCategories,_tmpDescription,_tmpLastModified,_tmpWordCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<PostEntity> {
    val _sql: String = "SELECT * FROM posts ORDER BY date DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRelativePath: Int = getColumnIndexOrThrow(_stmt, "relativePath")
        val _columnIndexOfSlug: Int = getColumnIndexOrThrow(_stmt, "slug")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDraft: Int = getColumnIndexOrThrow(_stmt, "draft")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfCategories: Int = getColumnIndexOrThrow(_stmt, "categories")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfLastModified: Int = getColumnIndexOrThrow(_stmt, "lastModified")
        val _columnIndexOfWordCount: Int = getColumnIndexOrThrow(_stmt, "wordCount")
        val _result: MutableList<PostEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PostEntity
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRelativePath: String
          _tmpRelativePath = _stmt.getText(_columnIndexOfRelativePath)
          val _tmpSlug: String
          _tmpSlug = _stmt.getText(_columnIndexOfSlug)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpDraft: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDraft).toInt()
          _tmpDraft = _tmp != 0
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpCategories: String
          _tmpCategories = _stmt.getText(_columnIndexOfCategories)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpLastModified: Long
          _tmpLastModified = _stmt.getLong(_columnIndexOfLastModified)
          val _tmpWordCount: Int
          _tmpWordCount = _stmt.getLong(_columnIndexOfWordCount).toInt()
          _item =
              PostEntity(_tmpFilePath,_tmpRelativePath,_tmpSlug,_tmpTitle,_tmpDate,_tmpDraft,_tmpTags,_tmpCategories,_tmpDescription,_tmpLastModified,_tmpWordCount)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByPath(filePath: String): PostEntity? {
    val _sql: String = "SELECT * FROM posts WHERE filePath = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, filePath)
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRelativePath: Int = getColumnIndexOrThrow(_stmt, "relativePath")
        val _columnIndexOfSlug: Int = getColumnIndexOrThrow(_stmt, "slug")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDraft: Int = getColumnIndexOrThrow(_stmt, "draft")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfCategories: Int = getColumnIndexOrThrow(_stmt, "categories")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfLastModified: Int = getColumnIndexOrThrow(_stmt, "lastModified")
        val _columnIndexOfWordCount: Int = getColumnIndexOrThrow(_stmt, "wordCount")
        val _result: PostEntity?
        if (_stmt.step()) {
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRelativePath: String
          _tmpRelativePath = _stmt.getText(_columnIndexOfRelativePath)
          val _tmpSlug: String
          _tmpSlug = _stmt.getText(_columnIndexOfSlug)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpDraft: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfDraft).toInt()
          _tmpDraft = _tmp != 0
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpCategories: String
          _tmpCategories = _stmt.getText(_columnIndexOfCategories)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpLastModified: Long
          _tmpLastModified = _stmt.getLong(_columnIndexOfLastModified)
          val _tmpWordCount: Int
          _tmpWordCount = _stmt.getLong(_columnIndexOfWordCount).toInt()
          _result =
              PostEntity(_tmpFilePath,_tmpRelativePath,_tmpSlug,_tmpTitle,_tmpDate,_tmpDraft,_tmpTags,_tmpCategories,_tmpDescription,_tmpLastModified,_tmpWordCount)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteByPath(filePath: String) {
    val _sql: String = "DELETE FROM posts WHERE filePath = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, filePath)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM posts"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
