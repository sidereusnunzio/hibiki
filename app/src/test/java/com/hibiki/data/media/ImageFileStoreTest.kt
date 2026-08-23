package com.hibiki.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImageFileStoreTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun installNamedWritesOnceAndSkipsExisting() {
        val store = ImageFileStore(temp.newFolder("images"))
        val first = store.installNamed("umamusume_special_week.jpg", "first".byteInputStream())
        val second = store.installNamed("umamusume_special_week.jpg", "second".byteInputStream())
        assertEquals(first, second)
        assertEquals("first", java.io.File(first).readText())
        assertTrue(store.exists(first))
        assertFalse(store.exists(null))
        assertFalse(store.exists("/missing.jpg"))
    }

    @Test
    fun installNamedOverwriteReplacesExisting() {
        val store = ImageFileStore(temp.newFolder("images"))
        store.installNamed("umamusume_special_week.jpg", "first".byteInputStream())
        val path = store.installNamed(
            "umamusume_special_week.jpg",
            "second".byteInputStream(),
            overwrite = true,
        )
        assertEquals("second", java.io.File(path).readText())
    }
}
