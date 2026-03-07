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
public class CommitDao_Impl(
  __db: RoomDatabase,
) : CommitDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfCommitEntity: EntityUpsertAdapter<CommitEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfCommitEntity = EntityUpsertAdapter<CommitEntity>(object :
        EntityInsertAdapter<CommitEntity>() {
      protected override fun createQuery(): String =
          "INSERT INTO `commits` (`hash`,`shortHash`,`message`,`authorName`,`authorEmail`,`time`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CommitEntity) {
        statement.bindText(1, entity.hash)
        statement.bindText(2, entity.shortHash)
        statement.bindText(3, entity.message)
        statement.bindText(4, entity.authorName)
        statement.bindText(5, entity.authorEmail)
        statement.bindLong(6, entity.time)
      }
    }, object : EntityDeleteOrUpdateAdapter<CommitEntity>() {
      protected override fun createQuery(): String =
          "UPDATE `commits` SET `hash` = ?,`shortHash` = ?,`message` = ?,`authorName` = ?,`authorEmail` = ?,`time` = ? WHERE `hash` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CommitEntity) {
        statement.bindText(1, entity.hash)
        statement.bindText(2, entity.shortHash)
        statement.bindText(3, entity.message)
        statement.bindText(4, entity.authorName)
        statement.bindText(5, entity.authorEmail)
        statement.bindLong(6, entity.time)
        statement.bindText(7, entity.hash)
      }
    })
  }

  public override suspend fun upsertAll(commits: List<CommitEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __upsertAdapterOfCommitEntity.upsert(_connection, commits)
  }

  public override fun observeRecent(limit: Int): Flow<List<CommitEntity>> {
    val _sql: String = "SELECT * FROM commits ORDER BY time DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("commits")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfHash: Int = getColumnIndexOrThrow(_stmt, "hash")
        val _columnIndexOfShortHash: Int = getColumnIndexOrThrow(_stmt, "shortHash")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfAuthorName: Int = getColumnIndexOrThrow(_stmt, "authorName")
        val _columnIndexOfAuthorEmail: Int = getColumnIndexOrThrow(_stmt, "authorEmail")
        val _columnIndexOfTime: Int = getColumnIndexOrThrow(_stmt, "time")
        val _result: MutableList<CommitEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CommitEntity
          val _tmpHash: String
          _tmpHash = _stmt.getText(_columnIndexOfHash)
          val _tmpShortHash: String
          _tmpShortHash = _stmt.getText(_columnIndexOfShortHash)
          val _tmpMessage: String
          _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          val _tmpAuthorName: String
          _tmpAuthorName = _stmt.getText(_columnIndexOfAuthorName)
          val _tmpAuthorEmail: String
          _tmpAuthorEmail = _stmt.getText(_columnIndexOfAuthorEmail)
          val _tmpTime: Long
          _tmpTime = _stmt.getLong(_columnIndexOfTime)
          _item =
              CommitEntity(_tmpHash,_tmpShortHash,_tmpMessage,_tmpAuthorName,_tmpAuthorEmail,_tmpTime)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM commits"
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
