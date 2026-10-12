package com.tarit.slipledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Tx::class, ScannedImage::class, CategoryRule::class, CategoryDef::class, TagDef::class, RecurringRule::class, Wallet::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txDao(): TxDao
    abstract fun scannedImageDao(): ScannedImageDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun recurringDao(): RecurringDao
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_rules` " +
                        "(`key` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL, `builtIn` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tag_defs` (`name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`name`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `amountSatang` INTEGER NOT NULL, `category` TEXT NOT NULL, " +
                        "`counterparty` TEXT NOT NULL, `note` TEXT NOT NULL, `dayOfMonth` INTEGER NOT NULL, " +
                        "`lastRun` TEXT NOT NULL, `active` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `walletId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `toWalletId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `needsReview` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `recurring` ADD COLUMN `walletId` INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `bank` TEXT NOT NULL, `accountHint` TEXT NOT NULL, " +
                        "`openingBalance` INTEGER NOT NULL, `openingDate` INTEGER NOT NULL, `balanceSet` INTEGER NOT NULL, " +
                        "`color` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "slipledger.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
