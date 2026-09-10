package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AccountType(val displayName: String) {
    CHECKING("Conta Corrente"),
    WALLET("Carteira / Dinheiro"),
    CREDIT_CARD("Cartão de Crédito"),
    SAVINGS("Poupança / Reserva");

    fun getIcon(): ImageVector {
        return when (this) {
            CHECKING -> Icons.Default.AccountBalance
            WALLET -> Icons.Default.Payments
            CREDIT_CARD -> Icons.Default.CreditCard
            SAVINGS -> Icons.Default.Savings
        }
    }
}
