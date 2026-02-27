package com.cs2.tournament.controller

import com.cs2.tournament.model.Team
import com.cs2.tournament.service.TournamentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/tournaments")
class TournamentController(
    private val tournamentService: TournamentService
) {

    data class CreateTournamentRequest(val name: String)
    data class AddTeamRequest(val name: String)
    data class ReportWinRequest(val winnerId: String)

    @GetMapping
    fun getAllTournaments(): ResponseEntity<List<TournamentService.TournamentResponse>> {
        return ResponseEntity.ok(tournamentService.getAllTournaments())
    }

    @GetMapping("/{id}")
    fun getTournament(@PathVariable id: String): ResponseEntity<TournamentService.TournamentResponse> {
        return try {
            ResponseEntity.ok(tournamentService.getTournament(id))
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun createTournament(@RequestBody request: CreateTournamentRequest, principal: Principal): ResponseEntity<TournamentService.TournamentResponse> {
        return try {
            val t = tournamentService.createTournament(request.name, principal.name)
            ResponseEntity.ok(t)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{id}/teams")
    fun addTeam(@PathVariable id: String, @RequestBody request: AddTeamRequest, principal: Principal): ResponseEntity<Team> {
        return try {
            val team = tournamentService.addTeam(id, request.name, principal.name)
            ResponseEntity.ok(team)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    fun removeTeam(@PathVariable id: String, @PathVariable teamId: String, principal: Principal): ResponseEntity<Void> {
        return try {
            tournamentService.removeTeam(id, teamId, principal.name)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{id}/start")
    fun startTournament(@PathVariable id: String, principal: Principal): ResponseEntity<Void> {
        return try {
            tournamentService.startTournament(id, principal.name)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/matches/{matchId}/result")
    fun reportResult(@PathVariable matchId: String, @RequestBody request: ReportWinRequest, principal: Principal): ResponseEntity<Void> {
        return try {
            tournamentService.reportMatchResult(matchId, request.winnerId, principal.name)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{id}/advance")
    fun advanceRound(@PathVariable id: String, principal: Principal): ResponseEntity<Void> {
        return try {
            tournamentService.advanceRound(id, principal.name)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}
