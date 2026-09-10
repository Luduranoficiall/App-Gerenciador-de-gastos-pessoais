package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) {
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun checkAndSeedInitialData() {
        if (accountDao.getAccountCount() == 0) {
            AppDatabase.populateInitialData(accountDao, transactionDao)
        }
    }

    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByAccount(accountId)
    }

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsInRange(startTime, endTime)
    }

    suspend fun insertAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        transactionDao.deleteTransactionsForAccount(account.id)
        accountDao.deleteAccount(account)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
}
