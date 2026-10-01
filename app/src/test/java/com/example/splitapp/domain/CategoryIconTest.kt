package com.example.splitapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryIconTest {
    @Test fun houseRentIsHome() = assertEquals("🏠", getCategoryIcon("House rent"))
    @Test fun movieTicketsAreEntertainment() = assertEquals("🎟️", getCategoryIcon("Movie tickets"))
    @Test fun creditCardFeeIsNotTravel() = assertNotEquals("🚗", getCategoryIcon("Credit card fee"))
    @Test fun greatDinnerIsFoodBecauseOfDinner() = assertEquals("🍔", getCategoryIcon("Great dinner"))
    @Test fun showerGelIsNotEntertainment() = assertNotEquals("🎟️", getCategoryIcon("Shower gel"))
    @Test fun greatAloneIsNotFood() = assertEquals("📄", getCategoryIcon("Great"))

    @Test fun otherCategoriesStillWork() {
        assertEquals("🚗", getCategoryIcon("Cab to airport"))
        assertEquals("🚗", getCategoryIcon("Bus fares"))
        assertEquals("🛍️", getCategoryIcon("Groceries"))
        assertEquals("🏠", getCategoryIcon("Electricity bill"))
        assertEquals("🍔", getCategoryIcon("TEA!"))
        assertEquals("📄", getCategoryIcon(""))
    }
}
