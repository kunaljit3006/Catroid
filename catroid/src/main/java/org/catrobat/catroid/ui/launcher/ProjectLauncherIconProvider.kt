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

package org.catrobat.catroid.ui.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import java.io.File

/**
 * Provides launcher icons for Catrobat projects by looking for thumbnails or scene screenshots.
 *
 * Icons are returned as 160x160px square Bitmaps with rounded corners.
 */
class ProjectLauncherIconProvider(
    private val bitmapDecoder: BitmapDecoder = BitmapDecoder { path ->
        android.graphics.BitmapFactory.decodeFile(path)
    }
) {

    /** Abstraction for bitmap decoding to allow JVM unit testing without Robolectric. */
    fun interface BitmapDecoder {
        fun decode(path: String): Bitmap?
    }

    /**
     * Loads or creates an icon for the given project directory.
     * Prefers `thumbnail.png`, then the first scene's screenshot.
     * Returns a 160x160 fallback if nothing is found.
     */
    fun getIconForProject(projectDir: File): Bitmap {
        val source = loadSourceBitmap(projectDir)
        val cropped = centreSquareCrop(source, ICON_SIZE_PX)
        return applyRoundedCorners(cropped, CORNER_RADIUS_PX)
    }

    internal fun loadSourceBitmap(projectDir: File): Bitmap {
        // 1. Try manual_screenshot.png in project root
        val manualFile = File(projectDir, SCREENSHOT_MANUAL_FILE_NAME)
        if (manualFile.exists()) {
            bitmapDecoder.decode(manualFile.absolutePath)?.let { return it }
        }

        // 2. Try automatic_screenshot.png in project root
        val automaticFile = File(projectDir, SCREENSHOT_AUTOMATIC_FILE_NAME)
        if (automaticFile.exists()) {
            bitmapDecoder.decode(automaticFile.absolutePath)?.let { return it }
        }

        // 3. Try first scene sub-directory for screenshots
        if (projectDir.isDirectory) {
            val files = projectDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        val sceneManual = File(file, SCREENSHOT_MANUAL_FILE_NAME)
                        if (sceneManual.exists()) {
                            bitmapDecoder.decode(sceneManual.absolutePath)?.let { return it }
                        }
                        val sceneAutomatic = File(file, SCREENSHOT_AUTOMATIC_FILE_NAME)
                        if (sceneAutomatic.exists()) {
                            bitmapDecoder.decode(sceneAutomatic.absolutePath)?.let { return it }
                        }
                        break // We only check the first directory, matching firstOrNull logic
                    }
                }
            }
        }

        // 4. Fallback
        return createFallbackBitmap()
    }

    internal fun centreSquareCrop(source: Bitmap, size: Int): Bitmap {
        val width = source.width
        val height = source.height
        val minDim = minOf(width, height)

        val left = (width - minDim) / 2
        val top = (height - minDim) / 2

        val cropped = Bitmap.createBitmap(source, left, top, minDim, minDim)
        return if (minDim != size) {
            Bitmap.createScaledBitmap(cropped, size, size, true)
        } else {
            cropped
        }
    }

    internal fun applyRoundedCorners(source: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, source.width, source.height)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)

        return output
    }

    internal fun createFallbackBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE_PX, ICON_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(0f, 0f, ICON_SIZE_PX.toFloat(), ICON_SIZE_PX.toFloat(), paint)

        return bitmap
    }

    companion object {
        const val ICON_SIZE_PX = 160
        const val CORNER_RADIUS_PX = 20f
        const val SCREENSHOT_AUTOMATIC_FILE_NAME = "automatic_screenshot.png"
        const val SCREENSHOT_MANUAL_FILE_NAME = "manual_screenshot.png"
    }
}
