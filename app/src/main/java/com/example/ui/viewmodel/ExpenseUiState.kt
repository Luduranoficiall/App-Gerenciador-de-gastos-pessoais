package com.example.ui.viewmodel

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionType

enum class PeriodFilter(val label: String) {
    THIS_MONTH("Este Mês"),
    THIS_WEEK("Esta Semana"),
    ALL("Tudo")
}

data class AccountWithBalance(
    val account: AccountEntity,
    val currentBalance: Double
)

data class CategorySummary(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentage: Float
)

data class DailyExpense(
    val dayLabel: String,
    val expenseAmount: Double,
    val incomeAmount: Double
)

data class ExpenseUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalIncomePeriod: Double = 0.0,
    val totalExpensePeriod: Double = 0.0,
    val periodSavingsRate: Float = 0f,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val dailyExpenses: List<DailyExpense> = emptyList(),
    val selectedPeriod: PeriodFilter = PeriodFilter.THIS_MONTH,
    val selectedTypeFilter: TransactionType? = null,
    val selectedAccountId: Long? = null,
    val searchQuery: String = "",
    val financialInsights: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)
