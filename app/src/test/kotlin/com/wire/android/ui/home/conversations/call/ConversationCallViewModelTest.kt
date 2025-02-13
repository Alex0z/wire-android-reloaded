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
package com.wire.android.ui.home.conversations.call

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ConversationCallViewModelTest {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var observeOngoingCalls: ObserveOngoingCallsUseCase

    @MockK
    private lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

    @MockK
    private lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var joinCall: AnswerCallUseCase

    @MockK
    private lateinit var endCall: EndCallUseCase

    @MockK
    private lateinit var observeSyncState: ObserveSyncStateUseCase

    @MockK
    private lateinit var isConferenceCallingEnabled: IsEligibleToStartCallUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    private lateinit var conversationCallViewModel: ConversationCallViewModel

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        every { savedStateHandle.get<String>(any()) } returns dummyConversationId
        every { savedStateHandle[any()] = any<String>() } returns Unit
        coEvery { observeEstablishedCalls.invoke() } returns emptyFlow()
        coEvery { observeOngoingCalls.invoke() } returns emptyFlow()
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")

        conversationCallViewModel = ConversationCallViewModel(
            qualifiedIdMapper = qualifiedIdMapper,
            savedStateHandle = savedStateHandle,
            observeOngoingCalls = observeOngoingCalls,
            observeEstablishedCalls = observeEstablishedCalls,
            navigationManager = navigationManager,
            answerCall = joinCall,
            endCall = endCall,
            observeSyncState = observeSyncState,
            isConferenceCallingEnabled = isConferenceCallingEnabled
        )
    }

    @Test
    fun `given join dialog displayed, when user dismiss it, then hide it`() {
        conversationCallViewModel.conversationCallViewState = conversationCallViewModel.conversationCallViewState.copy(
            shouldShowJoinAnywayDialog = true
        )

        conversationCallViewModel.dismissJoinCallAnywayDialog()

        assertEquals(false, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given no ongoing call, when user tries to join a call, then invoke answerCall call use case`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = false)

        coEvery { navigationManager.navigate(command = any()) } returns Unit
        coEvery { joinCall(conversationId = any()) } returns Unit

        conversationCallViewModel.joinOngoingCall()

        coVerify(exactly = 1) { joinCall(conversationId = any()) }
        coVerify(exactly = 1) { navigationManager.navigate(command = any()) }
        assertEquals(false, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
    }

    @Test
    fun `given an ongoing call, when user tries to join a call, then show JoinCallAnywayDialog`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = true)

        conversationCallViewModel.joinOngoingCall()

        assertEquals(true, conversationCallViewModel.conversationCallViewState.shouldShowJoinAnywayDialog)
        coVerify(inverse = true) { joinCall(conversationId = any()) }
    }

    @Test
    fun `given an ongoing call, when user confirms dialog to join a call, then end current call and join the newer one`() {
        conversationCallViewModel.conversationCallViewState =
            conversationCallViewModel.conversationCallViewState.copy(hasEstablishedCall = true)
        conversationCallViewModel.establishedCallConversationId = ConversationId("value", "Domain")
        coEvery { endCall(any()) } returns Unit

        conversationCallViewModel.joinAnyway()

        coVerify(exactly = 1) { endCall(any()) }
    }
}
