package com.example.capstone2

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all login-related tests
 * 
 * This suite includes:
 * - UserViewModel tests
 * - UserRepository tests  
 * - LoginActivity tests
 * - Integration tests
 * 
 * To run all login tests, execute this test suite.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    com.example.capstone2.viewmodel.UserViewModelTest::class,
    com.example.capstone2.repository.UserRepositoryTest::class,
    com.example.capstone2.authentication.LoginActivityTest::class,
    com.example.capstone2.integration.LoginIntegrationTest::class
)
class LoginTestSuite
