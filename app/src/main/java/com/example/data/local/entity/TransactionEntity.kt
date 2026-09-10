package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val type: String = TransactionType.EXPENSE.name,
    val categoryId: String = "food",
    val accountId: Long,
    val targetAccountId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val isPaid: Boolean = true,
    val notes: String = ""
)
