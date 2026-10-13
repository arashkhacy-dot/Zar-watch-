package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        AssetHoldingEntity::class,
        PriceObservationEntity::class,
        PriceAlertEntity::class,
        PaperTradeOrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ZarWatchDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun holdingDao(): HoldingDao
    abstract fun priceObservationDao(): PriceObservationDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun paperTradingDao(): PaperTradingDao

    companion object {
        @Volatile
        private var INSTANCE: ZarWatchDatabase? = null

        fun getInstance(context: Context): ZarWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZarWatchDatabase::class.java,
                    "zarwatch_portfolio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
