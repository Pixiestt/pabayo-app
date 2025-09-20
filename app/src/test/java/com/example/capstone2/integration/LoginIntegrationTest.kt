package com.example.capstone2.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.repository.UserRepository
import com.example.capstone2.testdata.TestData
import com.example.capstone2.viewmodel.UserViewModel
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
class LoginIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var userViewModel: UserViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var mockApiService: ApiService
    private lateinit var mockContext: android.content.Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockContext = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        
        // Mock ApiClient
        mockkStatic("com.example.capstone2.network.ApiClient")
        every { com.example.capstone2.network.ApiClient.getApiService(any()) } returns mockApiService
        
        // Mock SharedPrefManager
        mockkConstructor(com.example.capstone2.repository.SharedPrefManager::class)
        every { anyConstructed<com.example.capstone2.repository.SharedPrefManager>().saveAuthToken(any()) } just Runs
        
        userRepository = UserRepository(mockContext)
        userViewModel = UserViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `complete login flow with valid owner credentials should succeed`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val expectedResponse = Response.success(TestData.successfulLoginResponseOwner)
        
        coEvery { mockApiService.login(loginRequest) } returns expectedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(TestData.successfulLoginResponseOwner) }
        coVerify { mockApiService.login(loginRequest) }
        
        // Verify the response contains expected data
        val loginResponse = TestData.successfulLoginResponseOwner
        assert(loginResponse.user.roleID == 2L) // Owner role
        assert(loginResponse.token.isNotEmpty())
        assert(loginResponse.user.emailAddress == "john.doe@example.com")
    }

    @Test
    fun `complete login flow with valid customer credentials should succeed`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestCustomer
        val expectedResponse = Response.success(TestData.successfulLoginResponseCustomer)
        
        coEvery { mockApiService.login(loginRequest) } returns expectedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(TestData.successfulLoginResponseCustomer) }
        coVerify { mockApiService.login(loginRequest) }
        
        // Verify the response contains expected data
        val loginResponse = TestData.successfulLoginResponseCustomer
        assert(loginResponse.user.roleID == 3L) // Customer role
        assert(loginResponse.token.isNotEmpty())
        assert(loginResponse.user.emailAddress == "jane.smith@example.com")
    }

    @Test
    fun `complete login flow with invalid credentials should fail`() = runTest {
        // Given
        val loginRequest = TestData.invalidLoginRequest
        val failedResponse = Response.error<LoginResponse>(401, mockk())
        
        coEvery { mockApiService.login(loginRequest) } returns failedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(null) }
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `login flow should handle network errors gracefully`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val networkException = java.net.UnknownHostException("Network error")
        
        coEvery { mockApiService.login(loginRequest) } throws networkException

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(null) }
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `login flow should handle server errors gracefully`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val serverErrorResponse = Response.error<LoginResponse>(500, mockk())
        
        coEvery { mockApiService.login(loginRequest) } returns serverErrorResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(null) }
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `login flow should handle malformed response gracefully`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val malformedResponse = Response.success<LoginResponse>(null)
        
        coEvery { mockApiService.login(loginRequest) } returns malformedResponse

        // When
        val result = userViewModel.loginUser(loginRequest)
        val observer = mockk<Observer<LoginResponse?>>(relaxed = true)
        result.observeForever(observer)

        // Then
        advanceUntilIdle()
        
        verify { observer.onChanged(null) }
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `multiple login attempts should work independently`() = runTest {
        // Given
        val ownerRequest = TestData.validLoginRequestOwner
        val customerRequest = TestData.validLoginRequestCustomer
        val ownerResponse = Response.success(TestData.successfulLoginResponseOwner)
        val customerResponse = Response.success(TestData.successfulLoginResponseCustomer)
        
        coEvery { mockApiService.login(ownerRequest) } returns ownerResponse
        coEvery { mockApiService.login(customerRequest) } returns customerResponse

        // When - First login attempt
        val ownerResult = userViewModel.loginUser(ownerRequest)
        val ownerObserver = mockk<Observer<LoginResponse?>>(relaxed = true)
        ownerResult.observeForever(ownerObserver)

        // When - Second login attempt
        val customerResult = userViewModel.loginUser(customerRequest)
        val customerObserver = mockk<Observer<LoginResponse?>>(relaxed = true)
        customerResult.observeForever(customerObserver)

        // Then
        advanceUntilIdle()
        
        verify { ownerObserver.onChanged(TestData.successfulLoginResponseOwner) }
        verify { customerObserver.onChanged(TestData.successfulLoginResponseCustomer) }
        coVerify { mockApiService.login(ownerRequest) }
        coVerify { mockApiService.login(customerRequest) }
    }
}
