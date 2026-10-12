package com.tarit.slipledger.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
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
public class TagDao_Impl(
  __db: RoomDatabase,
) : TagDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTagDef: EntityInsertAdapter<TagDef>

  private val __deleteAdapterOfTagDef: EntityDeleteOrUpdateAdapter<TagDef>

  private val __updateAdapterOfTagDef: EntityDeleteOrUpdateAdapter<TagDef>
  init {
    this.__db = __db
    this.__insertAdapterOfTagDef = object : EntityInsertAdapter<TagDef>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `tag_defs` (`name`,`sortOrder`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TagDef) {
        statement.bindText(1, entity.name)
        statement.bindLong(2, entity.sortOrder.toLong())
      }
    }
    this.__deleteAdapterOfTagDef = object : EntityDeleteOrUpdateAdapter<TagDef>() {
      protected override fun createQuery(): String = "DELETE FROM `tag_defs` WHERE `name` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TagDef) {
        statement.bindText(1, entity.name)
      }
    }
    this.__updateAdapterOfTagDef = object : EntityDeleteOrUpdateAdapter<TagDef>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `tag_defs` SET `name` = ?,`sortOrder` = ? WHERE `name` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TagDef) {
        statement.bindText(1, entity.name)
        statement.bindLong(2, entity.sortOrder.toLong())
        statement.bindText(3, entity.name)
      }
    }
  }

  public override suspend fun insert(tag: TagDef): Unit = performSuspending(__db, false, true) {
      _connection ->
    __insertAdapterOfTagDef.insert(_connection, tag)
  }

  public override suspend fun delete(tag: TagDef): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfTagDef.handle(_connection, tag)
  }

  public override suspend fun update(list: List<TagDef>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfTagDef.handleMultiple(_connection, list)
  }

  public override fun observe(): Flow<List<TagDef>> {
    val _sql: String = "SELECT * FROM tag_defs ORDER BY sortOrder"
    return createFlow(__db, false, arrayOf("tag_defs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<TagDef> = mutableListOf()
        while (_stmt.step()) {
          val _item: TagDef
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item = TagDef(_tmpName,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun maxOrder(): Int {
    val _sql: String = "SELECT COALESCE(MAX(sortOrder), -1) FROM tag_defs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
