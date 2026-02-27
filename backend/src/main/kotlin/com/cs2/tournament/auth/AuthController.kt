package com.cs2.tournament.auth

import com.cs2.tournament.model.User
import com.cs2.tournament.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class AuthRequest(val username: String, val password: String)
data class AuthResponse(val token: String, val userId: String, val username: String)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtils: JwtUtils
) {

    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username already exists"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password) ?: ""
        )
        userRepository.save(user)

        val token = jwtUtils.generateToken(user.username, user.id)
        return ResponseEntity.ok(AuthResponse(token, user.id, user.username))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        val user = userRepository.findByUsername(request.username).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid credentials"))

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid credentials"))
        }

        val token = jwtUtils.generateToken(user.username, user.id)
        return ResponseEntity.ok(AuthResponse(token, user.id, user.username))
    }

    @GetMapping("/me")
    fun me(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Any> {
        val token = authHeader.substring(7)
        if (jwtUtils.validateToken(token)) {
            val username = jwtUtils.extractUsername(token)
            val userId = jwtUtils.extractUserId(token)
            return ResponseEntity.ok(mapOf("userId" to userId, "username" to username))
        }
        return ResponseEntity.status(401).build()
    }
}
