/*
 * Copyright © 2013, Adam Scarr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.vektah.codeglance.render

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.tree.IElementType
import com.intellij.util.ui.UIUtil
import net.vektah.codeglance.config.Config
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * A rendered minimap of a document
 */
class Minimap(private val config: Config) {
    var img: BufferedImage? = null
    private val logger = Logger.getInstance(javaClass)

    /**
     * Scans over the entire document once to work out the required dimensions then rebuilds the image if necessary.

     * Because java chars are UTF-8 16 bit chars this function should be UTF safe in the 2 byte range, which is all intellij
     * seems to handle anyway....
     */
    private fun updateDimensions(editor: Editor): Int {
        val scale = config.pixelsPerLine.toDouble() / editor.lineHeight
        val height = (editor.contentComponent.height * scale).roundToInt()

        // If the image is too small to represent the entire document now then regenerate it
        // TODO: Copy old image when incremental update is added.
        if (img == null || img!!.height < height || img!!.width < config.width) {
            if (img != null) img!!.flush()
            // Create an image that is a bit bigger then the one we need so we don't need to re-create it again soon.
            // Documents can get big, so rather then relative sizes lets just add a fixed amount on.
            img = UIUtil.createImage(config.width, height + (100 * config.pixelsPerLine), BufferedImage.TYPE_4BYTE_ABGR)
            logger.debug("Created new image")
        }

        return height
    }

    /**
     * Internal worker function to update the minimap image
     *
     * @param editor        The editor being drawn
     * @param hl            The syntax highlighter to use for the language this document is in.
     */
    fun update(editor: EditorImpl, hl: SyntaxHighlighter, indicator: ProgressIndicator?): Int {
        logger.debug("Updating file image.")
        val height = updateDimensions(editor)

        val text = editor.document.text
        val colorScheme = editor.colorsScheme
        val folds = editor.foldingModel

        var ch: Char
        val lexer = hl.highlightingLexer
        var tokenType: IElementType?

        val g = img!!.graphics as Graphics2D
        g.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR)
        g.fillRect(0, 0, img!!.width, img!!.height)

        lexer.start(text)
        tokenType = lexer.tokenType

        var x: Int
        var y: Int
        val line = editor.document.createLineIterator()
        val colorBuffer = FloatArray(4)
        val scaleBuffer = FloatArray(4)
        while (tokenType != null) {
            indicator?.checkCanceled()

            val tokenStart = lexer.tokenStart
            y = editor.offsetToVisualLine(tokenStart) * config.pixelsPerLine
            line.start(tokenStart)

            // Pre-loop to count whitespace from start of line.
            x = 0
            var i = line.start
            while (i < tokenStart) {
                // Jump over folds
                if (folds.isOffsetCollapsed(i)) {
                    i = folds.getCollapsedRegionAtOffset(i)!!.endOffset
                    continue
                }

                x += if (text[i++] == '\t') {
                    4
                } else {
                    1
                }

                // Abort if this line is getting to long...
                if (x > config.width)
                    break
            }

            // Render whole token, make sure multi lines are handled gracefully.
            while (i < lexer.tokenEnd) {
                // Jump over folds
                if (folds.isOffsetCollapsed(i)) {
                    i = folds.getCollapsedRegionAtOffset(i)!!.endOffset
                    continue
                }

                // Watch out for tokens that extend past the document... bad plugins? see issue #138
                if (i >= text.length)
                    return height

                ch = text[i]

                when (ch) {
                    '\n' -> {
                        x = 0
                        y += config.pixelsPerLine
                    }
                    '\t' -> x += 4
                    else -> x += 1
                }

                if (0 <= x && x < img!!.width && 0 <= y && y + config.pixelsPerLine < img!!.height) {
                    getColorForElementType(tokenType, hl, colorScheme, colorBuffer) // Load rgba into colorBuffer
                    if (config.clean) {
                        renderClean(x, y, text[i].toInt(), colorBuffer, scaleBuffer)
                    } else {
                        renderAccurate(x, y, text[i].toInt(), colorBuffer, scaleBuffer)
                    }
                }

                ++i
            }

            do // Skip to end of fold
                lexer.advance()
            while (lexer.tokenType != null && lexer.tokenStart < i)
            tokenType = lexer.tokenType
        }

