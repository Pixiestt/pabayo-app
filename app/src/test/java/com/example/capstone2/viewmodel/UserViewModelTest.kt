package com.example.capstone2.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.repository.UserRepository
import com.example.capstone2.testdata.TestData
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class UserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var userViewModel: UserViewModel
    private lateinit var mockUserRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockUserRepository = mockk()
        userViewModel = UserViewModel(mockUserRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loginUser with valid credentials should return successful response`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val expectedResponse = Response.success(TestData.successfulLoginResponseOwner)
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns expectedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(TestData.successfulLoginResponseOwner) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser with invalid credentials should return null`() = runTest {
        // Given
        val loginRequest = TestData.invalidLoginRequest
        val failedResponse = Response.error<LoginResponse>(401, mockk())
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns failedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(null) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser with empty email should return null`() = runTest {
        // Given
        val loginRequest = TestData.emptyEmailLoginRequest
        val failedResponse = Response.error<LoginResponse>(400, mockk())
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns failedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(null) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser with empty password should return null`() = runTest {
        // Given
        val loginRequest = TestData.emptyPasswordLoginRequest
        val failedResponse = Response.error<LoginResponse>(400, mockk())
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns failedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(null) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser should handle repository exception gracefully`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val exception = RuntimeException("Network error")
        
        coEvery { mockUserRepository.loginUser(loginRequest) } throws exception

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(null) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser should emit null when response body is null`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val responseWithNullBody = Response.success<LoginResponse>(null)
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns responseWithNullBody

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(null) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }

    @Test
    fun `loginUser should emit response when response is successful and body is not null`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestCustomer
        val expectedResponse = Response.success(TestData.successfulLoginResponseCustomer)
        
        coEvery { mockUserRepository.loginUser(loginRequest) } returns expectedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        verify { observer.onChanged(TestData.successfulLoginResponseCustomer) }
        coVerify { mockUserRepository.loginUser(loginRequest) }
    }
}
