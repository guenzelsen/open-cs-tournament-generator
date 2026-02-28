package com.cs2.tournament.controller

import com.cs2.tournament.service.TeamService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/teams")
class TeamController(
    private val teamService: TeamService
) {

    data class CreateTeamRequest(val name: String, val pictureUrl: String? = null)
    data class PlayerRequest(val username: String)

    @GetMapping("/search")
    fun searchTeams(@RequestParam name: String): ResponseEntity<List<TeamService.TeamResponse>> {
        return ResponseEntity.ok(teamService.searchTeams(name))
    }

    @GetMapping("/my")
    fun getMyTeams(principal: Principal): ResponseEntity<List<TeamService.TeamResponse>> {
        return ResponseEntity.ok(teamService.getMyTeams(principal.name))
    }

    @PostMapping
    fun createTeam(@RequestBody request: CreateTeamRequest, principal: Principal): ResponseEntity<TeamService.TeamResponse> {
        return try {
            val t = teamService.createTeam(request.name, principal.name, request.pictureUrl)
            ResponseEntity.ok(t)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{id}/players")
    fun addPlayer(@PathVariable id: String, @RequestBody request: PlayerRequest, principal: Principal): ResponseEntity<TeamService.TeamResponse> {
        return try {
            val t = teamService.addPlayerToTeam(id, request.username, principal.name)
            ResponseEntity.ok(t)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}/players/{username}")
    fun removePlayer(@PathVariable id: String, @PathVariable username: String, principal: Principal): ResponseEntity<TeamService.TeamResponse> {
        return try {
            val t = teamService.removePlayerFromTeam(id, username, principal.name)
            ResponseEntity.ok(t)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}
