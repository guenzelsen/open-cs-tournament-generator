package com.cs2.tournament.auth

import com.cs2.tournament.model.User
import com.cs2.tournament.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.*

class AuthControllerTest {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtUtils: JwtUtils
    private lateinit var controller: AuthController

    @BeforeEach
    fun setup() {
        userRepository = mock(UserRepository::class.java)
        jwtUtils = JwtUtils("my-32-character-ultra-secure-and-ultra-long-secret")
        controller = AuthController(userRepository, BCryptPasswordEncoder(), jwtUtils)
    }

    @Test
    fun `me should return 401 for malformed Authorization header without Bearer prefix`() {
        val response = controller.me("InvalidHeader")
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `me should return 401 for empty Bearer token`() {
        val response = controller.me("Bearer ")
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `me should return 401 for Bearer prefix only`() {
        val response = controller.me("Bearer")
        assertEquals(401, response.statusCode.value())
    }

    @Test
    fun `me should return user info for valid token`() {
        val token = jwtUtils.generateToken("testuser", "user123")
        val response = controller.me("Bearer $token")
        assertEquals(200, response.statusCode.value())
        val body = response.body as Map<*, *>
        assertEquals("testuser", body["username"])
        assertEquals("user123", body["userId"])
    }
}
