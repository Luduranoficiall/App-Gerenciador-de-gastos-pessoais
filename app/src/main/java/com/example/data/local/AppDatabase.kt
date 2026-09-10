package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.model.AccountType
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [AccountEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gastos_pessoais_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.accountDao(), database.transactionDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            accountDao: AccountDao,
            transactionDao: TransactionDao
        ) {
            val acc1 = AccountEntity(
                id = 1,
                name = "Banco Principal",
                type = AccountType.CHECKING.name,
                initialBalance = 2450.00,
                colorHex = 0xFF00796B
            )
            val acc2 = AccountEntity(
                id = 2,
                name = "Carteira Física",
                type = AccountType.WALLET.name,
                initialBalance = 180.00,
                colorHex = 0xFF388E3C
            )
            val acc3 = AccountEntity(
                id = 3,
                name = "Cartão de Crédito",
                type = AccountType.CREDIT_CARD.name,
                initialBalance = 0.00,
                colorHex = 0xFF7B1FA2
            )
            val acc4 = AccountEntity(
                id = 4,
                name = "Reserva de Emergência",
                type = AccountType.SAVINGS.name,
                initialBalance = 5200.00,
                colorHex = 0xFF0288D1
            )

            accountDao.insertAccounts(listOf(acc1, acc2, acc3, acc4))

            val now = System.currentTimeMillis()
            val dayMillis = 24 * 60 * 60 * 1000L

            val initialTransactions = listOf(
                TransactionEntity(
                    description = "Salário Mensal",
                    amount = 4500.00,
                    type = TransactionType.INCOME.name,
                    categoryId = "salary",
                    accountId = 1,
                    dateMillis = now - (2 * dayMillis),
                    isPaid = true,
                    notes = "Pagamento referente ao mês"
                ),
                TransactionEntity(
                    description = "Supermercado Semanal",
                    amount = 342.50,
                    type = TransactionType.EXPENSE.name,
                    categoryId = "food",
                    accountId = 1,
                    dateMillis = now - (1 * dayMillis),
                    isPaid = true,
                    notes = "Compras no mercado central"
                ),
                TransactionEntity(
                    description = "Abastecimento Carro",
                    amount = 120.00,
                    type = TransactionType.EXPENSE.name,
                    categoryId = "transport",
                    accountId = 3,
                    dateMillis = now - (3 * dayMillis),
                    isPaid = true,
                    notes = "Gasolina comum"
                ),
                TransactionEntity(
                    description = "Almoço de Domingo",
                    amount = 65.00,
                    type = TransactionType.EXPENSE.name,
                    categoryId = "food",
                    accountId = 2,
                    dateMillis = now - (4 * dayMillis),
                    isPaid = true,
                    notes = "Restaurante com família"
                ),
                TransactionEntity(
                    description = "Conta de Energia Elétrica",
                    amount = 185.30,
                    type = TransactionType.EXPENSE.name,
                    categoryId = "bills",
                    accountId = 1,
                    dateMillis = now - (5 * dayMillis),
                    isPaid = true,
                    notes = "Vencimento dia 10"
                ),
                TransactionEntity(
                    description = "Freelance Design",
                    amount = 850.00,
                    type = TransactionType.INCOME.name,
                    categoryId = "freelance",
                    accountId = 1,
                    dateMillis = now - (6 * dayMillis),
                    isPaid = true,
                    notes = "Projeto de identidade visual"
                ),
                TransactionEntity(
                    description = "Farmácia & Remédios",
                    amount = 48.90,
                    type = TransactionType.EXPENSE.name,
                    categoryId = "health",
                    accountId = 1,
                    dateMillis = now - (7 * dayMillis),
                    isPaid = true,
                    notes = "Vitaminas"
                )
            )

            transactionDao.insertTransactions(initialTransactions)
        }
    }
}
