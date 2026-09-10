package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: AppDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gastos Pessoais", appName)
  }

  @Test
  fun `test insert and read transaction in room database`() = runBlocking {
    val accountDao = db.accountDao()
    val transactionDao = db.transactionDao()

    val accountId = accountDao.insertAccount(
      AccountEntity(
        name = "Conta Corrente",
        type = AccountType.CHECKING.name,
        initialBalance = 1000.0,
        colorHex = 0xFF00796B
      )
    )

    val expenseId = transactionDao.insertTransaction(
      TransactionEntity(
        description = "Supermercado",
        amount = 150.0,
        type = TransactionType.EXPENSE.name,
        categoryId = "food",
        accountId = accountId
      )
    )

    val incomeId = transactionDao.insertTransaction(
      TransactionEntity(
        description = "Salário",
        amount = 3000.0,
        type = TransactionType.INCOME.name,
        categoryId = "salary",
        accountId = accountId
      )
    )

    val allTransactions = transactionDao.getAllTransactions().first()
    assertEquals(2, allTransactions.size)
    assertTrue(allTransactions.any { it.description == "Supermercado" && it.amount == 150.0 })
    assertTrue(allTransactions.any { it.description == "Salário" && it.amount == 3000.0 })

    transactionDao.deleteTransactionById(expenseId)
    val remainingTransactions = transactionDao.getAllTransactions().first()
    assertEquals(1, remainingTransactions.size)
    assertEquals("Salário", remainingTransactions.first().description)
  }
}
