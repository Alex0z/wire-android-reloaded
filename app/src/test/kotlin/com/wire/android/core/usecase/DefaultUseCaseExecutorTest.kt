package com.wire.android.core.usecase

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class DefaultUseCaseExecutorTest : UnitTest() {

    private lateinit var executor: DefaultUseCaseExecutor

    @Before
    fun setUp() {
        executor = DefaultUseCaseExecutor()
    }

    @Test
    fun `given a scope and a use case, when invoke is called on use case, then applies onResult to returned value`() {
        val useCase = mockk<UseCase<String, Int>>()
        val param = 3
        val result = Either.Right("3")
        coEvery { useCase.run(param) } returns result

        runBlocking {
            with(executor) {
                useCase.invoke(this@runBlocking, param, Dispatchers.IO) {
                    it shouldBe result
                }
            }
        }
    }

    @Test
    fun `given a scope and an observable use case, when invoke is called on use case, then applies onResult to returned value`() {
        val observableUseCase = mockk<ObservableUseCase<String, Int>>()
        val param = 3
        val result = Either.Right("3")
        coEvery { observableUseCase.run(param) } returns flowOf(result)

        runBlocking {
            with(executor) {
                observableUseCase.invoke(this@runBlocking, param, Dispatchers.IO) {
                    it shouldBe result
                }
            }
        }
    }
}
