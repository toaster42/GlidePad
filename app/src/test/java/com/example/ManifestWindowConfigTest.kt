package com.example

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

/**
 * JVM tests verifying [MainActivity] multi-window and ChromeOS float-mode bounds
 * declared in `AndroidManifest.xml` (AC-8).
 */
class ManifestWindowConfigTest {

    @Test
    fun mainActivity_isResizeable_andDeclaresFloatWindowLayoutBounds() {
        val manifestFile = resolveManifestFile()
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(manifestFile)

        val mainActivity = findMainActivity(document.documentElement)
        assertNotNull("MainActivity declaration not found in ${manifestFile.path}", mainActivity)
        mainActivity!!

        assertEquals(
            "true",
            mainActivity.getAttributeNS(ANDROID_NS, "resizeableActivity"),
        )

        val layoutElement = findDirectChildElement(mainActivity, "layout")
        assertNotNull("MainActivity must declare a child <layout> element", layoutElement)
        layoutElement!!

        assertEquals("240dp", layoutElement.getAttributeNS(ANDROID_NS, "minWidth"))
        assertEquals("180dp", layoutElement.getAttributeNS(ANDROID_NS, "minHeight"))
        assertEquals("900dp", layoutElement.getAttributeNS(ANDROID_NS, "defaultWidth"))
        assertEquals("650dp", layoutElement.getAttributeNS(ANDROID_NS, "defaultHeight"))
    }

    private fun resolveManifestFile(): File {
        return listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).firstOrNull { it.exists() } ?: error("AndroidManifest.xml not found")
    }

    private fun findMainActivity(root: Element): Element? {
        val activities = root.getElementsByTagName("activity")
        for (i in 0 until activities.length) {
            val element = activities.item(i) as? Element ?: continue
            if (element.getAttributeNS(ANDROID_NS, "name") == ".MainActivity") {
                return element
            }
        }
        return null
    }

    private fun findDirectChildElement(parent: Element, tagName: String): Element? {
        var child = parent.firstChild
        while (child != null) {
            if (child is Element && child.tagName == tagName) {
                return child
            }
            child = child.nextSibling
        }
        return null
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
