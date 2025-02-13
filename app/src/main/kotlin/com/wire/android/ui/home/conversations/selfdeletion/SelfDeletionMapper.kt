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
package com.wire.android.ui.home.conversations.selfdeletion

import com.wire.android.ui.home.messagecomposer.state.SelfDeletionDuration
import kotlin.time.Duration

object SelfDeletionMapper {
    fun Duration.toSelfDeletionDuration(): SelfDeletionDuration = when (this) {
        SelfDeletionDuration.TenSeconds.value -> SelfDeletionDuration.TenSeconds
        SelfDeletionDuration.OneMinute.value -> SelfDeletionDuration.OneMinute
        SelfDeletionDuration.FiveMinutes.value -> SelfDeletionDuration.FiveMinutes
        SelfDeletionDuration.OneHour.value -> SelfDeletionDuration.OneHour
        SelfDeletionDuration.OneDay.value -> SelfDeletionDuration.OneDay
        SelfDeletionDuration.OneWeek.value -> SelfDeletionDuration.OneWeek
        SelfDeletionDuration.FourWeeks.value -> SelfDeletionDuration.FourWeeks
        else -> SelfDeletionDuration.None
    }
}
