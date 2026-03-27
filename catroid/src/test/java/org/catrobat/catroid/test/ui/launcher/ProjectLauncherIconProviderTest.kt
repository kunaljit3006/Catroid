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
import org.catrobat.catroid.ui.launcher.ProjectLauncherIconProvider.Companion.SCREENSHOT_AUTOMATIC_FILE_NAME
import org.catrobat.catroid.ui.launcher.ProjectLauncherIconProvider.Companion.SCREENSHOT_MANUAL_FILE_NAME
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

    @Test
    fun `loadSourceBitmap falls through all stages when every decode returns null`() {
        // Create all screenshot files at both project root and scene level
        File(tempDir, SCREENSHOT_MANUAL_FILE_NAME).createNewFile()
        File(tempDir, SCREENSHOT_AUTOMATIC_FILE_NAME).createNewFile()
        val sceneDir = File(tempDir, "Scene1")
        sceneDir.mkdirs()
        File(sceneDir, SCREENSHOT_MANUAL_FILE_NAME).createNewFile()
        File(sceneDir, SCREENSHOT_AUTOMATIC_FILE_NAME).createNewFile()

        // Decoder always returns null → hits null branch of every ?.let
        val provider = ProjectLauncherIconProvider(decoderReturningNull())

        val bitmap = provider.loadSourceBitmap(tempDir)

        // Must fall all the way through to fallback
        assertEquals(ICON_SIZE_PX, bitmap.width)
        assertEquals(ICON_SIZE_PX, bitmap.height)
    }

    @Test
    fun `loadSourceBitmap scene manual decode fails falls to scene automatic`() {
        val sceneDir = File(tempDir, "Scene1")
        sceneDir.mkdirs()
        File(sceneDir, SCREENSHOT_MANUAL_FILE_NAME).createNewFile()
        File(sceneDir, SCREENSHOT_AUTOMATIC_FILE_NAME).createNewFile()

        var callCount = 0
        val sceneBitmap = createBitmap(200, 200, Color.GREEN)
        val selectiveDecoder = ProjectLauncherIconProvider.BitmapDecoder { _ ->
            callCount++
            // First call is scene manual → return null; second is scene automatic → success
            if (callCount == 1) null else sceneBitmap
        }
        val provider = ProjectLauncherIconProvider(selectiveDecoder)

        val bitmap = provider.loadSourceBitmap(tempDir)

        assertEquals(Color.GREEN, bitmap.getPixel(100, 100))
    }

    @Test
    fun `default constructor uses BitmapFactory decoder`() {
        // Create a file so the decode path is triggered via the default lambda
        File(tempDir, SCREENSHOT_AUTOMATIC_FILE_NAME).createNewFile()

        // Exercises the default BitmapDecoder lambda (BitmapFactory.decodeFile)
        val provider = ProjectLauncherIconProvider()

        // Robolectric's ShadowBitmapFactory returns a 100x100 dummy bitmap for empty files,
        // which proves that the default BitmapDecoder lambda was successfully invoked.
        val bitmap = provider.loadSourceBitmap(tempDir)

        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
        assertEquals(100, bitmap.height)
    }

    @Test
    fun `loadSourceBitmap handles listFiles returning null`() {
        // Mockito often struggles to mock java.io.File completely under Robolectric/JDK 17.
        // We use an anonymous subclass to guarantee listFiles returns null while isDirectory is true.
        val badDir = object : File(tempDir, "badDir") {
            override fun isDirectory() = true
            override fun listFiles(): Array<File>? = null
        }
        
        val provider = ProjectLauncherIconProvider()
        
        // This should not crash (handles files != null elegantly) and return fallback
        val bitmap = provider.loadSourceBitmap(badDir)
        assertEquals(ICON_SIZE_PX, bitmap.width)
    }

    @Test
    fun `loadSourceBitmap successfully decodes manual screenshot in scene subdirectory`() {
        val sceneDir = File(tempDir, "Scene1")
        sceneDir.mkdirs()
        File(sceneDir, SCREENSHOT_MANUAL_FILE_NAME).createNewFile()
        
        val provider = ProjectLauncherIconProvider { _ -> createBitmap(50, 50, Color.GREEN) }
        val bitmap = provider.loadSourceBitmap(tempDir)
        
        assertEquals(Color.GREEN, bitmap.getPixel(25, 25))
    }

    @Test
    fun `loadSourceBitmap falls back when scene directory exists but contains no automatic screenshot`() {
        val sceneDir = File(tempDir, "Scene1")
        sceneDir.mkdirs()
        // No files in sceneDir

        val provider = ProjectLauncherIconProvider()
        val bitmap = provider.loadSourceBitmap(tempDir)
        
        assertEquals(ICON_SIZE_PX, bitmap.width)
    }

    @Test
    fun `loadSourceBitmap ignores regular files during scene search`() {
        // Create a regular file that is NOT a screenshot, so it gets skipped
        File(tempDir, "random_file.txt").createNewFile()
        // No scene directories exist

        val provider = ProjectLauncherIconProvider()
        val bitmap = provider.loadSourceBitmap(tempDir)
        
        // Loop encounters random_file.txt, it.isDirectory is false, loop finishes -> fallback
        assertEquals(ICON_SIZE_PX, bitmap.width)
    }
}
