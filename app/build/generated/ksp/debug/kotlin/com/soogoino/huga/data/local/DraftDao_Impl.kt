package com.soogoino.huga.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
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
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class DraftDao_Impl(
  __db: RoomDatabase,
) : DraftDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfDraftEntity: EntityUpsertAdapter<DraftEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfDraftEntity = EntityUpsertAdapter<DraftEntity>(object :
        EntityInsertAdapter<DraftEntity>() {
      protected override fun createQuery(): String =
          "INSERT INTO `drafts` (`filePath`,`content`,`savedAt`,`isDirty`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DraftEntity) {
        statement.bindText(1, entity.filePath)
        statement.bindText(2, entity.content)
        statement.bindLong(3, entity.savedAt)
        val _tmp: Int = if (entity.isDirty) 1 else 0
        statement.bindLong(4, _tmp.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<DraftEntity>() {
      protected override fun createQuery(): String =
          "UPDATE `drafts` SET `filePath` = ?,`content` = ?,`savedAt` = ?,`isDirty` = ? WHERE `filePath` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DraftEntity) {
        statement.bindText(1, entity.filePath)
        statement.bindText(2, entity.content)
        statement.bindLong(3, entity.savedAt)
        val _tmp: Int = if (entity.isDirty) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.filePath)
      }
    })
  }

  public override suspend fun upsert(draft: DraftEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __upsertAdapterOfDraftEntity.upsert(_connection, draft)
  }

  public override suspend fun `get`(filePath: String): DraftEntity? {
    val _sql: String = "SELECT * FROM drafts WHERE filePath = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, filePath)
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfSavedAt: Int = getColumnIndexOrThrow(_stmt, "savedAt")
        val _columnIndexOfIsDirty: Int = getColumnIndexOrThrow(_stmt, "isDirty")
        val _result: DraftEntity?
        if (_stmt.step()) {
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpSavedAt: Long
          _tmpSavedAt = _stmt.getLong(_columnIndexOfSavedAt)
          val _tmpIsDirty: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsDirty).toInt()
          _tmpIsDirty = _tmp != 0
          _result = DraftEntity(_tmpFilePath,_tmpContent,_tmpSavedAt,_tmpIsDirty)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun delete(filePath: String) {
    val _sql: String = "DELETE FROM drafts WHERE filePath = ?"
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

  public override suspend fun markClean(filePath: String) {
    val _sql: String = "UPDATE drafts SET isDirty = 0 WHERE filePath = ?"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
