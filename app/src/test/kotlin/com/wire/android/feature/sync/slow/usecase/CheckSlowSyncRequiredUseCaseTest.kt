package com.wire.android.feature.sync.slow.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.sync.SyncRepository
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class CheckSlowSyncRequiredUseCaseTest : UnitTest() {

    @MockK
    private lateinit var syncRepository: SyncRepository

    private lateinit var checkSlowSyncRequiredUseCase: CheckSlowSyncRequiredUseCase

    @Before
    fun setUp() {
        checkSlowSyncRequiredUseCase = CheckSlowSyncRequiredUseCase(syncRepository)
    }

    @Test
    fun `given run is called, when syncRepository says slow sync is required, then propagates true`() {
        every { syncRepository.isSlowSyncRequired() } returns true

        val result = runBlocking { checkSlowSyncRequiredUseCase.run(Unit) }

        result shouldSucceed { it shouldBe true}
    }

    @Test
    fun `given run is called, when syncRepository says slow sync is not required, then propagates false`() {
        every { syncRepository.isSlowSyncRequired() } returns false

        val result = runBlocking { checkSlowSyncRequiredUseCase.run(Unit) }

        result shouldSucceed { it shouldBe false}
    }
}
