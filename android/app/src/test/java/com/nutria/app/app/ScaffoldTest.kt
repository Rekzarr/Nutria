package com.nutria.app.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaffoldTest {
    @Test
    fun nativeApplicationNamespaceIsStable() {
        assertEquals("com.nutria.app.app.MainActivity", MainActivity::class.java.name)
    }
}
