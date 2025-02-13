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

import android.text.util.Linkify
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.ui.common.LinkSpannableString
import com.wire.android.ui.markdown.MarkdownConsts.MENTION_MARK
import com.wire.android.ui.markdown.MarkdownConsts.TAG_URL
import com.wire.kalium.logic.data.message.mention.MessageMention
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Text as nodeText

@Composable
fun MarkdownDocument(document: Document, nodeData: NodeData) {
    MarkdownBlockChildren(document, nodeData)
}

@Composable
fun MarkdownBlockChildren(parent: Node, nodeData: NodeData) {
    var child = parent.firstChild

    var updateMentions = nodeData.mentions

    while (child != null) {
        val updatedNodeData = nodeData.copy(mentions = updateMentions)
        when (child) {
            is Document -> MarkdownDocument(child, updatedNodeData)
            is BlockQuote -> MarkdownBlockQuote(child, updatedNodeData)
            is ThematicBreak -> MarkdownThematicBreak()
            is Heading -> MarkdownHeading(child, updatedNodeData)
            is Paragraph -> MarkdownParagraph(child, updatedNodeData) {
                updateMentions = it
            }

            is FencedCodeBlock -> MarkdownFencedCodeBlock(child)
            is IndentedCodeBlock -> MarkdownIndentedCodeBlock(child)
            is BulletList -> MarkdownBulletList(child, updatedNodeData)
            is OrderedList -> MarkdownOrderedList(child, updatedNodeData)
            is TableBlock -> MarkdownTable(child, updatedNodeData) {
                updateMentions = it
            }
        }
        child = child.next
    }
}

@Suppress("LongMethod")
fun inlineChildren(
    parent: Node,
    annotatedString: AnnotatedString.Builder,
    nodeData: NodeData
): List<DisplayMention> {
    var child = parent.firstChild

    var updatedMentions = nodeData.mentions

    while (child != null) {
        when (child) {
            is Paragraph ->
                updatedMentions = inlineChildren(
                    child,
                    annotatedString,
                    nodeData.copy(mentions = updatedMentions),
                )

            is nodeText -> {
                updatedMentions = appendLinksAndMentions(
                    annotatedString,
                    convertTypoGraphs(child),
                    nodeData.copy(mentions = updatedMentions)
                )
            }

            is Image -> {
                updatedMentions = appendLinksAndMentions(
                    annotatedString,
                    child.destination,
                    nodeData.copy(mentions = updatedMentions)
                )
            }

            is Emphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body05.fontFamily,
                        fontStyle = FontStyle.Italic
                    )
                )
                updatedMentions = inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is StrongEmphasis -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        fontFamily = nodeData.typography.body02.fontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                updatedMentions = inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
            }

            is Code -> {
                annotatedString.pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                annotatedString.append(child.literal)
                annotatedString.pop()
            }

            is HardLineBreak -> {
                annotatedString.append("\n")
                annotatedString.pop()
            }

            is Link -> {
                annotatedString.pushStyle(
                    SpanStyle(
                        color = nodeData.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                annotatedString.pushStringAnnotation(TAG_URL, child.destination)
                updatedMentions = inlineChildren(
                    child,
                    annotatedString,
                    nodeData
                )
                annotatedString.pop()
                annotatedString.pop()
            }

            is Strikethrough -> {
                annotatedString.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                updatedMentions = inlineChildren(child, annotatedString, nodeData)
                annotatedString.pop()
            }
        }
        child = child.next
    }

    return updatedMentions
}

@Suppress("LongMethod")
fun appendLinksAndMentions(
    annotatedString: AnnotatedString.Builder,
    string: String,
    nodeData: NodeData
): List<DisplayMention> {

    val stringBuilder = StringBuilder(string)
    val updatedMentions = nodeData.mentions.toMutableList()

    // get mentions from text, remove mention marks and update position of mentions
    val mentionList: List<MessageMention> = if (stringBuilder.contains(MENTION_MARK) && updatedMentions.isNotEmpty()) {
        nodeData.mentions.mapNotNull { displayMention ->
            val markedMentionLength = (MENTION_MARK + displayMention.mentionUserName + MENTION_MARK).length
            val startIndex = stringBuilder.indexOf(MENTION_MARK + displayMention.mentionUserName + MENTION_MARK)
            val endIndex = startIndex + markedMentionLength

            if (startIndex != -1) {
                stringBuilder.replace(startIndex, endIndex, displayMention.mentionUserName)
                // remove mention from list to not use the same mention twice
                updatedMentions.removeAt(0)
                MessageMention(
                    startIndex,
                    displayMention.length,
                    displayMention.userId,
                    displayMention.isSelfMention
                )
            } else {
                null
            }
        }
    } else {
        listOf()
    }

    val linkInfos = LinkSpannableString.getLinkInfos(stringBuilder.toString(), Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)

    val append = buildAnnotatedString {
        append(stringBuilder)
        with(nodeData.colorScheme) {
            linkInfos.forEach {
                if (it.end - it.start <= 0) {
                    return@forEach
                }
                addStyle(
                    style = SpanStyle(
                        color = primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = it.start,
                    end = it.end
                )
                addStringAnnotation(
                    tag = TAG_URL,
                    annotation = it.url,
                    start = it.start,
                    end = it.end
                )
            }

            if (mentionList.isNotEmpty()) {
                mentionList.forEach {
                    if (it.length <= 0 || it.start >= length || it.start + it.length > length) {
                        return@forEach
                    }
                    addStyle(
                        style = SpanStyle(
                            fontWeight = nodeData.typography.body02.fontWeight,
                            color = onPrimaryVariant,
                            background = if (it.isSelfMention) primaryVariant else Color.Unspecified
                        ),
                        start = it.start,
                        end = it.start + it.length
                    )
                    addStringAnnotation(
                        tag = MarkdownConsts.TAG_MENTION,
                        annotation = it.userId.toString(),
                        start = it.start,
                        end = it.start + it.length
                    )
                }
            }
        }
    }
    annotatedString.append(append)
    return updatedMentions
}

private fun convertTypoGraphs(child: Text) = child.literal
    .replace("(c)", "©")
    .replace("(C)", "©")
    .replace("(r)", "®")
    .replace("(R)", "®")
    .replace("(tm)", "™")
    .replace("(TM)", "™")
    .replace("+-", "±")
