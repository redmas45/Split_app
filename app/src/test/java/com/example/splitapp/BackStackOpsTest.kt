package com.example.splitapp

import org.junit.Assert.assertEquals
import org.junit.Test

class BackStackOpsTest {
    // ---- resetTo ----

    @Test fun resetTo_fromEmptyStack() {
        val s = mutableListOf<String>()
        s.resetTo("Login")
        assertEquals(listOf("Login"), s)
    }

    @Test fun resetTo_fromSingleDifferentItem() {
        val s = mutableListOf("GroupSelection")
        s.resetTo("Banned")
        assertEquals(listOf("Banned"), s)
    }

    @Test fun resetTo_fromDeepStackLeavesNothingUnderTheNewKey() {
        // The old ban bug: group + admin panel stayed underneath, so Back bypassed the ban.
        val s = mutableListOf("GroupSelection", "Main", "AdminPanel", "Main")
        s.resetTo("Banned")
        assertEquals(listOf("Banned"), s)
    }

    @Test fun resetTo_whenKeyIsAlreadyInTheStack() {
        val s = mutableListOf("Login", "GroupSelection", "Login")
        s.resetTo("Login")
        assertEquals(listOf("Login"), s)
    }

    @Test fun resetTo_whenStackIsAlreadyJustTheKey() {
        val s = mutableListOf("Login")
        s.resetTo("Login")
        assertEquals(listOf("Login"), s)
    }

    // ---- pushIfNotTop ----

    @Test fun pushIfNotTop_pushesOntoADifferentTop() {
        val s = mutableListOf("GroupSelection")
        s.pushIfNotTop("Main(a)")
        assertEquals(listOf("GroupSelection", "Main(a)"), s)
    }

    @Test fun pushIfNotTop_ignoresAnIdenticalTop_doubleTap() {
        val s = mutableListOf("GroupSelection", "Main(a)")
        s.pushIfNotTop("Main(a)")
        assertEquals(listOf("GroupSelection", "Main(a)"), s)
    }

    @Test fun pushIfNotTop_worksOnEmptyStack() {
        val s = mutableListOf<String>()
        s.pushIfNotTop("Main(a)")
        assertEquals(listOf("Main(a)"), s)
    }

    @Test fun pushIfNotTop_allowsTheSameKeyWhenItIsNotOnTop() {
        val s = mutableListOf("Main(a)", "AdminPanel")
        s.pushIfNotTop("Main(a)")
        assertEquals(listOf("Main(a)", "AdminPanel", "Main(a)"), s)
    }
}
