/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import org.commonmark.node.BlockQuote

@Composable
fun MarkdownBlockQuote(blockQuote: BlockQuote, nodeData: NodeData) {
    val color = MaterialTheme.wireColorScheme.onBackground
    val xOffset = dimensions().spacing12x.value
    Column(modifier = Modifier
        .drawBehind {
            drawLine(
                color = color,
                strokeWidth = 2f,
                start = Offset(xOffset, 0f),
                end = Offset(xOffset, size.height)
            )
        }
        .padding(start = dimensions().spacing16x, top = dimensions().spacing4x, bottom = dimensions().spacing4x)) {

        var child = blockQuote.firstChild
        while (child != null) {
            when (child) {
                is BlockQuote -> MarkdownBlockQuote(child, nodeData)
                else -> {
                    val text = buildAnnotatedString {
                        pushStyle(
                            MaterialTheme.wireTypography.body01.toSpanStyle()
                                .plus(SpanStyle(fontStyle = FontStyle.Italic))
                        )
                        inlineChildren(child, this, nodeData)
                        pop()
                    }
                    Text(text)
                }
            }
            child = child.next
        }
    }
}
