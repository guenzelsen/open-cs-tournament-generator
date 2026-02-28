package com.cs2.tournament.service

import com.cs2.tournament.model.Match
import com.cs2.tournament.model.Team
import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.TournamentStatus
import com.cs2.tournament.model.MatchLobby
import com.cs2.tournament.repository.MatchRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository
) {

    data class TournamentResponse(
        val id: String,
        val name: String,
        val organizerName: String,
        val currentRound: Int,
        val status: TournamentStatus,
        val maxRounds: Int,
        val teams: List<Team>,
        val matches: List<Match>
    )

    fun toResponse(t: Tournament): TournamentResponse {
        return TournamentResponse(
            id = t.id,
            name = t.name,
            organizerName = t.organizer.username,
            currentRound = t.currentRound,
            status = t.status,
            maxRounds = t.maxRounds,
            teams = t.teams,
            matches = t.matches
        )
    }

    @Transactional(readOnly = true)
    fun getAllTournaments(): List<TournamentResponse> {
        return tournamentRepository.findAll().map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getTournament(id: String): TournamentResponse {
        val t = tournamentRepository.findById(id).orElseThrow { IllegalArgumentException("Not found") }
        return toResponse(t)
    }

    @Transactional
    fun createTournament(name: String, organizerUsername: String): TournamentResponse {
        val user = userRepository.findByUsername(organizerUsername).orElseThrow { IllegalArgumentException("User not found") }
        val t = Tournament(
            name = name,
            organizer = user
        )
        return toResponse(tournamentRepository.save(t))
    }

    @Transactional
    fun addTeam(tournamentId: String, teamName: String, username: String): Team {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can add teams")
        if (t.status != TournamentStatus.SETUP) throw IllegalStateException("Not in SETUP phase")

        val newTeam = Team(name = teamName, tournament = t)
        t.teams.add(newTeam)
        tournamentRepository.save(t)
        return newTeam
    }

    @Transactional
    fun removeTeam(tournamentId: String, teamId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can remove teams")
        if (t.status != TournamentStatus.SETUP) throw IllegalStateException("Not in SETUP phase")

        t.teams.removeIf { it.id == teamId }
        tournamentRepository.save(t)
    }

    @Transactional
    fun startTournament(tournamentId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can start")
        if (t.teams.size < 2 || t.teams.size % 2 != 0) throw IllegalStateException("Need even number of teams")
        if (t.status != TournamentStatus.SETUP) throw IllegalStateException("Already started")

        t.status = TournamentStatus.ACTIVE
        t.currentRound = 1
        generatePairings(t)
        tournamentRepository.save(t)
    }

    @Transactional
    fun reportMatchResult(matchId: String, winnerId: String, username: String) {
        val match = matchRepository.findById(matchId).orElseThrow { IllegalArgumentException("Match not found") }
        val t = match.tournament
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can report wins")
        if (match.winnerId != null) return

        match.winnerId = winnerId
        val loserId = if (winnerId == match.team1Id) match.team2Id else match.team1Id

        t.teams.find { it.id == winnerId }?.let { it.wins++ }
        t.teams.find { it.id == loserId }?.let { it.losses++ }
        
        matchRepository.save(match)
        tournamentRepository.save(t)
    }

    @Transactional
    fun advanceRound(tournamentId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can advance")

        val currentActiveMatches = t.matches.filter { it.round == t.currentRound }
        if (currentActiveMatches.any { it.winnerId == null }) {
            throw IllegalStateException("All matches must have a result before advancing.")
        }

        if (t.currentRound >= t.maxRounds) {
            t.status = TournamentStatus.FINISHED
        } else {
            t.currentRound++
            generatePairings(t)
        }
        tournamentRepository.save(t)
    }

    private fun generatePairings(t: Tournament) {
        val standings = t.teams.sortedWith(
            compareByDescending<Team> { it.wins }.thenBy { it.losses }
        ).toMutableList()

        val newMatches = mutableListOf<Match>()
        val pastMatches = t.matches

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
            val match = Match(
                tournament = t,
                team1Id = team1.id,
                team2Id = team2.id,
                round = t.currentRound,
                privateMatchCode = generateMatchCode()
            )
            // Create the lobby for the match automatically
            val lobby = MatchLobby(match = match)
            match.lobby = lobby
            
            newMatches.add(match)
        }
        t.matches.addAll(newMatches)
    }

    private fun generateMatchCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.util.Random()
        return (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
