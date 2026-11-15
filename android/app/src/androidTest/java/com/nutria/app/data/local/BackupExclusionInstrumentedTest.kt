package com.nutria.app.data.local

import android.content.pm.ApplicationInfo
import android.content.res.XmlResourceParser
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutria.app.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupExclusionInstrumentedTest {
    @Test
    fun backupsAreDisabledAndBothBackupModesExcludePrivateAppFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)

        val expectedDomains = setOf("root", "file", "database", "sharedpref", "external")
        val legacyExcludedDomains = excludedDomains(context.resources.getXml(R.xml.backup_rules))
        assertEquals(expectedDomains, legacyExcludedDomains)

        val extractionRules = context.resources.getXml(R.xml.data_extraction_rules)
        try {
            var mode: String? = null
            val domainsByMode = mutableMapOf<String, MutableSet<String>>()
            while (extractionRules.eventType != XmlResourceParser.END_DOCUMENT) {
                when (extractionRules.eventType) {
                    XmlResourceParser.START_TAG -> when (extractionRules.name) {
                        "cloud-backup", "device-transfer" -> {
                            mode = extractionRules.name
                            domainsByMode.getOrPut(mode!!) { mutableSetOf() }
                        }
                        "exclude" -> {
                            val currentMode = mode
                            val domain = extractionRules.getAttributeValue(null, "domain")
                            if (currentMode != null && domain != null) {
                                domainsByMode.getValue(currentMode).add(domain)
                            }
                        }
                    }
                    XmlResourceParser.END_TAG -> if (extractionRules.name == mode) mode = null
                }
                extractionRules.next()
            }
            assertEquals(setOf("cloud-backup", "device-transfer"), domainsByMode.keys)
            domainsByMode.values.forEach { assertEquals(expectedDomains, it) }
        } finally {
            extractionRules.close()
        }
    }

    private fun excludedDomains(parser: XmlResourceParser): Set<String> {
        val domains = mutableSetOf<String>()
        try {
            while (parser.eventType != XmlResourceParser.END_DOCUMENT) {
                if (parser.eventType == XmlResourceParser.START_TAG && parser.name == "exclude") {
                    parser.getAttributeValue(null, "domain")?.let(domains::add)
                }
                parser.next()
            }
        } finally {
            parser.close()
        }
        return domains
    }
}
