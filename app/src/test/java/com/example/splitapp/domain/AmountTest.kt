package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountTest {
    @Test fun parse_acceptsPlainAndDecimalAmounts() {
        assertEquals(9950L, parseAmountToPaise("99.5"))
        assertEquals(2000L, parseAmountToPaise(" 20 "))
        assertEquals(1L, parseAmountToPaise("0.01"))
        assertEquals(50L, parseAmountToPaise(".5"))
        assertEquals(100L, parseAmountToPaise("1."))
    }

    @Test fun parse_stripsThousandsSeparators() {
        assertEquals(150000L, parseAmountToPaise("1,500"))
        assertEquals(12345678L, parseAmountToPaise("1,23,456.78"))
    }

    @Test fun parse_acceptsExactlyTheMaximum() {
        assertEquals(MAX_AMOUNT_PAISE, parseAmountToPaise("10000000"))
        assertEquals(1_000_000_000L, MAX_AMOUNT_PAISE)
    }

    @Test fun parse_rejectsEverythingElse() {
        listOf(
            "", " ", "0", "0.00", "-5", "+5", "abc", "1e5", "Infinity", "NaN", "1.234", ".", "1.2.3",
            "10000000.01", "99999999999999999999", "5 0", "٣٠",
        ).forEach { assertNull("'$it' should be rejected", parseAmountToPaise(it)) }
    }

    @Test fun storage_isFixedTwoDecimals() {
        assertEquals("1500.00", paiseToStorage(150000))
        assertEquals("99.50", paiseToStorage(9950))
        assertEquals("0.05", paiseToStorage(5))
        assertEquals("0.00", paiseToStorage(0))
    }

    @Test fun storage_readsLegacyValues() {
        assertEquals(150000L, storageToPaise("1500"))
        assertEquals(9950L, storageToPaise("99.5"))
        assertEquals(10000L, storageToPaise("0100"))
        assertEquals(150000L, storageToPaise("1500.00"))
        assertEquals(1235L, storageToPaise("12.345"))   // rounds half up
        assertEquals(1234L, storageToPaise("12.344"))
        assertEquals(0L, storageToPaise("0"))
    }

    // The old app saved the raw typed text whenever Kotlin's toDoubleOrNull() accepted it and it was > 0.
    // Those rows must still show up (and count) after the update.
    @Test fun storage_readsEverythingTheOldAppCouldSave() {
        assertEquals(10000L, storageToPaise("100."))
        assertEquals(50L, storageToPaise(".5"))
        assertEquals(5000L, storageToPaise("+50"))
        assertEquals(100000L, storageToPaise("1e3"))
        assertEquals(2500L, storageToPaise("2.5E1"))
        assertEquals(5000L, storageToPaise("50f"))
        assertEquals(5000L, storageToPaise("50D"))
        assertEquals(1235L, storageToPaise("12.345"))
    }

    @Test fun storage_rejectsGarbage() {
        listOf("", "abc", "Infinity", "NaN", "-5", "1,500", "99999999999999999999", "1e20", ".", "e5")
            .forEach { assertNull("'$it' should be rejected", storageToPaise(it)) }
    }

    @Test fun storage_roundTrips() {
        listOf(0L, 1L, 99L, 100L, 9950L, 150000L, 1_000_000_000L)
            .forEach { assertEquals(it, storageToPaise(paiseToStorage(it))) }
    }

    @Test fun format_showsRupeesWithGroupingAndTwoDecimals() {
        assertEquals("₹1,500.00", formatRupees(150000))
        assertEquals("₹99.50", formatRupees(9950))
        assertEquals("₹0.05", formatRupees(5))
        assertEquals("₹0.00", formatRupees(0))
        assertEquals("₹1,234,567.89", formatRupees(123456789))
        assertEquals("-₹1,500.00", formatRupees(-150000))
    }
}
