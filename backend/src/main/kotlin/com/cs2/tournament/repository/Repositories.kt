package com.cs2.tournament.repository

import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.User
import com.cs2.tournament.model.Match
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, String> {
    fun findByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
}

@Repository
interface TournamentRepository : JpaRepository<Tournament, String> {
    fun findByOrganizerId(organizerId: String): List<Tournament>
}

@Repository
interface MatchRepository : JpaRepository<Match, String> {
    fun findByTournamentId(tournamentId: String): List<Match>
}

@Repository
interface MatchLobbyRepository : JpaRepository<com.cs2.tournament.model.MatchLobby, String> {
    fun findByMatchId(matchId: String): Optional<com.cs2.tournament.model.MatchLobby>
}
