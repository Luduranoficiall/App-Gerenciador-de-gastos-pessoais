package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ExpenseCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val isIncome: Boolean = false
)

object Categories {
    val EXPENSES = listOf(
        ExpenseCategory("food", "Alimentação", Icons.Default.Fastfood, Color(0xFFFF7043)),
        ExpenseCategory("transport", "Transporte", Icons.Default.DirectionsBus, Color(0xFF42A5F5)),
        ExpenseCategory("housing", "Moradia", Icons.Default.Home, Color(0xFFAB47BC)),
        ExpenseCategory("shopping", "Compras", Icons.Default.ShoppingBag, Color(0xFFEC407A)),
        ExpenseCategory("bills", "Contas & Boletos", Icons.Default.Receipt, Color(0xFFFFA726)),
        ExpenseCategory("health", "Saúde", Icons.Default.LocalHospital, Color(0xFFEF5350)),
        ExpenseCategory("leisure", "Lazer", Icons.Default.Movie, Color(0xFF26A69A)),
        ExpenseCategory("education", "Educação", Icons.Default.School, Color(0xFF5C6BC0)),
        ExpenseCategory("fitness", "Esporte & Bem-estar", Icons.Default.FitnessCenter, Color(0xFF7E57C2))
    )

    val INCOMES = listOf(
        ExpenseCategory("salary", "Salário", Icons.Default.Work, Color(0xFF2E7D32), isIncome = true),
        ExpenseCategory("freelance", "Freelance / Extra", Icons.Default.LocalAtm, Color(0xFF00897B), isIncome = true),
        ExpenseCategory("investment", "Rendimentos", Icons.Default.AccountBalance, Color(0xFF1565C0), isIncome = true),
        ExpenseCategory("other_income", "Outras Receitas", Icons.Default.Receipt, Color(0xFF43A047), isIncome = true)
    )

    val ALL = EXPENSES + INCOMES

    fun findById(id: String): ExpenseCategory {
        return ALL.find { it.id == id } ?: EXPENSES.first()
    }
}
