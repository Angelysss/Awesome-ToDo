package com.awesometodo.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun comparesSemanticVersionsNumerically() {
        assertTrue(UpdateChecker.compareVersions("0.10.0", "0.2.0") > 0)
        assertTrue(UpdateChecker.compareVersions("1.0.0", "1.0.1") < 0)
        assertEquals(0, UpdateChecker.compareVersions("v0.2.0", "0.2"))
    }

    @Test fun ignoresPrereleaseSuffixForNumericComparison() {
        assertEquals(0, UpdateChecker.compareVersions("0.2.0-beta.1", "0.2.0"))
    }
}
