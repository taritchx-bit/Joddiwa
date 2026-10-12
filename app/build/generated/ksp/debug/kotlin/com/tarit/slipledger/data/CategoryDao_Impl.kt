package com.tarit.slipledger.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.IllegalArgumentException
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
public class CategoryDao_Impl(
  __db: RoomDatabase,
) : CategoryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCategoryDef: EntityInsertAdapter<CategoryDef>

  private val __insertAdapterOfCategoryDef_1: EntityInsertAdapter<CategoryDef>

  private val __deleteAdapterOfCategoryDef: EntityDeleteOrUpdateAdapter<CategoryDef>

  private val __updateAdapterOfCategoryDef: EntityDeleteOrUpdateAdapter<CategoryDef>
  init {
    this.__db = __db
    this.__insertAdapterOfCategoryDef = object : EntityInsertAdapter<CategoryDef>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `categories` (`id`,`name`,`kind`,`icon`,`color`,`sortOrder`,`builtIn`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryDef) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __TxType_enumToString(entity.kind))
        statement.bindText(4, entity.icon)
        statement.bindLong(5, entity.color)
        statement.bindLong(6, entity.sortOrder.toLong())
        val _tmp: Int = if (entity.builtIn) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
    this.__insertAdapterOfCategoryDef_1 = object : EntityInsertAdapter<CategoryDef>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `categories` (`id`,`name`,`kind`,`icon`,`color`,`sortOrder`,`builtIn`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryDef) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __TxType_enumToString(entity.kind))
        statement.bindText(4, entity.icon)
        statement.bindLong(5, entity.color)
        statement.bindLong(6, entity.sortOrder.toLong())
        val _tmp: Int = if (entity.builtIn) 1 else 0
        statement.bindLong(7, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfCategoryDef = object : EntityDeleteOrUpdateAdapter<CategoryDef>() {
      protected override fun createQuery(): String = "DELETE FROM `categories` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryDef) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfCategoryDef = object : EntityDeleteOrUpdateAdapter<CategoryDef>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `categories` SET `id` = ?,`name` = ?,`kind` = ?,`icon` = ?,`color` = ?,`sortOrder` = ?,`builtIn` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryDef) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __TxType_enumToString(entity.kind))
        statement.bindText(4, entity.icon)
        statement.bindLong(5, entity.color)
        statement.bindLong(6, entity.sortOrder.toLong())
        val _tmp: Int = if (entity.builtIn) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindLong(8, entity.id)
      }
    }
  }

  public override suspend fun insertAll(list: List<CategoryDef>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfCategoryDef.insert(_connection, list)
  }

  public override suspend fun insert(def: CategoryDef): Long = performSuspending(__db, false, true)
      { _connection ->
    val _result: Long = __insertAdapterOfCategoryDef_1.insertAndReturnId(_connection, def)
    _result
  }

  public override suspend fun delete(def: CategoryDef): Unit = performSuspending(__db, false, true)
      { _connection ->
    __deleteAdapterOfCategoryDef.handle(_connection, def)
  }

  public override suspend fun update(list: List<CategoryDef>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfCategoryDef.handleMultiple(_connection, list)
  }

  public override fun observe(): Flow<List<CategoryDef>> {
    val _sql: String = "SELECT * FROM categories ORDER BY kind, sortOrder"
    return createFlow(__db, false, arrayOf("categories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfIcon: Int = getColumnIndexOrThrow(_stmt, "icon")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _columnIndexOfBuiltIn: Int = getColumnIndexOrThrow(_stmt, "builtIn")
        val _result: MutableList<CategoryDef> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryDef
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpKind: TxType
          _tmpKind = __TxType_stringToEnum(_stmt.getText(_columnIndexOfKind))
          val _tmpIcon: String
          _tmpIcon = _stmt.getText(_columnIndexOfIcon)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          val _tmpBuiltIn: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBuiltIn).toInt()
          _tmpBuiltIn = _tmp != 0
          _item = CategoryDef(_tmpId,_tmpName,_tmpKind,_tmpIcon,_tmpColor,_tmpSortOrder,_tmpBuiltIn)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun count(): Int {
    val _sql: String = "SELECT COUNT(*) FROM categories"
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

  private fun __TxType_enumToString(_value: TxType): String = when (_value) {
    TxType.INCOME -> "INCOME"
    TxType.EXPENSE -> "EXPENSE"
    TxType.TRANSFER -> "TRANSFER"
  }

  private fun __TxType_stringToEnum(_value: String): TxType = when (_value) {
    "INCOME" -> TxType.INCOME
    "EXPENSE" -> TxType.EXPENSE
    "TRANSFER" -> TxType.TRANSFER
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
