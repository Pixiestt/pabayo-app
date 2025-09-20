package com.example.capstone2.repository

import android.content.Context
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.testdata.TestData
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import retrofit2.Response

@RunWith(JUnit4::class)
class UserRepositoryTest {

    private lateinit var userRepository: UserRepository
    private lateinit var mockContext: Context
    private lateinit var mockApiService: ApiService
    private lateinit var mockSharedPrefManager: SharedPrefManager

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockSharedPrefManager = mockk(relaxed = true)
        
        // Mock the ApiClient.getApiService method
        mockkStatic("com.example.capstone2.network.ApiClient")
        every { com.example.capstone2.network.ApiClient.getApiService(any()) } returns mockApiService
        
        // Mock SharedPrefManager constructor
        mockkConstructor(SharedPrefManager::class)
        every { anyConstructed<SharedPrefManager>().saveAuthToken(any()) } just Runs
        
        userRepository = UserRepository(mockContext)
    }

    @Test
    fun `loginUser with successful response should save token and return response`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val expectedResponse = Response.success(TestData.successfulLoginResponseOwner)
        
        coEvery { mockApiService.login(loginRequest) } returns expectedResponse

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(result.isSuccessful)
        assert(result.body() == TestData.successfulLoginResponseOwner)
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser with failed response should not save token and return failed response`() = runTest {
        // Given
        val loginRequest = TestData.invalidLoginRequest
        val failedResponse = Response.error<LoginResponse>(401, mockk())
        
        coEvery { mockApiService.login(loginRequest) } returns failedResponse

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(!result.isSuccessful)
        assert(result.code() == 401)
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser with successful response but null token should not save token`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val responseWithNullToken = Response.success(
            TestData.successfulLoginResponseOwner.copy(token = "")
        )
        
        coEvery { mockApiService.login(loginRequest) } returns responseWithNullToken

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(result.isSuccessful)
        assert(result.body()?.token == "")
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser with successful response and valid token should save token`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestCustomer
        val expectedResponse = Response.success(TestData.successfulLoginResponseCustomer)
        
        coEvery { mockApiService.login(loginRequest) } returns expectedResponse

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(result.isSuccessful)
        assert(result.body()?.token == "valid_token_456")
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser should handle API service exception`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val exception = RuntimeException("Network error")
        
        coEvery { mockApiService.login(loginRequest) } throws exception

        // When & Then
        try {
            userRepository.loginUser(loginRequest)
            assert(false) { "Expected exception to be thrown" }
        } catch (e: RuntimeException) {
            assert(e.message == "Network error")
        }
        
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser with empty email should return appropriate response`() = runTest {
        // Given
        val loginRequest = TestData.emptyEmailLoginRequest
        val failedResponse = Response.error<LoginResponse>(400, mockk())
        
        coEvery { mockApiService.login(loginRequest) } returns failedResponse

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(!result.isSuccessful)
        assert(result.code() == 400)
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser with empty password should return appropriate response`() = runTest {
        // Given
        val loginRequest = TestData.emptyPasswordLoginRequest
        val failedResponse = Response.error<LoginResponse>(400, mockk())
        
        coEvery { mockApiService.login(loginRequest) } returns failedResponse

        // When
        val result = userRepository.loginUser(loginRequest)

        // Then
        assert(!result.isSuccessful)
        assert(result.code() == 400)
        coVerify { mockApiService.login(loginRequest) }
    }

    @Test
    fun `loginUser should call API service with correct parameters`() = runTest {
        // Given
        val loginRequest = TestData.validLoginRequestOwner
        val expectedResponse = Response.success(TestData.successfulLoginResponseOwner)
        
        coEvery { mockApiService.login(loginRequest) } returns expectedResponse

        // When
        userRepository.loginUser(loginRequest)

        // Then
        coVerify { mockApiService.login(loginRequest) }
    }
}
