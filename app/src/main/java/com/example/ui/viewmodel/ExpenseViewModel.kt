package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.Categories
import com.example.data.model.TransactionType
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.THIS_MONTH)
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            try {
                repository.checkAndSeedInitialData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.allAccounts,
        repository.allTransactions,
        _selectedPeriod,
        _selectedTypeFilter,
        _selectedAccountId,
        _searchQuery,
        _userMessage
    ) { args: Array<Any?> ->
        val accounts = args[0] as List<AccountEntity>
        val transactions = args[1] as List<TransactionEntity>
        val period = args[2] as PeriodFilter
        val typeFilter = args[3] as TransactionType?
        val accountId = args[4] as Long?
        val query = args[5] as String
        val message = args[6] as String?

        processState(
            accounts = accounts,
            transactions = transactions,
            period = period,
            typeFilter = typeFilter,
            accountId = accountId,
            query = query,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    private fun processState(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        period: PeriodFilter,
        typeFilter: TransactionType?,
        accountId: Long?,
        query: String,
        message: String?
    ): ExpenseUiState {
        // Calculate account current balances
        val accountWithBalances = accounts.map { acc ->
            var currentBal = acc.initialBalance
            for (tx in transactions) {
                if (tx.accountId == acc.id) {
                    when (tx.type) {
                        TransactionType.INCOME.name -> currentBal += tx.amount
                        TransactionType.EXPENSE.name -> currentBal -= tx.amount
                        TransactionType.TRANSFER.name -> currentBal -= tx.amount
                    }
                }
                if (tx.targetAccountId == acc.id && tx.type == TransactionType.TRANSFER.name) {
                    currentBal += tx.amount
                }
            }
            AccountWithBalance(account = acc, currentBalance = currentBal)
        }

        // Net balance across accounts (checking, wallet, savings + credit card balance)
        val totalBalance = accountWithBalances.sumOf {
            if (it.account.type == AccountType.CREDIT_CARD.name && it.currentBalance < 0) {
                it.currentBalance
            } else {
                it.currentBalance
            }
        }

        // Period filter boundary
        val (periodStart, periodEnd) = getPeriodRange(period)
        val periodTransactions = transactions.filter {
            it.dateMillis in periodStart..periodEnd
        }

        val totalIncomePeriod = periodTransactions
            .filter { it.type == TransactionType.INCOME.name }
            .sumOf { it.amount }

        val totalExpensePeriod = periodTransactions
            .filter { it.type == TransactionType.EXPENSE.name }
            .sumOf { it.amount }

        val periodSavingsRate = if (totalIncomePeriod > 0) {
            val net = totalIncomePeriod - totalExpensePeriod
            if (net > 0) ((net / totalIncomePeriod) * 100).toFloat() else 0f
        } else 0f

        // Category breakdown for expenses in this period
        val expenseTxList = periodTransactions.filter { it.type == TransactionType.EXPENSE.name }
        val categoryGroups = expenseTxList.groupBy { it.categoryId }
        val categorySummaries = categoryGroups.map { (catId, txList) ->
            val cat = Categories.findById(catId)
            val sum = txList.sumOf { it.amount }
            val pct = if (totalExpensePeriod > 0) (sum / totalExpensePeriod).toFloat() else 0f
            CategorySummary(category = cat, totalAmount = sum, percentage = pct)
        }.sortedByDescending { it.totalAmount }

        // Daily expenses for the last 7 days chart
        val dailyExpenses = calculateDailyStats(transactions)

        // Filtered transactions for the list
        val filtered = transactions.filter { tx ->
            val matchesPeriod = tx.dateMillis in periodStart..periodEnd
            val matchesType = typeFilter == null || tx.type == typeFilter.name
            val matchesAccount = accountId == null || tx.accountId == accountId || tx.targetAccountId == accountId
            val matchesQuery = query.isBlank() ||
                    tx.description.contains(query, ignoreCase = true) ||
                    Categories.findById(tx.categoryId).name.contains(query, ignoreCase = true)
            matchesPeriod && matchesType && matchesAccount && matchesQuery
        }

        val insights = generateInsights(totalIncomePeriod, totalExpensePeriod, categorySummaries)

        return ExpenseUiState(
            accounts = accountWithBalances,
            transactions = transactions,
            filteredTransactions = filtered,
            totalBalance = totalBalance,
            totalIncomePeriod = totalIncomePeriod,
            totalExpensePeriod = totalExpensePeriod,
            periodSavingsRate = periodSavingsRate,
            categorySummaries = categorySummaries,
            dailyExpenses = dailyExpenses,
            selectedPeriod = period,
            selectedTypeFilter = typeFilter,
            selectedAccountId = accountId,
            searchQuery = query,
            financialInsights = insights,
            isLoading = false,
            userMessage = message
        )
    }

    private fun getPeriodRange(period: PeriodFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis
        return when (period) {
            PeriodFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Pair(start, Long.MAX_VALUE)
            }
            PeriodFilter.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Pair(start, Long.MAX_VALUE)
            }
            PeriodFilter.ALL -> Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun calculateDailyStats(transactions: List<TransactionEntity>): List<DailyExpense> {
        val result = mutableListOf<DailyExpense>()
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale("pt", "BR"))

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = targetCal.timeInMillis
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000L) - 1

            val dayTx = transactions.filter { it.dateMillis in startOfDay..endOfDay }
            val expense = dayTx.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }
            val income = dayTx.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val label = if (i == 0) "Hoje" else dayFormat.format(Date(startOfDay)).replaceFirstChar { it.uppercase() }

            result.add(DailyExpense(dayLabel = label, expenseAmount = expense, incomeAmount = income))
        }
        return result
    }

    private fun generateInsights(
        income: Double,
        expense: Double,
        categories: List<CategorySummary>
    ): List<String> {
        val list = mutableListOf<String>()
        if (income > 0) {
            val net = income - expense
            if (net > 0) {
                val pct = ((net / income) * 100).toInt()
                list.add("Excelente! Você está poupando $pct% das suas receitas neste período.")
            } else {
                list.add("Atenção: Os gastos superaram as receitas no período selecionado.")
            }
        }
        if (categories.isNotEmpty()) {
            val top = categories.first()
            val pct = (top.percentage * 100).toInt()
            list.add("Maior categoria de gasto: ${top.category.name} representando $pct% do total.")
        }
        list.add("Dica: Registre compras no momento em que acontecem para manter seu saldo em dia.")
        return list
    }

    fun setPeriodFilter(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setAccountFilter(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        accountId: Long,
        targetAccountId: Long? = null,
        dateMillis: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    description = description.trim(),
                    amount = amount,
                    type = type.name,
                    categoryId = categoryId,
                    accountId = accountId,
                    targetAccountId = targetAccountId,
                    dateMillis = dateMillis,
                    notes = notes.trim()
                )
            )
            _userMessage.value = "Transação adicionada com sucesso!"
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _userMessage.value = "Transação atualizada!"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _userMessage.value = "Transação excluída!"
        }
    }

    fun addAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name.trim(),
                    type = type.name,
                    initialBalance = initialBalance,
                    colorHex = colorHex
                )
            )
            _userMessage.value = "Conta cadastrada com sucesso!"
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _userMessage.value = "Conta e transações vinculadas foram removidas."
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application, kotlinx.coroutines.GlobalScope)
                    val repository = ExpenseRepository(db.accountDao(), db.transactionDao())
                    return ExpenseViewModel(application, repository) as T
                }
            }
    }
}
