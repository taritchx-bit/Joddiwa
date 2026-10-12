package com.tarit.slipledger.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
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

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ScannedImageDao_Impl(
  __db: RoomDatabase,
) : ScannedImageDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfScannedImage: EntityInsertAdapter<ScannedImage>
  init {
    this.__db = __db
    this.__insertAdapterOfScannedImage = object : EntityInsertAdapter<ScannedImage>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `scanned_images` (`mediaId`,`isSlip`,`scannedAt`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ScannedImage) {
        statement.bindLong(1, entity.mediaId)
        val _tmp: Int = if (entity.isSlip) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        statement.bindLong(3, entity.scannedAt)
      }
    }
  }

  public override suspend fun insert(image: ScannedImage): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfScannedImage.insert(_connection, image)
  }

  public override suspend fun allIds(): List<Long> {
    val _sql: String = "SELECT mediaId FROM scanned_images"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
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

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM scanned_images"
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
