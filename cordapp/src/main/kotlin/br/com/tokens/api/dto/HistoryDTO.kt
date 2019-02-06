package br.com.tokens.api.dto

import java.time.Instant

data class HistoryDTO(
        val pages: Int,
        val page: Int,
        val records: List<HistoryRecordDTO>
)

data class HistoryRecordDTO(
        val transactionId: String,
        val from: String,
        val to: String,
        val timeStamp: Instant,
        val value: Int)