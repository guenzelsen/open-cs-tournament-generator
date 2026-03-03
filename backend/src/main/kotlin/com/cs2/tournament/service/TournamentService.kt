package com.cs2.tournament.service

import com.cs2.tournament.model.Match
import com.cs2.tournament.model.TournamentTeam
import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.TournamentStatus
import com.cs2.tournament.model.MatchLobby
import com.cs2.tournament.repository.MatchRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import com.cs2.tournament.repository.TeamRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository,
    private val teamRepository: TeamRepository
) {

    data class TournamentResponse(
        val id: String,
        val name: String,
        val organizerName: String,
        val currentRound: Int,
        val status: TournamentStatus,
        val maxRounds: Int,
        val startTime: java.time.LocalDateTime?,
        val pictureUrl: String?,
        val teams: List<TournamentTeam>,
        val matches: List<Match>,
        val adminUsernames: List<String>
    )

    fun toResponse(t: Tournament): TournamentResponse {
        return TournamentResponse(
            id = t.id,
            name = t.name,
            organizerName = t.organizer.username,
            currentRound = t.currentRound,
            status = t.status,
            maxRounds = t.maxRounds,
            startTime = t.startTime,
            pictureUrl = t.pictureUrl,
            teams = t.teams,
            matches = t.matches,
            adminUsernames = t.admins.map { it.username }
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
    fun createTournament(name: String, organizerUsername: String, startTime: java.time.LocalDateTime? = null, pictureUrl: String? = null): TournamentResponse {
        val user = userRepository.findByUsername(organizerUsername).orElseThrow { IllegalArgumentException("User not found") }
        val t = Tournament(
            name = name,
            organizer = user,
            startTime = startTime,
            pictureUrl = pictureUrl
        )
        return toResponse(tournamentRepository.save(t))
    }

    @Transactional
    fun addAdmin(tournamentId: String, newAdminUsername: String, requestorUsername: String): TournamentResponse {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != requestorUsername) throw IllegalAccessException("Only organizer can add admins")
        
        val newAdmin = userRepository.findByUsername(newAdminUsername).orElseThrow { IllegalArgumentException("User not found") }
        t.admins.add(newAdmin)
        return toResponse(tournamentRepository.save(t))
    }

    @Transactional
    fun removeAdmin(tournamentId: String, adminUsername: String, requestorUsername: String): TournamentResponse {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != requestorUsername) throw IllegalAccessException("Only organizer can remove admins")
        
        t.admins.removeIf { it.username == adminUsername }
        return toResponse(tournamentRepository.save(t))
    }

    /**
     * Adds a team to a tournament during the SETUP phase.
     * Organizers and admins can add any team. Normal users can only add their own team.
     *
     * @param tournamentId The tournament to add the team to.
     * @param globalTeamId The global team ID to register.
     * @param username The requesting user's username.
     * @return The created tournament team entry.
     * @throws IllegalAccessException If the user lacks permission to add this team.
     * @throws IllegalStateException If the tournament is not in SETUP phase.
     * @throws IllegalArgumentException If the team is already registered or not found.
     */
    @Transactional
    fun addTeam(tournamentId: String, globalTeamId: String, username: String): TournamentTeam {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.status != TournamentStatus.SETUP) throw IllegalStateException("Not in SETUP phase")
        if (t.teams.any { it.globalTeamId == globalTeamId }) throw IllegalArgumentException("Team already added to this tournament")

        val globalTeam = teamRepository.findById(globalTeamId).orElseThrow { IllegalArgumentException("Global team not found") }

        // Permission: organizer and admins can add any team; normal users can only add their own
        val isOrganizerOrAdmin = t.organizer.username == username || t.admins.any { it.username == username }
        if (!isOrganizerOrAdmin && globalTeam.owner.username != username) {
            throw IllegalAccessException("You can only add your own team, or you must be an organizer/admin")
        }

        val isTeamComplete = globalTeam.players.size >= 5

        val newTeam = TournamentTeam(name = globalTeam.name, globalTeamId = globalTeam.id, tournament = t, isComplete = isTeamComplete)
        t.teams.add(newTeam)
        tournamentRepository.save(t)
        return newTeam
    }

    /**
     * Removes a team from a tournament during the SETUP phase.
     * Only the organizer or tournament admins can remove teams.
     *
     * @param tournamentId The tournament to remove the team from.
     * @param teamId The tournament team ID to remove.
     * @param username The requesting user's username.
     * @throws IllegalAccessException If the user is not the organizer or an admin.
     * @throws IllegalStateException If the tournament is not in SETUP phase.
     */
    @Transactional
    fun removeTeam(tournamentId: String, teamId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        val isOrganizerOrAdmin = t.organizer.username == username || t.admins.any { it.username == username }
        if (!isOrganizerOrAdmin) throw IllegalAccessException("Only organizer or admins can remove teams")
        if (t.status != TournamentStatus.SETUP) throw IllegalStateException("Not in SETUP phase")

        t.teams.removeIf { it.id == teamId }
        tournamentRepository.save(t)
    }

    @Transactional
    fun startTournament(tournamentId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        if (t.organizer.username != username) throw IllegalAccessException("Only organizer can start")
        
        // Remove any incomplete teams before starting
        t.teams.removeIf { !it.isComplete }
        
        if (t.teams.size < 2) throw IllegalStateException("Need at least 2 complete teams to start")
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
        val isOrganizerOrAdmin = t.organizer.username == username || t.admins.any { it.username == username }
        if (!isOrganizerOrAdmin) throw IllegalAccessException("Only organizer or admins can report wins")
        if (match.winnerId != null) return

        match.winnerId = winnerId
        val loserId = if (winnerId == match.team1Id) match.team2Id else match.team1Id

        t.teams.find { it.id == winnerId }?.let { it.wins++ }
        t.teams.find { it.id == loserId }?.let { it.losses++ }

        calculateBuchholz(t)
        
        matchRepository.save(match)
        tournamentRepository.save(t)
    }

    /**
     * Allows the losing team of a match to propose the final score and declare the winning team.
     * Validates that the requestor is part of the match and is proposing the opponent as the winner.
     * 
     * @param matchId The unique identifier of the match.
     * @param reportedWinnerId The unique identifier of the tournament team winning the match.
     * @param reportedScore The proposed score (e.g. 13-10).
     * @param requestorUsername The username of the user making the proposal.
     * @throws IllegalArgumentException If the match is not found or the request is logically invalid.
     * @throws IllegalStateException If the match already has a confirmed winner.
     * @throws IllegalAccessException If the user is unassociated with the match.
     */
    @Transactional
    fun proposeMatchResult(matchId: String, reportedWinnerId: String, reportedScore: String, requestorUsername: String) {
        val match = matchRepository.findById(matchId).orElseThrow { IllegalArgumentException("Match not found") }
        if (match.winnerId != null) throw IllegalStateException("Match already has a winner")

        val t = match.tournament
        
        // Find the global team owned by the requestor
        val requestorTeam = t.teams.find { tournamentTeam ->
            val globalTeam = teamRepository.findById(tournamentTeam.globalTeamId).orElse(null)
            globalTeam?.owner?.username == requestorUsername
        }

        if (requestorTeam == null) {
            throw IllegalAccessException("You are not participating in this tournament")
        }

        if (requestorTeam.id != match.team1Id && requestorTeam.id != match.team2Id) {
            throw IllegalAccessException("Your team is not in this match")
        }

        // Feature: Only the loser proposes the score
        if (requestorTeam.id == reportedWinnerId) {
            throw IllegalArgumentException("The losing team must report the score. You cannot report yourself as the winner.")
        }

        match.reportedWinnerId = reportedWinnerId
        match.reportedScore = reportedScore

        matchRepository.save(match)
    }

    /**
     * Allows the proposed winning team to confirm the result proposed by the losing team.
     * Confirms the match result, advances scores, and updates the tournament standings points.
     * 
     * @param matchId The unique identifier of the match.
     * @param requestorUsername The username of the winning team player confirming the loss.
     * @throws IllegalArgumentException If the match is not found.
     * @throws IllegalStateException If the match already has a winner or no result was proposed.
     * @throws IllegalAccessException If the user confirming is not the declared match winner.
     */
    @Transactional
    fun confirmMatchResult(matchId: String, requestorUsername: String) {
        val match = matchRepository.findById(matchId).orElseThrow { IllegalArgumentException("Match not found") }
        if (match.winnerId != null) throw IllegalStateException("Match already has a winner")
        
        val reportedWinnerId = match.reportedWinnerId ?: throw IllegalStateException("No result has been proposed yet")

        val t = match.tournament
        
        // Find the global team owned by the requestor
        val requestorTeam = t.teams.find { tournamentTeam ->
            val globalTeam = teamRepository.findById(tournamentTeam.globalTeamId).orElse(null)
            globalTeam?.owner?.username == requestorUsername
        }

        if (requestorTeam == null || requestorTeam.id != reportedWinnerId) {
            throw IllegalAccessException("Only the reported winning team can confirm the result")
        }

        match.winnerId = reportedWinnerId
        
        val loserId = if (match.team1Id == reportedWinnerId) match.team2Id else match.team1Id
        t.teams.find { it.id == reportedWinnerId }?.let { it.wins++ }
        t.teams.find { it.id == loserId }?.let { it.losses++ }

        calculateBuchholz(t)
        
        matchRepository.save(match)
        tournamentRepository.save(t)
    }

    @Transactional
    fun advanceRound(tournamentId: String, username: String) {
        val t = tournamentRepository.findById(tournamentId).orElseThrow { IllegalArgumentException("Not found") }
        val isOrganizerOrAdmin = t.organizer.username == username || t.admins.any { it.username == username }
        if (!isOrganizerOrAdmin) throw IllegalAccessException("Only organizer or admins can advance")

        val currentActiveMatches = t.matches.filter { it.round == t.currentRound }
        if (currentActiveMatches.any { it.winnerId == null }) {
            throw IllegalStateException("All matches must have a result before advancing.")
        }

        if (t.currentRound >= t.maxRounds && t.status == TournamentStatus.ACTIVE) {
            // Swiss rounds are over. Transition to Playoffs if enough teams exist.
            if (t.teams.size > 6) {
                t.status = TournamentStatus.QUARTER_FINALS
                generatePlayoffs(t, 8)
            } else if (t.teams.size in 4..6) {
                t.status = TournamentStatus.SEMI_FINALS
                generatePlayoffs(t, 4)
            } else if (t.teams.size in 2..3) {
                t.status = TournamentStatus.FINALS
                generatePlayoffs(t, 2)
            } else {
                t.status = TournamentStatus.FINISHED
            }
        } else if (t.status == TournamentStatus.QUARTER_FINALS) {
            t.status = TournamentStatus.SEMI_FINALS
            generatePlayoffsFromPrevious(t, TournamentStatus.QUARTER_FINALS)
        } else if (t.status == TournamentStatus.SEMI_FINALS) {
            t.status = TournamentStatus.FINALS
            generatePlayoffsFromPrevious(t, TournamentStatus.SEMI_FINALS)
        } else if (t.status == TournamentStatus.FINALS) {
            t.status = TournamentStatus.FINISHED
        } else {
            // Still in ACTIVE (Swiss)
            t.currentRound++
            generatePairings(t)
        }
        tournamentRepository.save(t)
    }

    /**
     * Calculates the Buchholz score for all teams in a tournament.
     * Buchholz score = sum of wins of all opponents a team has faced.
     * Used as a tiebreaker in Swiss-system tournament standings.
     *
     * @param t The tournament to calculate Buchholz scores for.
     */
    private fun calculateBuchholz(t: Tournament) {
        val completedMatches = t.matches.filter { it.winnerId != null }
        for (team in t.teams) {
            val opponentIds = completedMatches
                .filter { it.team1Id == team.id || it.team2Id == team.id }
                .map { if (it.team1Id == team.id) it.team2Id else it.team1Id }
            team.buchholzScore = opponentIds.sumOf { oppId ->
                t.teams.find { it.id == oppId }?.wins ?: 0
            }
        }
    }

    /**
     * Generates Swiss-system pairings for the current round.
     * Teams are sorted by standings (wins desc, losses asc, Buchholz desc) and
     * paired with the closest-ranked opponent they haven't played yet.
     * Handles odd numbers of teams via a BYE that rotates across rounds.
     *
     * @param t The tournament to generate pairings for.
     */
    private fun generatePairings(t: Tournament) {
        val standings = t.teams.sortedWith(
            compareByDescending<TournamentTeam> { it.wins }
                .thenBy { it.losses }
                .thenByDescending { it.buchholzScore }
        ).toMutableList()

        val newMatches = mutableListOf<Match>()
        val pastMatches = t.matches

        // Handle odd number of teams: give BYE to the lowest-ranked team that hasn't had one yet.
        // If all teams have had a BYE, reset the tracker and start over.
        if (standings.size % 2 != 0) {
            if (t.byeTeamIds.size >= standings.size) {
                t.byeTeamIds.clear()
            }
            val byeTeam = standings.lastOrNull { it.id !in t.byeTeamIds }
                ?: standings.last()
            standings.remove(byeTeam)
            byeTeam.wins++
            t.byeTeamIds.add(byeTeam.id)
        }

        while (standings.size >= 2) {
            val team1 = standings.removeAt(0)
            var opponentIndex = 0
            var foundUnplayed = false
            for (i in standings.indices) {
                val team2 = standings[i]
                val hasPlayed = pastMatches.any {
                    (it.team1Id == team1.id && it.team2Id == team2.id) ||
                    (it.team1Id == team2.id && it.team2Id == team1.id)
                }
                if (!hasPlayed) {
                    opponentIndex = i
                    foundUnplayed = true
                    break
                }
            }

            if (!foundUnplayed) {
                // All remaining opponents have been played; pair with closest-ranked (index 0) as fallback
                opponentIndex = 0
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

    /**
     * Generates a random 6-character alphanumeric match code using a secure random generator.
     * @return A 6-character uppercase alphanumeric string.
     */
    private fun generateMatchCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.security.SecureRandom()
        return (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    /**
     * Generates the initial playoff bracket based on Swiss standings.
     * Seeds: 1 vs 8, 2 vs 7, 3 vs 6, 4 vs 5 (for an 8-team bracket).
     * If there are fewer teams than the bracket size, the top seeds get a bye.
     */
    private fun generatePlayoffs(t: Tournament, bracketSize: Int) {
        t.currentRound++ // Increment round for the playoff phase
        
        val standings = t.teams.sortedWith(
            compareByDescending<TournamentTeam> { it.wins }
                .thenBy { it.losses }
                .thenByDescending { it.buchholzScore }
        )

        // Take top N teams
        val playoffTeams = standings.take(bracketSize).toMutableList()

        // If we don't have enough teams, we pad with nulls to represent BYEs
        val seeds = MutableList<TournamentTeam?>(bracketSize) { null }
        for (i in playoffTeams.indices) {
            seeds[i] = playoffTeams[i]
        }

        // Standard seeding for Single Elimination
        // For 8: 1v8, 4v5, 2v7, 3v6 is conventional, but for simplicity we will pair:
        // top half vs bottom half reversed: 1 vs N, 2 vs N-1, etc.
        val matchesToPlay = bracketSize / 2
        for (i in 0 until matchesToPlay) {
            val highSeed = seeds[i]
            val lowSeed = seeds[bracketSize - 1 - i]

            if (highSeed != null) {
                // Determine if this is a bye match (lowSeed is null)
                val match = Match(
                    tournament = t,
                    team1Id = highSeed.id,
                    team2Id = lowSeed?.id ?: highSeed.id, // If bye, just set both to highSeed to easily track or we can set team2Id to a dummy, but let's handle byes
                    round = t.currentRound,
                    privateMatchCode = generateMatchCode(),
                    winnerId = if (lowSeed == null) highSeed.id else null // Auto-win if BYE
                )
                
                // If it's not a bye, give it a lobby
                if (lowSeed != null) {
                    val lobby = MatchLobby(match = match)
                    match.lobby = lobby
                }

                t.matches.add(match)
            }
        }
    }

    /**
     * Generates the next round of playoffs from the previous phase's winners.
     */
    private fun generatePlayoffsFromPrevious(t: Tournament, previousPhase: TournamentStatus) {
        val previousRound = t.currentRound
        t.currentRound++

        // Get the winners from the previous round
        // Note: this assumes we are strictly flowing from QF -> SF -> F
        // so the 'previousRound' matches are the ones we just finished.
        val previousMatches = t.matches.filter { it.round == previousRound }
        
        // Ensure all have winners (should be true due to advanceRound check)
        val winners = previousMatches.mapNotNull { match ->
            t.teams.find { it.id == match.winnerId }
        }

        // Pair them up 1v2, 3v4
        for (i in 0 until winners.size step 2) {
            if (i + 1 < winners.size) {
                val team1 = winners[i]
                val team2 = winners[i + 1]
                
                val match = Match(
                    tournament = t,
                    team1Id = team1.id,
                    team2Id = team2.id,
                    round = t.currentRound,
                    privateMatchCode = generateMatchCode()
                )
                val lobby = MatchLobby(match = match)
                match.lobby = lobby
                
                t.matches.add(match)
            } else {
                // Odd number of winners? Shouldn't happen in a fixed bracket unless byes were weird,
                // but if so, advance them via a bye.
                val team = winners[i]
                val match = Match(
                    tournament = t,
                    team1Id = team.id,
                    team2Id = team.id,
                    round = t.currentRound,
                    privateMatchCode = generateMatchCode(),
                    winnerId = team.id
                )
                t.matches.add(match)
            }
        }
    }
}
