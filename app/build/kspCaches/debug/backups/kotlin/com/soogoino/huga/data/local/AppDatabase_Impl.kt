package com.soogoino.huga.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _postDao: Lazy<PostDao> = lazy {
    PostDao_Impl(this)
  }

  private val _commitDao: Lazy<CommitDao> = lazy {
    CommitDao_Impl(this)
  }

  private val _draftDao: Lazy<DraftDao> = lazy {
    DraftDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2,
        "dc60c1a48eeb176756517d8d19f6d9a1", "c0c83c71b9caf6076f8cae4585e72b9e") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `posts` (`filePath` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `slug` TEXT NOT NULL, `title` TEXT NOT NULL, `date` TEXT NOT NULL, `draft` INTEGER NOT NULL, `tags` TEXT NOT NULL, `categories` TEXT NOT NULL, `description` TEXT NOT NULL, `lastModified` INTEGER NOT NULL, `wordCount` INTEGER NOT NULL, PRIMARY KEY(`filePath`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `commits` (`hash` TEXT NOT NULL, `shortHash` TEXT NOT NULL, `message` TEXT NOT NULL, `authorName` TEXT NOT NULL, `authorEmail` TEXT NOT NULL, `time` INTEGER NOT NULL, PRIMARY KEY(`hash`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `drafts` (`filePath` TEXT NOT NULL, `content` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, `isDirty` INTEGER NOT NULL, PRIMARY KEY(`filePath`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dc60c1a48eeb176756517d8d19f6d9a1')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `posts`")
        connection.execSQL("DROP TABLE IF EXISTS `commits`")
        connection.execSQL("DROP TABLE IF EXISTS `drafts`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsPosts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPosts.put("filePath", TableInfo.Column("filePath", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("relativePath", TableInfo.Column("relativePath", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("slug", TableInfo.Column("slug", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("date", TableInfo.Column("date", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("draft", TableInfo.Column("draft", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("tags", TableInfo.Column("tags", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("categories", TableInfo.Column("categories", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("lastModified", TableInfo.Column("lastModified", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPosts.put("wordCount", TableInfo.Column("wordCount", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPosts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPosts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPosts: TableInfo = TableInfo("posts", _columnsPosts, _foreignKeysPosts,
            _indicesPosts)
        val _existingPosts: TableInfo = read(connection, "posts")
        if (!_infoPosts.equals(_existingPosts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |posts(com.soogoino.huga.data.local.PostEntity).
              | Expected:
              |""".trimMargin() + _infoPosts + """
              |
              | Found:
              |""".trimMargin() + _existingPosts)
        }
        val _columnsCommits: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCommits.put("hash", TableInfo.Column("hash", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCommits.put("shortHash", TableInfo.Column("shortHash", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCommits.put("message", TableInfo.Column("message", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCommits.put("authorName", TableInfo.Column("authorName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCommits.put("authorEmail", TableInfo.Column("authorEmail", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCommits.put("time", TableInfo.Column("time", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCommits: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCommits: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCommits: TableInfo = TableInfo("commits", _columnsCommits, _foreignKeysCommits,
            _indicesCommits)
        val _existingCommits: TableInfo = read(connection, "commits")
        if (!_infoCommits.equals(_existingCommits)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |commits(com.soogoino.huga.data.local.CommitEntity).
              | Expected:
              |""".trimMargin() + _infoCommits + """
              |
              | Found:
              |""".trimMargin() + _existingCommits)
        }
        val _columnsDrafts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDrafts.put("filePath", TableInfo.Column("filePath", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDrafts.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDrafts.put("savedAt", TableInfo.Column("savedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDrafts.put("isDirty", TableInfo.Column("isDirty", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDrafts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDrafts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDrafts: TableInfo = TableInfo("drafts", _columnsDrafts, _foreignKeysDrafts,
            _indicesDrafts)
        val _existingDrafts: TableInfo = read(connection, "drafts")
        if (!_infoDrafts.equals(_existingDrafts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |drafts(com.soogoino.huga.data.local.DraftEntity).
              | Expected:
              |""".trimMargin() + _infoDrafts + """
              |
              | Found:
              |""".trimMargin() + _existingDrafts)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "posts", "commits", "drafts")
  }

  public override fun clearAllTables() {
    super.performClear(false, "posts", "commits", "drafts")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PostDao::class, PostDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CommitDao::class, CommitDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(DraftDao::class, DraftDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun postDao(): PostDao = _postDao.value

  public override fun commitDao(): CommitDao = _commitDao.value

  public override fun draftDao(): DraftDao = _draftDao.value
}
