package com.tarit.slipledger.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TxDao_Impl(
  __db: RoomDatabase,
) : TxDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTx: EntityInsertAdapter<Tx>

  private val __deleteAdapterOfTx: EntityDeleteOrUpdateAdapter<Tx>

  private val __updateAdapterOfTx: EntityDeleteOrUpdateAdapter<Tx>
  init {
    this.__db = __db
    this.__insertAdapterOfTx = object : EntityInsertAdapter<Tx>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `transactions` (`id`,`type`,`amountSatang`,`timestamp`,`counterparty`,`bank`,`account`,`category`,`note`,`source`,`sourceKey`,`imageUri`,`rawText`,`tags`,`walletId`,`toWalletId`,`needsReview`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Tx) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, __TxType_enumToString(entity.type))
        statement.bindLong(3, entity.amountSatang)
        statement.bindLong(4, entity.timestamp)
        statement.bindText(5, entity.counterparty)
        statement.bindText(6, entity.bank)
        statement.bindText(7, entity.account)
        statement.bindText(8, entity.category)
        statement.bindText(9, entity.note)
        statement.bindText(10, __TxSource_enumToString(entity.source))
        statement.bindText(11, entity.sourceKey)
        val _tmpImageUri: String? = entity.imageUri
        if (_tmpImageUri == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpImageUri)
        }
        statement.bindText(13, entity.rawText)
        statement.bindText(14, entity.tags)
        val _tmpWalletId: Long? = entity.walletId
        if (_tmpWalletId == null) {
          statement.bindNull(15)
        } else {
          statement.bindLong(15, _tmpWalletId)
        }
        val _tmpToWalletId: Long? = entity.toWalletId
        if (_tmpToWalletId == null) {
          statement.bindNull(16)
        } else {
          statement.bindLong(16, _tmpToWalletId)
        }
        val _tmp: Int = if (entity.needsReview) 1 else 0
        statement.bindLong(17, _tmp.toLong())
      }
    }
    this.__deleteAdapterOfTx = object : EntityDeleteOrUpdateAdapter<Tx>() {
      protected override fun createQuery(): String = "DELETE FROM `transactions` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Tx) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfTx = object : EntityDeleteOrUpdateAdapter<Tx>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `transactions` SET `id` = ?,`type` = ?,`amountSatang` = ?,`timestamp` = ?,`counterparty` = ?,`bank` = ?,`account` = ?,`category` = ?,`note` = ?,`source` = ?,`sourceKey` = ?,`imageUri` = ?,`rawText` = ?,`tags` = ?,`walletId` = ?,`toWalletId` = ?,`needsReview` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Tx) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, __TxType_enumToString(entity.type))
        statement.bindLong(3, entity.amountSatang)
        statement.bindLong(4, entity.timestamp)
        statement.bindText(5, entity.counterparty)
        statement.bindText(6, entity.bank)
        statement.bindText(7, entity.account)
        statement.bindText(8, entity.category)
        statement.bindText(9, entity.note)
        statement.bindText(10, __TxSource_enumToString(entity.source))
        statement.bindText(11, entity.sourceKey)
        val _tmpImageUri: String? = entity.imageUri
        if (_tmpImageUri == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmpImageUri)
        }
        statement.bindText(13, entity.rawText)
        statement.bindText(14, entity.tags)
        val _tmpWalletId: Long? = entity.walletId
        if (_tmpWalletId == null) {
          statement.bindNull(15)
        } else {
          statement.bindLong(15, _tmpWalletId)
        }
        val _tmpToWalletId: Long? = entity.toWalletId
        if (_tmpToWalletId == null) {
          statement.bindNull(16)
        } else {
          statement.bindLong(16, _tmpToWalletId)
        }
        val _tmp: Int = if (entity.needsReview) 1 else 0
        statement.bindLong(17, _tmp.toLong())
        statement.bindLong(18, entity.id)
      }
    }
  }

  public override suspend fun insert(tx: Tx): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfTx.insertAndReturnId(_connection, tx)
    _result
  }

  public override suspend fun delete(tx: Tx): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfTx.handle(_connection, tx)
  }

  public override suspend fun update(tx: Tx): Unit = performSuspending(__db, false, true) {
      _connection ->
    __updateAdapterOfTx.handle(_connection, tx)
  }

  public override fun observeRange(from: Long, to: Long): Flow<List<Tx>> {
    val _sql: String =
        "SELECT * FROM transactions WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAll(): Flow<List<Tx>> {
    val _sql: String = "SELECT * FROM transactions ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun all(): List<Tx> {
    val _sql: String = "SELECT * FROM transactions ORDER BY timestamp"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTimestampsSince(from: Long): Flow<List<Long>> {
    val _sql: String =
        "SELECT timestamp FROM transactions WHERE timestamp >= ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        val _result: MutableList<Long> = mutableListOf()
        while (_stmt.step()) {
          val _item: Long
          _item = _stmt.getLong(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findSimilar(
    amount: Long,
    from: Long,
    to: Long,
  ): List<Tx> {
    val _sql: String =
        "SELECT * FROM transactions WHERE amountSatang = ? AND timestamp BETWEEN ? AND ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, amount)
        _argIndex = 2
        _stmt.bindLong(_argIndex, from)
        _argIndex = 3
        _stmt.bindLong(_argIndex, to)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun expenseByCategory(from: Long, to: Long): List<CategoryTotal> {
    val _sql: String =
        "SELECT category, SUM(amountSatang) AS total FROM transactions WHERE type = 'EXPENSE' AND timestamp >= ? AND timestamp < ? GROUP BY category"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _columnIndexOfCategory: Int = 0
        val _columnIndexOfTotal: Int = 1
        val _result: MutableList<CategoryTotal> = mutableListOf()
        while (_stmt.step()) {
          val _item: CategoryTotal
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpTotal: Long
          _tmpTotal = _stmt.getLong(_columnIndexOfTotal)
          _item = CategoryTotal(_tmpCategory,_tmpTotal)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun withTagLike(tag: String): List<Tx> {
    val _sql: String = "SELECT * FROM transactions WHERE tags LIKE '%' || ? || '%'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, tag)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeNeedsReview(): Flow<List<Tx>> {
    val _sql: String = "SELECT * FROM transactions WHERE needsReview = 1 ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfAmountSatang: Int = getColumnIndexOrThrow(_stmt, "amountSatang")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfCounterparty: Int = getColumnIndexOrThrow(_stmt, "counterparty")
        val _columnIndexOfBank: Int = getColumnIndexOrThrow(_stmt, "bank")
        val _columnIndexOfAccount: Int = getColumnIndexOrThrow(_stmt, "account")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfSourceKey: Int = getColumnIndexOrThrow(_stmt, "sourceKey")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "imageUri")
        val _columnIndexOfRawText: Int = getColumnIndexOrThrow(_stmt, "rawText")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfWalletId: Int = getColumnIndexOrThrow(_stmt, "walletId")
        val _columnIndexOfToWalletId: Int = getColumnIndexOrThrow(_stmt, "toWalletId")
        val _columnIndexOfNeedsReview: Int = getColumnIndexOrThrow(_stmt, "needsReview")
        val _result: MutableList<Tx> = mutableListOf()
        while (_stmt.step()) {
          val _item: Tx
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpType: TxType
          _tmpType = __TxType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpAmountSatang: Long
          _tmpAmountSatang = _stmt.getLong(_columnIndexOfAmountSatang)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpCounterparty: String
          _tmpCounterparty = _stmt.getText(_columnIndexOfCounterparty)
          val _tmpBank: String
          _tmpBank = _stmt.getText(_columnIndexOfBank)
          val _tmpAccount: String
          _tmpAccount = _stmt.getText(_columnIndexOfAccount)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpSource: TxSource
          _tmpSource = __TxSource_stringToEnum(_stmt.getText(_columnIndexOfSource))
          val _tmpSourceKey: String
          _tmpSourceKey = _stmt.getText(_columnIndexOfSourceKey)
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawText: String
          _tmpRawText = _stmt.getText(_columnIndexOfRawText)
          val _tmpTags: String
          _tmpTags = _stmt.getText(_columnIndexOfTags)
          val _tmpWalletId: Long?
          if (_stmt.isNull(_columnIndexOfWalletId)) {
            _tmpWalletId = null
          } else {
            _tmpWalletId = _stmt.getLong(_columnIndexOfWalletId)
          }
          val _tmpToWalletId: Long?
          if (_stmt.isNull(_columnIndexOfToWalletId)) {
            _tmpToWalletId = null
          } else {
            _tmpToWalletId = _stmt.getLong(_columnIndexOfToWalletId)
          }
          val _tmpNeedsReview: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfNeedsReview).toInt()
          _tmpNeedsReview = _tmp != 0
          _item =
              Tx(_tmpId,_tmpType,_tmpAmountSatang,_tmpTimestamp,_tmpCounterparty,_tmpBank,_tmpAccount,_tmpCategory,_tmpNote,_tmpSource,_tmpSourceKey,_tmpImageUri,_tmpRawText,_tmpTags,_tmpWalletId,_tmpToWalletId,_tmpNeedsReview)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun expenseTotal(from: Long, to: Long): Long {
    val _sql: String =
        "SELECT COALESCE(SUM(amountSatang), 0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= ? AND timestamp < ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, from)
        _argIndex = 2
        _stmt.bindLong(_argIndex, to)
        val _result: Long
        if (_stmt.step()) {
          val _tmp: Long
          _tmp = _stmt.getLong(0)
          _result = _tmp
        } else {
          _result = 0L
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM transactions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun setCategoryFor(
    counterparty: String,
    type: TxType,
    category: String,
  ) {
    val _sql: String = "UPDATE transactions SET category = ? WHERE counterparty = ? AND type = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _argIndex = 2
        _stmt.bindText(_argIndex, counterparty)
        _argIndex = 3
        _stmt.bindText(_argIndex, __TxType_enumToString(type))
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun renameCategory(old: String, new: String) {
    val _sql: String = "UPDATE transactions SET category = ? WHERE category = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, new)
        _argIndex = 2
        _stmt.bindText(_argIndex, old)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun markReviewed(ids: List<Long>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("UPDATE transactions SET needsReview = 0 WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: Long in ids) {
          _stmt.bindLong(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
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

  private fun __TxSource_enumToString(_value: TxSource): String = when (_value) {
    TxSource.SLIP -> "SLIP"
    TxSource.NOTIFICATION -> "NOTIFICATION"
    TxSource.MANUAL -> "MANUAL"
    TxSource.RECURRING -> "RECURRING"
  }

  private fun __TxType_stringToEnum(_value: String): TxType = when (_value) {
    "INCOME" -> TxType.INCOME
    "EXPENSE" -> TxType.EXPENSE
    "TRANSFER" -> TxType.TRANSFER
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  private fun __TxSource_stringToEnum(_value: String): TxSource = when (_value) {
    "SLIP" -> TxSource.SLIP
    "NOTIFICATION" -> TxSource.NOTIFICATION
    "MANUAL" -> TxSource.MANUAL
    "RECURRING" -> TxSource.RECURRING
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
