package com.cs2.tournament.service

import com.cs2.tournament.model.Match
import com.cs2.tournament.model.Team
import com.cs2.tournament.model.TournamentState
import com.cs2.tournament.model.TournamentStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
class TournamentService {

    private val state = TournamentState()
    private val lock = Any() // Simple lock for concurrent safety

    // DTO for AddTeam
    data class AddTeamRequest(val name: String)
    data class ReportWinRequest(val winnerId: String)

    fun getState(): TournamentState {
        synchronized(lock) {
            return state.copy() // Should ideally deep copy or DTO
        }
    }

    fun addTeam(request: AddTeamRequest): Team {
        synchronized(lock) {
            if (state.status != TournamentStatus.SETUP) {
                throw IllegalStateException("Tournament is not in SETUP phase.")
            }
            val newTeam = Team(
                id = UUID.randomUUID().toString(),
                name = request.name
            )
            state.teams.add(newTeam)
            return newTeam
        }
    }

    fun removeTeam(id: String) {
        synchronized(lock) {
            if (state.status != TournamentStatus.SETUP) {
                throw IllegalStateException("Tournament is not in SETUP phase.")
            }
            state.teams.removeIf { it.id == id }
        }
    }

    fun startTournament() {
        synchronized(lock) {
            if (state.teams.size < 2 || state.teams.size % 2 != 0) {
                throw IllegalStateException("Need an even number of teams to start.")
            }
            if (state.status != TournamentStatus.SETUP) {
                throw IllegalStateException("Already started.")
            }

            state.status = TournamentStatus.ACTIVE
            state.currentRound = 1
            generatePairings()
        }
    }

    fun reportMatchResult(matchId: String, request: ReportWinRequest) {
        synchronized(lock) {
            val match = state.matches.find { it.id == matchId }
                ?: throw IllegalArgumentException("Match not found")

            if (match.winnerId != null) return // already reported

            match.winnerId = request.winnerId

            val loserId = if (request.winnerId == match.team1Id) match.team2Id else match.team1Id

            state.teams.find { it.id == request.winnerId }?.let { it.wins++ }
            state.teams.find { it.id == loserId }?.let { it.losses++ }
        }
    }

    fun advanceRound() {
        synchronized(lock) {
            val currentActiveMatches = state.matches.filter { it.round == state.currentRound }
            if (currentActiveMatches.any { it.winnerId == null }) {
                throw IllegalStateException("All matches must have a result before advancing.")
            }

            if (state.currentRound >= state.maxRounds) {
                state.status = TournamentStatus.FINISHED
                return
            }

            state.currentRound++
            generatePairings()
        }
    }

    private fun generatePairings() {
        // Sorted standings
        val standings = state.teams.sortedWith(
            compareByDescending<Team> { it.wins }
                .thenBy { it.losses }
        ).toMutableList()

        val newMatches = mutableListOf<Match>()
        val pastMatches = state.matches

        while (standings.size >= 2) {
            val team1 = standings.removeAt(0)

            var opponentIndex = 0
            for (i in standings.indices) {
                val team2 = standings[i]
                val hasPlayed = pastMatches.any {
                    (it.team1Id == team1.id && it.team2Id == team2.id) ||
                    (it.team1Id == team2.id && it.team2Id == team1.id)
                }

                if (!hasPlayed) {
                    opponentIndex = i
                    break
                }
            }

            val team2 = standings.removeAt(opponentIndex)

            newMatches.add(
                Match(
                    id = UUID.randomUUID().toString(),
                    team1Id = team1.id,
                    team2Id = team2.id,
                    round = state.currentRound,
                    privateMatchCode = generateMatchCode()
                )
            )
        }

        state.matches.addAll(newMatches)
    }

    private fun generateMatchCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var result = ""
        val random = java.util.Random()
        for (i in 0 until 6) {
            result += chars[random.nextInt(chars.length)]
        }
        return result
    }
}
