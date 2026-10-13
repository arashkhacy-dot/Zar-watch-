package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE metal = :metal ORDER BY timestamp DESC")
    fun getTransactionsByMetal(metal: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsAscSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE metal = :metal ORDER BY timestamp ASC")
    suspend fun getTransactionsByMetalAscSync(metal: String): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface HoldingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHolding(holding: AssetHoldingEntity)

    @Query("SELECT * FROM asset_holdings WHERE metal = :metal")
    fun getHolding(metal: String): Flow<AssetHoldingEntity?>

    @Query("SELECT * FROM asset_holdings")
    fun getAllHoldings(): Flow<List<AssetHoldingEntity>>

    @Query("SELECT * FROM asset_holdings WHERE metal = :metal")
    suspend fun getHoldingSync(metal: String): AssetHoldingEntity?

    @Query("SELECT * FROM asset_holdings")
    suspend fun getAllHoldingsSync(): List<AssetHoldingEntity>

    @Query("DELETE FROM asset_holdings")
    suspend fun clearAll()
}

@Dao
interface PriceObservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(observation: PriceObservationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(observations: List<PriceObservationEntity>)

    @Query("SELECT * FROM price_observations WHERE metal = :metal AND timestamp >= :since ORDER BY timestamp ASC")
    fun getObservations(metal: String, since: Long): Flow<List<PriceObservationEntity>>

    @Query("SELECT * FROM price_observations WHERE metal = :metal AND timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getObservationsSync(metal: String, since: Long): List<PriceObservationEntity>

    @Query("SELECT * FROM price_observations WHERE metal = :metal ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestObservation(metal: String): PriceObservationEntity?

    @Query("DELETE FROM price_observations WHERE timestamp < :cutoff")
    suspend fun pruneOldObservations(cutoff: Long)
}

@Dao
interface PriceAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: PriceAlertEntity): Long

    @Update
    suspend fun update(alert: PriceAlertEntity)

    @Delete
    suspend fun delete(alert: PriceAlertEntity)

    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1")
    suspend fun getActiveAlertsSync(): List<PriceAlertEntity>
}

@Dao
interface PaperTradingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: PaperTradeOrderEntity): Long

    @Query("SELECT * FROM paper_trades ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<PaperTradeOrderEntity>>

    @Query("DELETE FROM paper_trades")
    suspend fun clearAll()
}
