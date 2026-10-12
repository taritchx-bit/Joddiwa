package com.tarit.slipledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TxDao {
    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC")
    fun observeRange(from: Long, to: Long): Flow<List<Tx>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Tx>>

    @Query("SELECT * FROM transactions ORDER BY timestamp")
    suspend fun all(): List<Tx>

    /** Timestamps since [from], newest first, for streak counting. */
    @Query("SELECT timestamp FROM transactions WHERE timestamp >= :from ORDER BY timestamp DESC")
    fun observeTimestampsSince(from: Long): Flow<List<Long>>

    /** Returns -1 when a row with the same sourceKey already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tx: Tx): Long

    @Update
    suspend fun update(tx: Tx)

    @Delete
    suspend fun delete(tx: Tx)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query(
        "SELECT * FROM transactions WHERE amountSatang = :amount AND timestamp BETWEEN :from AND :to"
    )
    suspend fun findSimilar(amount: Long, from: Long, to: Long): List<Tx>

    @Query(
        "SELECT category, SUM(amountSatang) AS total FROM transactions " +
            "WHERE type = 'EXPENSE' AND timestamp >= :from AND timestamp < :to GROUP BY category"
    )
    suspend fun expenseByCategory(from: Long, to: Long): List<CategoryTotal>

    @Query("UPDATE transactions SET category = :category WHERE counterparty = :counterparty AND type = :type")
    suspend fun setCategoryFor(counterparty: String, type: TxType, category: String)

    @Query("UPDATE transactions SET category = :new WHERE category = :old")
    suspend fun renameCategory(old: String, new: String)

    @Query("SELECT * FROM transactions WHERE tags LIKE '%' || :tag || '%'")
    suspend fun withTagLike(tag: String): List<Tx>

    @Query("SELECT * FROM transactions WHERE needsReview = 1 ORDER BY timestamp DESC")
    fun observeNeedsReview(): Flow<List<Tx>>

    @Query("UPDATE transactions SET needsReview = 0 WHERE id IN (:ids)")
    suspend fun markReviewed(ids: List<Long>)

    @Query("SELECT COALESCE(SUM(amountSatang), 0) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :from AND timestamp < :to")
    suspend fun expenseTotal(from: Long, to: Long): Long
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY kind, sortOrder")
    fun observe(): Flow<List<CategoryDef>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<CategoryDef>)

    @Insert
    suspend fun insert(def: CategoryDef): Long

    @Update
    suspend fun update(list: List<CategoryDef>)

    @Delete
    suspend fun delete(def: CategoryDef)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_defs ORDER BY sortOrder")
    fun observe(): Flow<List<TagDef>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tag_defs")
    suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagDef)

    @Update
    suspend fun update(list: List<TagDef>)

    @Delete
    suspend fun delete(tag: TagDef)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY dayOfMonth")
    fun observe(): Flow<List<RecurringRule>>

    @Query("SELECT * FROM recurring WHERE active = 1")
    suspend fun active(): List<RecurringRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRule): Long

    @Delete
    suspend fun delete(rule: RecurringRule)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY sortOrder, id")
    fun observe(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets ORDER BY sortOrder, id")
    suspend fun all(): List<Wallet>

    @Insert
    suspend fun insert(wallet: Wallet): Long

    @Update
    suspend fun update(list: List<Wallet>)

    @Delete
    suspend fun delete(wallet: Wallet)

    @Query("UPDATE transactions SET walletId = NULL WHERE walletId = :id")
    suspend fun detachFrom(id: Long)

    @Query("UPDATE transactions SET toWalletId = NULL WHERE toWalletId = :id")
    suspend fun detachTo(id: Long)
}

@Dao
interface CategoryRuleDao {
    @Query("SELECT category FROM category_rules WHERE `key` = :key")
    suspend fun find(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CategoryRule)

    @Query("UPDATE category_rules SET category = :new WHERE category = :old")
    suspend fun renameCategory(old: String, new: String)

    @Query("DELETE FROM category_rules WHERE category = :category")
    suspend fun deleteFor(category: String)

    @Query("DELETE FROM category_rules")
    suspend fun clear()
}

@Dao
interface ScannedImageDao {
    @Query("SELECT mediaId FROM scanned_images")
    suspend fun allIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ScannedImage)

    @Query("DELETE FROM scanned_images")
    suspend fun clear()
}
