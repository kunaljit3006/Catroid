/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2025 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.test.ui.launcher

import android.graphics.Bitmap
import android.graphics.Color
import org.catrobat.catroid.ui.launcher.ProjectLauncherIconProvider
import org.catrobat.catroid.ui.launcher.ProjectLauncherIconProvider.Companion.ICON_SIZE_PX
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProjectLauncherIconProviderTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = createTempDir("providerTest")
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // --- Helper ---

    private fun createBitmap(width: Int, height: Int, color: Int = Color.RED): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }

    private fun decoderReturning(bitmap: Bitmap): ProjectLauncherIconProvider.BitmapDecoder =
        ProjectLauncherIconProvider.BitmapDecoder { bitmap }

    private fun decoderReturningNull(): ProjectLauncherIconProvider.BitmapDecoder =
        ProjectLauncherIconProvider.BitmapDecoder { null }

    // ================================================================
    // RED tests — these expose the screenshot-path bug.
    // Catroid stores automatic_screenshot.png in the project root,
    // NOT inside scenes/{scene}/screenshots/.
    // ================================================================

    @Test
    fun `loadSourceBitmap finds automatic_screenshot in project root`() {
        // Catroid saves screenshots directly in the project directory
        File(tempDir, "automatic_screenshot.png").createNewFile()

        val expected = createBitmap(200, 200, Color.CYAN)
        val provider = ProjectLauncherIconProvider(decoderReturning(expected))

        val bitmap = provider.loadSourceBitmap(tempDir)

        // Should load the screenshot, NOT return the grey fallback
        assertEquals(
            "Provider must find automatic_screenshot.png in the project root",
            Color.CYAN, bitmap.getPixel(100, 100)
        )
    }

    @Test
    fun `loadSourceBitmap finds manual_screenshot in project root`() {
        File(tempDir, "manual_screenshot.png").createNewFile()

        val expected = createBitmap(200, 200, Color.MAGENTA)
        val provider = ProjectLauncherIconProvider(decoderReturning(expected))

        val bitmap = provider.loadSourceBitmap(tempDir)

        assertEquals(
            "Provider must find manual_screenshot.png in the project root",
            Color.MAGENTA, bitmap.getPixel(100, 100)
        )
    }

    @Test
    fun `loadSourceBitmap finds screenshot in scene subdirectory`() {
        // Scene screenshots live directly in {projectDir}/{sceneName}/
        val sceneDir = File(tempDir, "Scene1")
        sceneDir.mkdirs()
        File(sceneDir, "automatic_screenshot.png").createNewFile()

        val expected = createBitmap(200, 200, Color.YELLOW)
        val provider = ProjectLauncherIconProvider(decoderReturning(expected))

        val bitmap = provider.loadSourceBitmap(tempDir)

        assertEquals(
            "Provider must find screenshot in scene subdirectory",
            Color.YELLOW, bitmap.getPixel(100, 100)
        )
    }

    @Test
    fun `loadSourceBitmap prefers manual over automatic screenshot`() {
        File(tempDir, "manual_screenshot.png").createNewFile()
        File(tempDir, "automatic_screenshot.png").createNewFile()

        val manualBitmap = createBitmap(100, 100, Color.BLUE)
        val provider = ProjectLauncherIconProvider(decoderReturning(manualBitmap))

        val bitmap = provider.loadSourceBitmap(tempDir)

        assertEquals(
            "manual_screenshot.png should be preferred",
            Color.BLUE, bitmap.getPixel(50, 50)
        )
    }

    // ================================================================
    // Existing tests — these still pass against the current code
    // ================================================================

    @Test
    fun `getIconForProject returns fallback icon when no screenshots exist`() {
        val provider = ProjectLauncherIconProvider(decoderReturningNull())

        val icon = provider.getIconForProject(tempDir)

        assertNotNull(icon)
        assertEquals(ICON_SIZE_PX, icon.width)
        assertEquals(ICON_SIZE_PX, icon.height)
    }

    @Test
    fun `centreSquareCrop produces expected size from landscape source`() {
        val source = createBitmap(400, 200)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop produces expected size from portrait source`() {
        val source = createBitmap(200, 400)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop produces expected size from square source`() {
        val source = createBitmap(300, 300)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop with source already at ICON_SIZE does not rescale`() {
        val source = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop does not crash on 1x1 bitmap`() {
        val source = createBitmap(1, 1)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop does not crash on odd dimensions (3x7)`() {
        val source = createBitmap(3, 7)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop does not crash on odd dimensions (7x3)`() {
        val source = createBitmap(7, 3)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `centreSquareCrop does not crash on very large dimensions`() {
        val source = createBitmap(2000, 1000)
        val provider = ProjectLauncherIconProvider()

        val cropped = provider.centreSquareCrop(source, ICON_SIZE_PX)

        assertEquals(ICON_SIZE_PX, cropped.width)
        assertEquals(ICON_SIZE_PX, cropped.height)
    }

    @Test
    fun `applyRoundedCorners preserves dimensions`() {
        val source = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Color.GREEN)
        val provider = ProjectLauncherIconProvider()

        val result = provider.applyRoundedCorners(source, 20f)

        assertEquals(ICON_SIZE_PX, result.width)
        assertEquals(ICON_SIZE_PX, result.height)
    }

    @Test
    fun `applyRoundedCorners produces transparent corners`() {
        val source = createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Color.GREEN)
        val provider = ProjectLauncherIconProvider()

        val result = provider.applyRoundedCorners(source, 20f)

        val topLeftPixel = result.getPixel(0, 0)
        assertEquals("Top-left pixel should be transparent after rounding",
            0, Color.alpha(topLeftPixel))
    }

    @Test
    fun `createFallbackBitmap returns correct dimensions`() {
        val provider = ProjectLauncherIconProvider()

        val fallback = provider.createFallbackBitmap()

        assertEquals(ICON_SIZE_PX, fallback.width)
        assertEquals(ICON_SIZE_PX, fallback.height)
    }

    @Test
    fun `loadSourceBitmap returns fallback for empty project directory`() {
        val provider = ProjectLauncherIconProvider(decoderReturningNull())

        val bitmap = provider.loadSourceBitmap(tempDir)

        assertNotNull(bitmap)
        assertEquals(ICON_SIZE_PX, bitmap.width)
        assertEquals(ICON_SIZE_PX, bitmap.height)
    }

    @Test
    fun `loadSourceBitmap returns fallback for non-existent directory`() {
        val nonExistent = File(tempDir, "does_not_exist")
        val provider = ProjectLauncherIconProvider(decoderReturningNull())

        val bitmap = provider.loadSourceBitmap(nonExistent)

        assertNotNull(bitmap)
        assertEquals(ICON_SIZE_PX, bitmap.width)
    }
}
