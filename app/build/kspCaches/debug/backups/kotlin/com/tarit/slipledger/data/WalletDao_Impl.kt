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
public class WalletDao_Impl(
  __db: RoomDatabase,
) : WalletDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfWallet: EntityInsertAdapter<Wallet>

  private val __deleteAdapterOfWallet: EntityDeleteOrUpdateAdapter<Wallet>

  private val __updateAdapterOfWallet: EntityDeleteOrUpdateAdapter<Wallet>
  init {
    this.__db = __db
    this.__insertAdapterOfWallet = object : EntityInsertAdapter<Wallet>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `wallets` (`id`,`name`,`kind`,`bank`,`accountHint`,`openingBalance`,`openingDate`,`balanceSet`,`color`,`sortOrder`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Wallet) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __WalletKind_enumToString(entity.kind))
        statement.bindText(4, entity.bank)
        statement.bindText(5, entity.accountHint)
        statement.bindLong(6, entity.openingBalance)
        statement.bindLong(7, entity.openingDate)
        val _tmp: Int = if (entity.balanceSet) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindLong(9, entity.color)
        statement.bindLong(10, entity.sortOrder.toLong())
      }
    }
    this.__deleteAdapterOfWallet = object : EntityDeleteOrUpdateAdapter<Wallet>() {
      protected override fun createQuery(): String = "DELETE FROM `wallets` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Wallet) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfWallet = object : EntityDeleteOrUpdateAdapter<Wallet>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `wallets` SET `id` = ?,`name` = ?,`kind` = ?,`bank` = ?,`accountHint` = ?,`openingBalance` = ?,`openingDate` = ?,`balanceSet` = ?,`color` = ?,`sortOrder` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Wallet) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, __WalletKind_enumToString(entity.kind))
        statement.bindText(4, entity.bank)
        statement.bindText(5, entity.accountHint)
        statement.bindLong(6, entity.openingBalance)
        statement.bindLong(7, entity.openingDate)
        val _tmp: Int = if (entity.balanceSet) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindLong(9, entity.color)
        statement.bindLong(10, entity.sortOrder.toLong())
        statement.bindLong(11, entity.id)
      }
    }
  }

  public override suspend fun insert(wallet: Wallet): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfWallet.insertAndReturnId(_connection, wallet)
    _result
  }

  public override suspend fun delete(wallet: Wallet): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfWallet.handle(_connection, wallet)
  }

  public override suspend fun update(list: List<Wallet>): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfWallet.handleMultiple(_connection, list)
  }

  public override fun observe(): Flow<List<Wallet>> {
    val _sql: String = "SELECT * FROM wallets ORDER BY sortOrder, id"
    return createFlow(__db, false, arrayOf("wallets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccountHint: Int = getColumnIndexOrThrow(_stmt, "accountHint")
        val _columnIndexOfOpeningBalance: Int = getColumnIndexOrThrow(_stmt, "openingBalance")
        val _columnIndexOfOpeningDate: Int = getColumnIndexOrThrow(_stmt, "openingDate")
        val _columnIndexOfBalanceSet: Int = getColumnIndexOrThrow(_stmt, "balanceSet")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<Wallet> = mutableListOf()
        while (_stmt.step()) {
          val _item: Wallet
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpKind: WalletKind
          _tmpKind = __WalletKind_stringToEnum(_stmt.getText(_columnIndexOfKind))
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccountHint: String
          _tmpAccountHint = _stmt.getText(_columnIndexOfAccountHint)
          val _tmpOpeningBalance: Long
          _tmpOpeningBalance = _stmt.getLong(_columnIndexOfOpeningBalance)
          val _tmpOpeningDate: Long
          _tmpOpeningDate = _stmt.getLong(_columnIndexOfOpeningDate)
          val _tmpBalanceSet: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBalanceSet).toInt()
          _tmpBalanceSet = _tmp != 0
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item =
              Wallet(_tmpId,_tmpName,_tmpKind,_tmpBank,_tmpAccountHint,_tmpOpeningBalance,_tmpOpeningDate,_tmpBalanceSet,_tmpColor,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun all(): List<Wallet> {
    val _sql: String = "SELECT * FROM wallets ORDER BY sortOrder, id"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfKind: Int = getColumnIndexOrThrow(_stmt, "kind")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccountHint: Int = getColumnIndexOrThrow(_stmt, "accountHint")
        val _columnIndexOfOpeningBalance: Int = getColumnIndexOrThrow(_stmt, "openingBalance")
        val _columnIndexOfOpeningDate: Int = getColumnIndexOrThrow(_stmt, "openingDate")
        val _columnIndexOfBalanceSet: Int = getColumnIndexOrThrow(_stmt, "balanceSet")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<Wallet> = mutableListOf()
        while (_stmt.step()) {
          val _item: Wallet
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpKind: WalletKind
          _tmpKind = __WalletKind_stringToEnum(_stmt.getText(_columnIndexOfKind))
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccountHint: String
          _tmpAccountHint = _stmt.getText(_columnIndexOfAccountHint)
          val _tmpOpeningBalance: Long
          _tmpOpeningBalance = _stmt.getLong(_columnIndexOfOpeningBalance)
          val _tmpOpeningDate: Long
          _tmpOpeningDate = _stmt.getLong(_columnIndexOfOpeningDate)
          val _tmpBalanceSet: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBalanceSet).toInt()
          _tmpBalanceSet = _tmp != 0
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item =
              Wallet(_tmpId,_tmpName,_tmpKind,_tmpBank,_tmpAccountHint,_tmpOpeningBalance,_tmpOpeningDate,_tmpBalanceSet,_tmpColor,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun detachFrom(id: Long) {
    val _sql: String = "UPDATE transactions SET walletId = NULL WHERE walletId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun detachTo(id: Long) {
    val _sql: String = "UPDATE transactions SET toWalletId = NULL WHERE toWalletId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __WalletKind_enumToString(_value: WalletKind): String = when (_value) {
    WalletKind.BANK -> "BANK"
    WalletKind.CASH -> "CASH"
    WalletKind.EWALLET -> "EWALLET"
  }

  private fun __WalletKind_stringToEnum(_value: String): WalletKind = when (_value) {
    "BANK" -> WalletKind.BANK
    "CASH" -> WalletKind.CASH
    "EWALLET" -> WalletKind.EWALLET
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
