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
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageEditStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.DeliveryStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject

class MessageMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val messageContentMapper: MessageContentMapper,
    private val isoFormatter: ISOFormatter,
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    fun memberIdList(messages: List<Message>): List<UserId> = messages.flatMap { message ->
        listOf(message.senderUserId).plus(
            when (message) {
                is Message.Regular -> {
                    when (val failureType = message.deliveryStatus) {
                        is DeliveryStatus.CompleteDelivery -> listOf()
                        is DeliveryStatus.PartialDelivery ->
                            failureType.recipientsFailedDelivery + failureType.recipientsFailedWithNoClients
                    }
                }
                is Message.System -> {
                    when (val content = message.content) {
                        is MessageContent.MemberChange -> content.members
                        else -> listOf()
                    }
                }
                is Message.Signaling -> listOf()
            }
        )
    }.distinct()

    @Suppress("LongMethod")
    fun toUIMessage(userList: List<User>, message: Message.Standalone): UIMessage? {
        val sender = userList.findUser(message.senderUserId)
        val content = messageContentMapper.fromMessage(
            message = message,
            userList = userList
        )

        val footer = if (message is Message.Regular) {
            // TODO find ugly and proper heart emoji and merge them to ugly one 😅
            val totalHeartsCount = message.reactions.totalReactions
                .filterKeys { isHeart(it) }.values
                .sum()

            val hasSelfHeart = message.reactions.selfUserReactions.any { isHeart(it) }

            MessageFooter(
                message.id,
                message.reactions.totalReactions
                    .filter { !isHeart(it.key) }
                    .run {
                        if (totalHeartsCount != 0) {
                            plus("❤" to totalHeartsCount)
                        } else {
                            this
                        }
                    },
                message.reactions.selfUserReactions
                    .filter { isHeart(it) }.toSet()
                    .run {
                        if (hasSelfHeart) {
                            plus("❤")
                        } else {
                            this
                        }
                    }
            )
        } else {
            MessageFooter(message.id)
        }

        return when (content) {
            is UIMessageContent.Regular ->
                UIMessage.Regular(
                    messageContent = content,
                    source = if (sender is SelfUser) MessageSource.Self else MessageSource.OtherUser,
                    header = provideMessageHeader(sender, message),
                    messageFooter = footer,
                    userAvatarData = getUserAvatarData(sender)
                )

            is UIMessageContent.SystemMessage ->
                UIMessage.System(
                    messageContent = content,
                    source = if (sender is SelfUser) MessageSource.Self else MessageSource.OtherUser,
                    header = provideMessageHeader(sender, message),
                )

            null -> null

            /**
             * IncompleteAssetMessage is a displayable asset that's missing the remote data.
             * Sometimes client receives two events about the same asset, first one with only part of the data ("preview" type from web),
             * so such asset shouldn't be shown until all the required data is received.
             */
            UIMessageContent.IncompleteAssetMessage -> null
        }
    }

    private fun provideExpirationData(message: Message.Standalone): ExpirationStatus {
        val expirationData = (message as? Message.Regular)?.expirationData
        return if (expirationData == null) {
            ExpirationStatus.NotExpirable
        } else {
            ExpirationStatus.Expirable(
                expireAfter = expirationData.expireAfter,
                selfDeletionStatus = expirationData.selfDeletionStatus
            )
        }
    }

    private fun isHeart(it: String) = it == "❤️" || it == "❤"

    private fun provideMessageHeader(sender: User?, message: Message.Standalone): MessageHeader = MessageHeader(
        username = sender?.name?.let { UIText.DynamicString(it) }
            ?: UIText.StringResource(R.string.username_unavailable_label),
        membership = when (sender) {
            is OtherUser -> userTypeMapper.toMembership(sender.userType)
            is SelfUser, null -> Membership.None
        },
        connectionState = getConnectionState(sender),
        isLegalHold = false,
        messageTime = MessageTime(message.date),
        messageStatus = getMessageStatus(message),
        messageId = message.id,
        userId = sender?.id,
        isSenderDeleted = when (sender) {
            is OtherUser -> sender.deleted
            is SelfUser, null -> false
        },
        isSenderUnavailable = when (sender) {
            is OtherUser -> sender.isUnavailableUser
            is SelfUser, null -> false
        },
        clientId = (message as? Message.Sendable)?.senderClientId
    )

    private fun getMessageStatus(message: Message.Standalone): MessageStatus {
        val isMessageEdited = message is Message.Regular && message.editStatus is Message.EditStatus.Edited

        val content = message.content
        val flowStatus = if (content is MessageContent.FailedDecryption) {
            MessageFlowStatus.Failure.Decryption(content.isDecryptionResolved)
        } else {
            when (message.status) {
                Message.Status.PENDING -> MessageFlowStatus.Sending
                Message.Status.SENT -> MessageFlowStatus.Sent
                Message.Status.READ -> MessageFlowStatus.Read(1) // TODO add read count
                Message.Status.FAILED -> MessageFlowStatus.Failure.Send.Locally(isMessageEdited)
                Message.Status.FAILED_REMOTELY -> MessageFlowStatus.Failure.Send.Remotely(isMessageEdited, message.conversationId.domain)
            }
        }

        return MessageStatus(
            flowStatus = flowStatus,
            editStatus = if (message is Message.Regular && message.editStatus is Message.EditStatus.Edited) {
                MessageEditStatus.Edited(
                    isoFormatter.fromISO8601ToTimeFormat(
                        utcISO = (message.editStatus as Message.EditStatus.Edited).lastTimeStamp
                    )
                )
            } else {
                MessageEditStatus.NonEdited
            },
            expirationStatus = provideExpirationData(message),
            isDeleted = message.visibility == Message.Visibility.DELETED
        )
    }

    private fun getUserAvatarData(sender: User?) = UserAvatarData(
        asset = sender?.previewAsset(wireSessionImageLoader),
        availabilityStatus = sender?.availabilityStatus ?: UserAvailabilityStatus.NONE,
        connectionState = getConnectionState(sender)
    )

    private fun getConnectionState(sender: User?) =
        when (sender) {
            is OtherUser -> sender.connectionStatus
            is SelfUser, null -> null
        }
}
