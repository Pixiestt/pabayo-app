# Login Function Unit Tests

This directory contains comprehensive unit tests for the login functionality in the Pabayo App.

## Test Structure

### Test Files

1. **TestData.kt** - Contains test data objects and mock responses
2. **UserViewModelTest.kt** - Tests for the UserViewModel loginUser function
3. **UserRepositoryTest.kt** - Tests for the UserRepository loginUser function  
4. **LoginActivityTest.kt** - Tests for LoginActivity business logic
5. **LoginIntegrationTest.kt** - Integration tests for the complete login flow
6. **TestUtils.kt** - Utility functions for testing
7. **LoginTestSuite.kt** - Test suite to run all login tests together

### Test Coverage

The tests cover the following scenarios:

#### UserViewModel Tests
- ✅ Successful login with valid credentials
- ✅ Failed login with invalid credentials
- ✅ Login with empty email/password
- ✅ Exception handling during login
- ✅ Null response handling
- ✅ Successful response emission

#### UserRepository Tests
- ✅ Successful API response with token saving
- ✅ Failed API response without token saving
- ✅ Empty token handling
- ✅ API service exception handling
- ✅ Input validation
- ✅ Correct API service calls

#### LoginActivity Tests
- ✅ Token saving to SharedPreferences
- ✅ User ID saving to SharedPreferences
- ✅ Input validation (email/password)
- ✅ LoginRequest creation
- ✅ LoginResponse validation
- ✅ Role-based activity routing

#### Integration Tests
- ✅ Complete login flow for owner role
- ✅ Complete login flow for customer role
- ✅ Invalid credentials handling
- ✅ Network error handling
- ✅ Server error handling
- ✅ Multiple login attempts

## Running the Tests

### Run All Login Tests
```bash
./gradlew test --tests "com.example.capstone2.LoginTestSuite"
```

### Run Individual Test Classes
```bash
# UserViewModel tests
./gradlew test --tests "com.example.capstone2.viewmodel.UserViewModelTest"

# UserRepository tests  
./gradlew test --tests "com.example.capstone2.repository.UserRepositoryTest"

# LoginActivity tests
./gradlew test --tests "com.example.capstone2.authentication.LoginActivityTest"

# Integration tests
./gradlew test --tests "com.example.capstone2.integration.LoginIntegrationTest"
```

### Run Specific Test Methods
```bash
# Run a specific test method
./gradlew test --tests "com.example.capstone2.viewmodel.UserViewModelTest.loginUser with valid credentials should return successful response"
```

## Dependencies Added

The following testing dependencies were added to `build.gradle.kts`:

```kotlin
testImplementation("org.mockito:mockito-core:5.8.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("androidx.lifecycle:lifecycle-runtime-testing:2.8.7")
```

## Test Data

The `TestData.kt` file provides:
- Mock user objects (owner and customer roles)
- Valid and invalid login requests
- Successful and failed login responses
- Edge case data (empty fields, invalid tokens)

## Mocking Strategy

The tests use MockK for mocking:
- API service responses
- SharedPreferences operations
- Context objects
- Repository dependencies

## Best Practices Implemented

1. **Isolation**: Each test is independent and doesn't affect others
2. **Mocking**: External dependencies are properly mocked
3. **Edge Cases**: Tests cover both happy path and error scenarios
4. **Coroutines**: Proper testing of coroutine-based code
5. **LiveData**: Testing of LiveData emissions and observers
6. **Integration**: End-to-end testing of the complete login flow

## Test Results

When you run the tests, you should see output similar to:
```
> Task :app:testDebugUnitTest

com.example.capstone2.viewmodel.UserViewModelTest > loginUser with valid credentials should return successful response PASSED
com.example.capstone2.viewmodel.UserViewModelTest > loginUser with invalid credentials should return null PASSED
...

BUILD SUCCESSFUL in 2s
```

All tests should pass, demonstrating that the login functionality works correctly across all tested scenarios.
