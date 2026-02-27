package com.cs2.tournament.auth

import com.cs2.tournament.model.User
import com.cs2.tournament.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.util.UUID

@RestController
@RequestMapping("/api/auth/steam")
class SteamAuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtils: JwtUtils
) {
    private val steamLoginUrl = "https://steamcommunity.com/openid/login"
    private val restTemplate = RestTemplate()

    @Value("\${app.frontend.url:http://localhost:4200}")
    lateinit var frontendUrl: String

    @Value("\${app.backend.url:http://localhost:8080}")
    lateinit var backendUrl: String

    @GetMapping
    fun steamLogin(response: HttpServletResponse) {
        val returnTo = "$backendUrl/api/auth/steam/callback"
        val realm = backendUrl

        val params = mapOf(
            "openid.ns" to "http://specs.openid.net/auth/2.0",
            "openid.mode" to "checkid_setup",
            "openid.return_to" to returnTo,
            "openid.realm" to realm,
            "openid.identity" to "http://specs.openid.net/auth/2.0/identifier_select",
            "openid.claimed_id" to "http://specs.openid.net/auth/2.0/identifier_select"
        )

        val queryString = params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        response.sendRedirect("$steamLoginUrl?$queryString")
    }

    @GetMapping("/callback")
    fun steamCallback(request: HttpServletRequest, response: HttpServletResponse) {
        val params = request.parameterMap
        val mode = request.getParameter("openid.mode")

        if (mode != "id_res") {
            response.sendRedirect("$frontendUrl/login?error=steam_auth_failed")
            return
        }

        // Validate the response with Steam
        val validateParams: MultiValueMap<String, String> = LinkedMultiValueMap()
        validateParams.add("openid.mode", "check_authentication")
        
        params.forEach { (key, values) ->
            if (key != "openid.mode") {
                validateParams.add(key, values.first())
            }
        }

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        val httpEntity = HttpEntity(validateParams, headers)

        val steamResponse: ResponseEntity<String> = restTemplate.postForEntity(
            steamLoginUrl,
            httpEntity,
            String::class.java
        )

        if (steamResponse.body?.contains("is_valid:true") == true) {
            val claimedId = request.getParameter("openid.claimed_id") ?: ""
            val steamId = claimedId.substringAfterLast("/")

            // Find or create user
            val user = userRepository.findBySteamId(steamId).orElseGet {
                val newUser = User(
                    username = "steam_$steamId",
                    passwordHash = passwordEncoder.encode(UUID.randomUUID().toString()) ?: "",
                    steamId = steamId
                )
                userRepository.save(newUser)
            }

            val token = jwtUtils.generateToken(user.username, user.id)
            response.sendRedirect("$frontendUrl/login?token=$token")
        } else {
            response.sendRedirect("$frontendUrl/login?error=steam_validation_failed")
        }
    }
}
