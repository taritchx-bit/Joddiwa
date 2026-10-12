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
public class RecurringDao_Impl(
  __db: RoomDatabase,
) : RecurringDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRecurringRule: EntityInsertAdapter<RecurringRule>

  private val __deleteAdapterOfRecurringRule: EntityDeleteOrUpdateAdapter<RecurringRule>
  init {
    this.__db = __db
    this.__insertAdapterOfRecurringRule = object : EntityInsertAdapter<RecurringRule>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `recurring` (`id`,`type`,`amountSatang`,`category`,`counterparty`,`note`,`dayOfMonth`,`lastRun`,`active`,`walletId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecurringRule) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, __TxType_enumToString(entity.type))
        statement.bindLong(3, entity.amountSatang)
        statement.bindText(4, entity.category)
        statement.bindText(5, entity.counterparty)
        statement.bindText(6, entity.note)
        statement.bindLong(7, entity.dayOfMonth.toLong())
        statement.bindText(8, entity.lastRun)
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        val _tmpWalletId: Long? = entity.walletId
        if (_tmpWalletId == null) {
          statement.bindNull(10)
        } else {
          statement.bindLong(10, _tmpWalletId)
        }
      }
    }
    this.__deleteAdapterOfRecurringRule = object : EntityDeleteOrUpdateAdapter<RecurringRule>() {
      protected override fun createQuery(): String = "DELETE FROM `recurring` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RecurringRule) {
        statement.bindLong(1, entity.id)
      }
    }
  }

  public override suspend fun upsert(rule: RecurringRule): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfRecurringRule.insertAndReturnId(_connection, rule)
    _result
  }

  public override suspend fun delete(rule: RecurringRule): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfRecurringRule.handle(_connection, rule)
  }

  public override fun observe(): Flow<List<RecurringRule>> {
    val _sql: String = "SELECT * FROM recurring ORDER BY dayOfMonth"
    return createFlow(__db, false, arrayOf("recurring")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfDayOfMonth: Int = getColumnIndexOrThrow(_stmt, "dayOfMonth")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _result: MutableList<RecurringRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecurringRule
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpDayOfMonth: Int
          _tmpDayOfMonth = _stmt.getLong(_columnIndexOfDayOfMonth).toInt()
          val _tmpLastRun: String
          _tmpLastRun = _stmt.getText(_columnIndexOfLastRun)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          _item =
              RecurringRule(_tmpId,_tmpType,_tmpAmountSatang,_tmpCategory,_tmpCounterparty,_tmpNote,_tmpDayOfMonth,_tmpLastRun,_tmpActive,_tmpWalletId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun active(): List<RecurringRule> {
    val _sql: String = "SELECT * FROM recurring WHERE active = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfDayOfMonth: Int = getColumnIndexOrThrow(_stmt, "dayOfMonth")
        val _columnIndexOfLastRun: Int = getColumnIndexOrThrow(_stmt, "lastRun")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _result: MutableList<RecurringRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecurringRule
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpDayOfMonth: Int
          _tmpDayOfMonth = _stmt.getLong(_columnIndexOfDayOfMonth).toInt()
          val _tmpLastRun: String
          _tmpLastRun = _stmt.getText(_columnIndexOfLastRun)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          _item =
              RecurringRule(_tmpId,_tmpType,_tmpAmountSatang,_tmpCategory,_tmpCounterparty,_tmpNote,_tmpDayOfMonth,_tmpLastRun,_tmpActive,_tmpWalletId)
          _result.add(_item)
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
