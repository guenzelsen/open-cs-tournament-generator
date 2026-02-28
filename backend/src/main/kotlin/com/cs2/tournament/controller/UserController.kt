package com.cs2.tournament.controller

import com.cs2.tournament.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository
) {

    data class UserProfileResponse(
        val username: String,
        val pictureUrl: String?
    )

    data class UpdateProfileRequest(
        val pictureUrl: String?
    )

    @GetMapping("/me")
    @Transactional(readOnly = true)
    fun getMyProfile(principal: Principal): ResponseEntity<UserProfileResponse> {
        val user = userRepository.findByUsername(principal.name).orElseThrow { IllegalArgumentException("User not found") }
        return ResponseEntity.ok(UserProfileResponse(user.username, user.pictureUrl))
    }

    @PutMapping("/me")
    @Transactional
    fun updateProfile(@RequestBody request: UpdateProfileRequest, principal: Principal): ResponseEntity<UserProfileResponse> {
        val user = userRepository.findByUsername(principal.name).orElseThrow { IllegalArgumentException("User not found") }
        user.pictureUrl = request.pictureUrl
        userRepository.save(user)
        return ResponseEntity.ok(UserProfileResponse(user.username, user.pictureUrl))
    }
}
