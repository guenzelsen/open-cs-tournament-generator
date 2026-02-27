package com.cs2.tournament.controller

import com.cs2.tournament.model.Team
import com.cs2.tournament.model.TournamentState
import com.cs2.tournament.service.TournamentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tournament")
class TournamentController(
    private val tournamentService: TournamentService
) {

    @GetMapping("/state")
    fun getState(): ResponseEntity<TournamentState> {
        return ResponseEntity.ok(tournamentService.getState())
    }

    @PostMapping("/teams")
    fun addTeam(@RequestBody request: TournamentService.AddTeamRequest): ResponseEntity<Team> {
        return try {
            val team = tournamentService.addTeam(request)
            ResponseEntity.ok(team)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/teams/{id}")
    fun removeTeam(@PathVariable id: String): ResponseEntity<Void> {
        return try {
            tournamentService.removeTeam(id)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/start")
    fun startTournament(): ResponseEntity<Void> {
        return try {
            tournamentService.startTournament()
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/matches/{matchId}/result")
    fun reportResult(@PathVariable matchId: String, @RequestBody request: TournamentService.ReportWinRequest): ResponseEntity<Void> {
        return try {
            tournamentService.reportMatchResult(matchId, request)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/advance")
    fun advanceRound(): ResponseEntity<Void> {
        return try {
            tournamentService.advanceRound()
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
}
