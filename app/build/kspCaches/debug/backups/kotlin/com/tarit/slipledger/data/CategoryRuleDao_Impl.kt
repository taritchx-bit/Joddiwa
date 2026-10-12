package com.tarit.slipledger.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class CategoryRuleDao_Impl(
  __db: RoomDatabase,
) : CategoryRuleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCategoryRule: EntityInsertAdapter<CategoryRule>
  init {
    this.__db = __db
    this.__insertAdapterOfCategoryRule = object : EntityInsertAdapter<CategoryRule>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `category_rules` (`key`,`category`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CategoryRule) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.category)
      }
    }
  }

  public override suspend fun upsert(rule: CategoryRule): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfCategoryRule.insert(_connection, rule)
  }

  public override suspend fun find(key: String): String? {
    val _sql: String = "SELECT category FROM category_rules WHERE `key` = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _result: String?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getText(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun renameCategory(old: String, new: String) {
    val _sql: String = "UPDATE category_rules SET category = ? WHERE category = ?"
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

  public override suspend fun deleteFor(category: String) {
    val _sql: String = "DELETE FROM category_rules WHERE category = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clear() {
    val _sql: String = "DELETE FROM category_rules"
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
