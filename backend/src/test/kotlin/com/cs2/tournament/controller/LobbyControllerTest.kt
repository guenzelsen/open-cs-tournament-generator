package com.cs2.tournament.controller

import com.cs2.tournament.model.*
import com.cs2.tournament.repository.MatchLobbyRepository
import com.cs2.tournament.repository.TeamRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.ResponseEntity
import java.security.Principal
import java.util.*

class LobbyControllerTest {

    private lateinit var lobbyRepository: MatchLobbyRepository
    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var teamRepository: TeamRepository
    private lateinit var userRepository: UserRepository
    private lateinit var controller: LobbyController

    @BeforeEach
    fun setup() {
        lobbyRepository = mock(MatchLobbyRepository::class.java)
        tournamentRepository = mock(TournamentRepository::class.java)
        teamRepository = mock(TeamRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        controller = LobbyController(lobbyRepository, tournamentRepository, teamRepository, userRepository)
    }

    @Test
    fun `should allow team 1 captain to ban map on round 0`() {
        // Setup Models
        val user1 = User(id = "user1", username = "captain1", passwordHash = "hash")
        val globalTeam1 = Team(id = "gt1", name = "Global Team 1", owner = user1)
        
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user1)
        val tourneyTeam1 = TournamentTeam(id = "tt1", name = "Global Team 1", globalTeamId = "gt1", tournament = tournament)
        val tourneyTeam2 = TournamentTeam(id = "tt2", name = "Global Team 2", globalTeamId = "gt2", tournament = tournament)
        tournament.teams = mutableListOf(tourneyTeam1, tourneyTeam2)

        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "AAAAAA")
        val lobby = MatchLobby(id = "l1", match = match, bannedMaps = mutableListOf())

        // Setup Mocks
        val principal = mock(Principal::class.java)
        `when`(principal.name).thenReturn("captain1")
        `when`(userRepository.findByUsername("captain1")).thenReturn(Optional.of(user1))
        `when`(lobbyRepository.findByMatchId("m1")).thenReturn(Optional.of(lobby))
        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))
        `when`(teamRepository.findById("gt1")).thenReturn(Optional.of(globalTeam1))

        // Execute
        val response = controller.banMap("m1", LobbyController.BanRequest("de_mirage"), principal)
        
        // Assert
        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!.bannedMaps.contains("de_mirage"))
    }
    
    @Test
    fun `should prevent team 2 captain from banning on round 0`() {
        // Setup Models
        val user1 = User(id = "user1", username = "captain1", passwordHash = "hash")
        val user2 = User(id = "user2", username = "captain2", passwordHash = "hash")
        val globalTeam1 = Team(id = "gt1", name = "Global Team 1", owner = user1) // Real owner is captain1
        
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user1)
        val tourneyTeam1 = TournamentTeam(id = "tt1", name = "Global Team 1", globalTeamId = "gt1", tournament = tournament)
        val tourneyTeam2 = TournamentTeam(id = "tt2", name = "Global Team 2", globalTeamId = "gt2", tournament = tournament)
        tournament.teams = mutableListOf(tourneyTeam1, tourneyTeam2)

        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "AAAAAA")
        // Size is 0, meaning it's team 1's turn
        val lobby = MatchLobby(id = "l1", match = match, bannedMaps = mutableListOf())

        // Setup Mocks
        val principal = mock(Principal::class.java)
        `when`(principal.name).thenReturn("captain2") // captain2 trying to ban on round 0
        `when`(userRepository.findByUsername("captain2")).thenReturn(Optional.of(user2))
        `when`(lobbyRepository.findByMatchId("m1")).thenReturn(Optional.of(lobby))
        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))
        `when`(teamRepository.findById("gt1")).thenReturn(Optional.of(globalTeam1))

        // Execute
        val response = controller.banMap("m1", LobbyController.BanRequest("de_mirage"), principal)
        
        // Assert: Forbidden (403) because it's team 1's turn, but logged user is not team 1's owner
        assertEquals(403, response.statusCode.value())
    }
}
