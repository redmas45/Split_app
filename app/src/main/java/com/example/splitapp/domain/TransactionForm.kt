package com.example.splitapp.domain

enum class TxKind { Expense, Transfer }

data class FormErrors(
    val amount: String? = null,
    val description: String? = null,
    val people: String? = null,
    val split: String? = null,
) {
    val any: Boolean get() = amount != null || description != null || people != null || split != null
}

sealed class FormResult {
    data class Valid(val draft: TransactionDraft) : FormResult()
    data class Invalid(val errors: FormErrors) : FormResult()
}

const val AMOUNT_ERROR = "Enter an amount like 250 or 99.50"
const val DESCRIPTION_ERROR = "Add a description"
const val PAYER_ERROR = "Choose who paid"
const val PEOPLE_ERROR = "Choose two different people"
const val SPLIT_ERROR = "Pick at least one person to split with"

/** Checks the add-transaction form. For an expense [fromId] is the payer; for a transfer it is the sender. */
fun validateDraft(
    kind: TxKind, amountText: String, description: String, fromId: Int?, toId: Int?,
    splitIds: Set<Int>, memberIds: Set<Int>,
): FormResult {
    val paise = parseAmountToPaise(amountText)
    val amountError = if (paise == null) AMOUNT_ERROR else null

    return when (kind) {
        TxKind.Expense -> {
            val text = description.trim()
            val split = splitIds.filter { it in memberIds }.sorted()
            val errors = FormErrors(
                amount = amountError,
                description = if (text.isEmpty()) DESCRIPTION_ERROR else null,
                people = if (fromId == null || fromId !in memberIds) PAYER_ERROR else null,
                split = if (split.isEmpty()) SPLIT_ERROR else null,
            )
            if (errors.any || paise == null || fromId == null) FormResult.Invalid(errors)
            else FormResult.Valid(TransactionDraft.Expense(text, fromId, paise, split))
        }
        TxKind.Transfer -> {
            val peopleOk = fromId != null && toId != null && fromId != toId && fromId in memberIds && toId in memberIds
            val errors = FormErrors(amount = amountError, people = if (peopleOk) null else PEOPLE_ERROR)
            if (errors.any || paise == null || fromId == null || toId == null) FormResult.Invalid(errors)
            else FormResult.Valid(TransactionDraft.Transfer(fromId, toId, paise))
        }
    }
}
