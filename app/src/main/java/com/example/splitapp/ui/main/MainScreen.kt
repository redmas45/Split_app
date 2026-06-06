package com.example.splitapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey

data class PersonState(
    val id: Int,
    val name: String,
    val amount: String
)

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var persons by remember {
        mutableStateOf(
            listOf(
                PersonState(1, "Person 1", ""),
                PersonState(2, "Person 2", "")
            )
        )
    }
    var nextId by remember { mutableIntStateOf(3) }
    var splitMode by remember { mutableStateOf("Split equally") }
    var resultText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Expense Splitter", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(persons) { index, person ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = person.name,
                        onValueChange = { newName ->
                            val newList = persons.toMutableList()
                            newList[index] = person.copy(name = newName)
                            persons = newList
                        },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = person.amount,
                        onValueChange = { newAmount ->
                            val newList = persons.toMutableList()
                            newList[index] = person.copy(amount = newAmount)
                            persons = newList
                        },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (persons.size > 2) {
                                val newList = persons.toMutableList()
                                newList.removeAt(index)
                                persons = newList
                            }
                        },
                        enabled = persons.size > 2
                    ) {
                        Text("X", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            item {
                if (persons.size < 5) {
                    TextButton(onClick = {
                        val newList = persons.toMutableList()
                        newList.add(PersonState(nextId, "Person $nextId", ""))
                        nextId++
                        persons = newList
                    }) {
                        Text("+ Add person")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = splitMode == "Split equally",
                onClick = { splitMode = "Split equally" }
            )
            Text("Split equally")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = splitMode == "Settle debts",
                onClick = { splitMode = "Settle debts" }
            )
            Text("Settle debts")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                resultText = calculateSplit(persons, splitMode)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate split")
        }
        
        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resultText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

fun calculateSplit(persons: List<PersonState>, mode: String): String {
    val amounts = persons.map { it.amount.toDoubleOrNull() ?: 0.0 }
    val total = amounts.sum()
    if (total <= 0.0) return "Total is 0. Nothing to split."

    val average = total / persons.size
    val balances = mutableListOf<Pair<String, Double>>()
    
    var details = ""
    if (mode == "Split equally") {
        details += "Total: ₹%.2f\n".format(total)
        details += "Each person should pay: ₹%.2f\n\n".format(average)
    }

    for (i in persons.indices) {
        val balance = amounts[i] - average
        balances.add(persons[i].name to balance)
        if (mode == "Split equally") {
            if (balance > 0.01) {
                details += "${persons[i].name} overpaid by ₹%.2f\n".format(balance)
            } else if (balance < -0.01) {
                details += "${persons[i].name} owes ₹%.2f\n".format(-balance)
            }
        }
    }
    
    if (mode == "Split equally") {
        details += "\nTransactions to settle up:\n"
    } else {
        details += "Minimal transactions to settle debts:\n"
    }

    val debtors = balances.filter { it.second < -0.01 }.map { it.first to -it.second }.toMutableList()
    val creditors = balances.filter { it.second > 0.01 }.toMutableList()

    debtors.sortByDescending { it.second }
    creditors.sortByDescending { it.second }

    var d = 0
    var c = 0
    var transactionsFound = false

    while (d < debtors.size && c < creditors.size) {
        val debtor = debtors[d]
        val creditor = creditors[c]

        val amount = minOf(debtor.second, creditor.second)
        if (amount > 0.01) {
            details += "${debtor.first} owes ${creditor.first} ₹%.2f\n".format(amount)
            transactionsFound = true
        }

        debtors[d] = debtor.first to (debtor.second - amount)
        creditors[c] = creditor.first to (creditor.second - amount)

        if (debtors[d].second < 0.01) d++
        if (creditors[c].second < 0.01) c++
    }

    if (!transactionsFound) {
        details += "Everyone is settled up!"
    }

    return details.trimEnd()
}
