package com.tarit.slipledger.`data`

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
  private val _txDao: Lazy<TxDao> = lazy {
    TxDao_Impl(this)
  }

  private val _scannedImageDao: Lazy<ScannedImageDao> = lazy {
    ScannedImageDao_Impl(this)
  }

  private val _categoryRuleDao: Lazy<CategoryRuleDao> = lazy {
    CategoryRuleDao_Impl(this)
  }

  private val _categoryDao: Lazy<CategoryDao> = lazy {
    CategoryDao_Impl(this)
  }

  private val _tagDao: Lazy<TagDao> = lazy {
    TagDao_Impl(this)
  }

  private val _recurringDao: Lazy<RecurringDao> = lazy {
    RecurringDao_Impl(this)
  }

  private val _walletDao: Lazy<WalletDao> = lazy {
    WalletDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4,
        "358c5a4c73e8fa8e77d34cc8f2600b18", "67588a47a1a6cb13561f4e8a965052d8") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amountSatang` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `counterparty` TEXT NOT NULL, `bank` TEXT NOT NULL, `account` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `source` TEXT NOT NULL, `sourceKey` TEXT NOT NULL, `imageUri` TEXT, `rawText` TEXT NOT NULL, `tags` TEXT NOT NULL, `walletId` INTEGER, `toWalletId` INTEGER, `needsReview` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_sourceKey` ON `transactions` (`sourceKey`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `scanned_images` (`mediaId` INTEGER NOT NULL, `isSlip` INTEGER NOT NULL, `scannedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `category_rules` (`key` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`key`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `builtIn` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tag_defs` (`name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`name`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recurring` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amountSatang` INTEGER NOT NULL, `category` TEXT NOT NULL, `counterparty` TEXT NOT NULL, `note` TEXT NOT NULL, `dayOfMonth` INTEGER NOT NULL, `lastRun` TEXT NOT NULL, `active` INTEGER NOT NULL, `walletId` INTEGER)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `bank` TEXT NOT NULL, `accountHint` TEXT NOT NULL, `openingBalance` INTEGER NOT NULL, `openingDate` INTEGER NOT NULL, `balanceSet` INTEGER NOT NULL, `color` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '358c5a4c73e8fa8e77d34cc8f2600b18')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transactions`")
        connection.execSQL("DROP TABLE IF EXISTS `scanned_images`")
        connection.execSQL("DROP TABLE IF EXISTS `category_rules`")
        connection.execSQL("DROP TABLE IF EXISTS `categories`")
        connection.execSQL("DROP TABLE IF EXISTS `tag_defs`")
        connection.execSQL("DROP TABLE IF EXISTS `recurring`")
        connection.execSQL("DROP TABLE IF EXISTS `wallets`")
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
        val _columnsTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("amountSatang", TableInfo.Column("amountSatang", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("counterparty", TableInfo.Column("counterparty", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("bank", TableInfo.Column("bank", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("account", TableInfo.Column("account", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("note", TableInfo.Column("note", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("source", TableInfo.Column("source", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("sourceKey", TableInfo.Column("sourceKey", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("imageUri", TableInfo.Column("imageUri", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("rawText", TableInfo.Column("rawText", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("tags", TableInfo.Column("tags", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("walletId", TableInfo.Column("walletId", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("toWalletId", TableInfo.Column("toWalletId", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("needsReview", TableInfo.Column("needsReview", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTransactions.add(TableInfo.Index("index_transactions_sourceKey", true,
            listOf("sourceKey"), listOf("ASC")))
        _indicesTransactions.add(TableInfo.Index("index_transactions_timestamp", false,
            listOf("timestamp"), listOf("ASC")))
        val _infoTransactions: TableInfo = TableInfo("transactions", _columnsTransactions,
            _foreignKeysTransactions, _indicesTransactions)
        val _existingTransactions: TableInfo = read(connection, "transactions")
        if (!_infoTransactions.equals(_existingTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transactions(com.tarit.slipledger.data.Tx).
              | Expected:
              |""".trimMargin() + _infoTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingTransactions)
        }
        val _columnsScannedImages: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsScannedImages.put("mediaId", TableInfo.Column("mediaId", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScannedImages.put("isSlip", TableInfo.Column("isSlip", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsScannedImages.put("scannedAt", TableInfo.Column("scannedAt", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysScannedImages: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesScannedImages: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoScannedImages: TableInfo = TableInfo("scanned_images", _columnsScannedImages,
            _foreignKeysScannedImages, _indicesScannedImages)
        val _existingScannedImages: TableInfo = read(connection, "scanned_images")
        if (!_infoScannedImages.equals(_existingScannedImages)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |scanned_images(com.tarit.slipledger.data.ScannedImage).
              | Expected:
              |""".trimMargin() + _infoScannedImages + """
              |
              | Found:
              |""".trimMargin() + _existingScannedImages)
        }
        val _columnsCategoryRules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCategoryRules.put("key", TableInfo.Column("key", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategoryRules.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCategoryRules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCategoryRules: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCategoryRules: TableInfo = TableInfo("category_rules", _columnsCategoryRules,
            _foreignKeysCategoryRules, _indicesCategoryRules)
        val _existingCategoryRules: TableInfo = read(connection, "category_rules")
        if (!_infoCategoryRules.equals(_existingCategoryRules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |category_rules(com.tarit.slipledger.data.CategoryRule).
              | Expected:
              |""".trimMargin() + _infoCategoryRules + """
              |
              | Found:
              |""".trimMargin() + _existingCategoryRules)
        }
        val _columnsCategories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCategories.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("icon", TableInfo.Column("icon", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("color", TableInfo.Column("color", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCategories.put("builtIn", TableInfo.Column("builtIn", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCategories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCategories: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesCategories.add(TableInfo.Index("index_categories_name", true, listOf("name"),
            listOf("ASC")))
        val _infoCategories: TableInfo = TableInfo("categories", _columnsCategories,
            _foreignKeysCategories, _indicesCategories)
        val _existingCategories: TableInfo = read(connection, "categories")
        if (!_infoCategories.equals(_existingCategories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |categories(com.tarit.slipledger.data.CategoryDef).
              | Expected:
              |""".trimMargin() + _infoCategories + """
              |
              | Found:
              |""".trimMargin() + _existingCategories)
        }
        val _columnsTagDefs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTagDefs.put("name", TableInfo.Column("name", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTagDefs.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTagDefs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTagDefs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTagDefs: TableInfo = TableInfo("tag_defs", _columnsTagDefs, _foreignKeysTagDefs,
            _indicesTagDefs)
        val _existingTagDefs: TableInfo = read(connection, "tag_defs")
        if (!_infoTagDefs.equals(_existingTagDefs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tag_defs(com.tarit.slipledger.data.TagDef).
              | Expected:
              |""".trimMargin() + _infoTagDefs + """
              |
              | Found:
              |""".trimMargin() + _existingTagDefs)
        }
        val _columnsRecurring: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecurring.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("amountSatang", TableInfo.Column("amountSatang", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("counterparty", TableInfo.Column("counterparty", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("note", TableInfo.Column("note", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("dayOfMonth", TableInfo.Column("dayOfMonth", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("lastRun", TableInfo.Column("lastRun", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecurring.put("walletId", TableInfo.Column("walletId", "INTEGER", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecurring: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecurring: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRecurring: TableInfo = TableInfo("recurring", _columnsRecurring,
            _foreignKeysRecurring, _indicesRecurring)
        val _existingRecurring: TableInfo = read(connection, "recurring")
        if (!_infoRecurring.equals(_existingRecurring)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recurring(com.tarit.slipledger.data.RecurringRule).
              | Expected:
              |""".trimMargin() + _infoRecurring + """
              |
              | Found:
              |""".trimMargin() + _existingRecurring)
        }
        val _columnsWallets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWallets.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("kind", TableInfo.Column("kind", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("bank", TableInfo.Column("bank", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("accountHint", TableInfo.Column("accountHint", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("openingBalance", TableInfo.Column("openingBalance", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("openingDate", TableInfo.Column("openingDate", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("balanceSet", TableInfo.Column("balanceSet", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("color", TableInfo.Column("color", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWallets.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWallets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWallets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWallets: TableInfo = TableInfo("wallets", _columnsWallets, _foreignKeysWallets,
            _indicesWallets)
        val _existingWallets: TableInfo = read(connection, "wallets")
        if (!_infoWallets.equals(_existingWallets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |wallets(com.tarit.slipledger.data.Wallet).
              | Expected:
              |""".trimMargin() + _infoWallets + """
              |
              | Found:
              |""".trimMargin() + _existingWallets)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions",
        "scanned_images", "category_rules", "categories", "tag_defs", "recurring", "wallets")
  }

  public override fun clearAllTables() {
    super.performClear(false, "transactions", "scanned_images", "category_rules", "categories",
        "tag_defs", "recurring", "wallets")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TxDao::class, TxDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ScannedImageDao::class, ScannedImageDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CategoryRuleDao::class, CategoryRuleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CategoryDao::class, CategoryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(TagDao::class, TagDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RecurringDao::class, RecurringDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(WalletDao::class, WalletDao_Impl.getRequiredConverters())
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

  public override fun txDao(): TxDao = _txDao.value

  public override fun scannedImageDao(): ScannedImageDao = _scannedImageDao.value

  public override fun categoryRuleDao(): CategoryRuleDao = _categoryRuleDao.value

  public override fun categoryDao(): CategoryDao = _categoryDao.value

  public override fun tagDao(): TagDao = _tagDao.value

  public override fun recurringDao(): RecurringDao = _recurringDao.value

  public override fun walletDao(): WalletDao = _walletDao.value
}