        return height
    }

    private fun renderClean(x: Int, y: Int, char: Int, color: FloatArray, buffer: FloatArray) {
        val weight = when (char) {
            in 0..32 -> 0.0f
            in 33..126 -> 0.8f
            else -> 0.4f
        }

        if (weight == 0.0f) return

        when (config.pixelsPerLine) {
            1 -> // Cant show whitespace between lines any more. This looks rather ugly...
                setPixel(x, y + 1, color, weight * 0.6f, buffer)

            2 -> {
                // Two lines we make the top line a little lighter to give the illusion of whitespace between lines.
                setPixel(x, y, color, weight * 0.3f, buffer)
                setPixel(x, y + 1, color, weight * 0.6f, buffer)
            }
            3 -> {
                // Three lines we make the top nearly empty, and fade the bottom a little too
                setPixel(x, y, color, weight * 0.1f, buffer)
                setPixel(x, y + 1, color, weight * 0.6f, buffer)
                setPixel(x, y + 2, color, weight * 0.6f, buffer)
            }
            4 -> {
                // Empty top line, Nice blend for everything else
                setPixel(x, y + 1, color, weight * 0.6f, buffer)
                setPixel(x, y + 2, color, weight * 0.6f, buffer)
                setPixel(x, y + 3, color, weight * 0.6f, buffer)
            }
        }
    }

    private fun renderAccurate(x: Int, y: Int, char: Int, color: FloatArray, buffer: FloatArray) {
        val topWeight = GetTopWeight(char)
        val bottomWeight = GetBottomWeight(char)
        // No point rendering non visible characters.
        if (topWeight == 0.0f && bottomWeight == 0.0f) return

        when (config.pixelsPerLine) {
            1 -> // Cant show whitespace between lines any more. This looks rather ugly...
                setPixel(x, y + 1, color, ((topWeight + bottomWeight) / 2.0).toFloat(), buffer)

            2 -> {
                // Two lines we make the top line a little lighter to give the illusion of whitespace between lines.
                setPixel(x, y, color, topWeight * 0.5f, buffer)
                setPixel(x, y + 1, color, bottomWeight, buffer)
            }
            3 -> {
                // Three lines we make the top nearly empty, and fade the bottom a little too
                setPixel(x, y, color, topWeight * 0.3f, buffer)
                setPixel(x, y + 1, color, ((topWeight + bottomWeight) / 2.0).toFloat(), buffer)
                setPixel(x, y + 2, color, bottomWeight * 0.7f, buffer)
            }
            4 -> {
                // Empty top line, Nice blend for everything else
                setPixel(x, y + 1, color, topWeight, buffer)
                setPixel(x, y + 2, color, ((topWeight + bottomWeight) / 2.0).toFloat(), buffer)
                setPixel(x, y + 3, color, bottomWeight, buffer)
            }
        }
    }

    /**
     * Looks up the color a token should be rendered with.
     *
     * @param element       The element to get the color for
     * @param hl            the syntax highlighter for this document
     * @param colorScheme   the users color scheme
     */
    private fun getColorForElementType(element: IElementType, hl: SyntaxHighlighter, colorScheme: EditorColorsScheme, result: FloatArray) {
        colorScheme.defaultForeground.getRGBComponents(result)
        for (attribute in hl.getTokenHighlights(element)) {
            colorScheme.getAttributes(attribute)?.
                foregroundColor?.
                getRGBComponents(result)
        }
    }

    /**
     * mask out the alpha component and set it to the given value.
     * @param rgba      Color A
     * *
     * @param alpha     alpha percent from 0-1.
     */
    private fun setPixel(x: Int, y: Int, rgba: FloatArray, alpha: Float, scaleBuffer: FloatArray) {
        for (i in 0..2) scaleBuffer[i] = rgba[i] * 0xFF
        scaleBuffer[3] = when {
            alpha > 1 -> rgba[3]
            else -> Math.max(alpha, 0f)
        } * 0xFF

        img!!.raster.setPixel(x, y, scaleBuffer)
    }
}
