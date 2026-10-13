package com.example.data.providers

import com.example.domain.model.MetalType
import com.example.domain.model.PriceObservation
import com.example.domain.model.PriceQuote
import com.example.domain.model.ProviderStatus
import com.example.domain.model.TimeFrame

interface MarketPriceProvider {
    fun getSourceName(): String
    fun getSourceStatus(): ProviderStatus
    fun getLastUpdatedTimestamp(): Long
    suspend fun fetchGoldPrice(): PriceQuote
    suspend fun fetchSilverPrice(): PriceQuote
    suspend fun fetchHistoricalPrices(metal: MetalType, timeFrame: TimeFrame): List<PriceObservation>
}
