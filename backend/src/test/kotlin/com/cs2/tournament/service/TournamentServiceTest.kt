package com.cs2.tournament.service

import com.cs2.tournament.model.TournamentTeam
import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.TournamentStatus
import com.cs2.tournament.model.User
import com.cs2.tournament.model.Match
import com.cs2.tournament.repository.MatchRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import com.cs2.tournament.repository.TeamRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.util.*

class TournamentServiceTest {

    private lateinit var tournamentRepository: TournamentRepository
    private lateinit var userRepository: UserRepository
    private lateinit var matchRepository: MatchRepository
    private lateinit var teamRepository: TeamRepository
    private lateinit var service: TournamentService

    @BeforeEach
    fun setup() {
        tournamentRepository = mock(TournamentRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        matchRepository = mock(MatchRepository::class.java)
        teamRepository = mock(TeamRepository::class.java)
        service = TournamentService(tournamentRepository, userRepository, matchRepository, teamRepository)
    }

    @Test
    fun `should create tournament and return response with organizerName`() {
        val user = User(id = "user1", username = "fguenzelsen", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user)
        
        `when`(userRepository.findByUsername("fguenzelsen")).thenReturn(Optional.of(user))
        `when`(tournamentRepository.save(any())).thenReturn(tournament)

        val response = service.createTournament("Test Cup", "fguenzelsen")

        assertEquals("Test Cup", response.name)
        assertEquals("fguenzelsen", response.organizerName)
        assertEquals(TournamentStatus.SETUP, response.status)
    }

    @Test
    fun `should fail to start with less than two complete teams`() {
        val user = User(id = "user1", username = "organizer", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user)
        // One incomplete team, one complete team. Incomplete is removed, leaving 1 complete team. -> Fails.
        tournament.teams = mutableListOf(
            TournamentTeam(name = "Navi", globalTeamId = "g1", tournament = tournament, isComplete = false),
            TournamentTeam(name = "FaZe", globalTeamId = "g2", tournament = tournament, isComplete = true)
        )

        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))

        val exception = assertThrows(IllegalStateException::class.java) {
            service.startTournament("t1", "organizer")
        }
        assertTrue(exception.message!!.contains("Need at least 2 complete teams to start"))
    }

    @Test
    fun `should start tournament and update status to ACTIVE`() {
        val user = User(id = "user1", username = "organizer", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user)
        tournament.teams = mutableListOf(
            TournamentTeam(name = "Navi", globalTeamId = "g1", tournament = tournament, isComplete = true),
            TournamentTeam(name = "FaZe", globalTeamId = "g2", tournament = tournament, isComplete = true)
        )

        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))

        service.startTournament("t1", "organizer")

        assertEquals(TournamentStatus.ACTIVE, tournament.status)
        assertEquals(1, tournament.currentRound)
        assertEquals(1, tournament.matches.size)
    }

    @Test
    fun `should allow admin to report match result`() {
        val organizer = User(id = "user1", username = "organizer", passwordHash = "hash")
        val admin = User(id = "user2", username = "adminUser", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = organizer, admins = mutableSetOf(admin))
        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "AAAAAA")

        `when`(matchRepository.findById("m1")).thenReturn(Optional.of(match))
        
        service.reportMatchResult("m1", "tt1", "adminUser")

        assertEquals("tt1", match.winnerId)
    }

    @Test
    fun `should restrict non-admin from reporting match result`() {
        val organizer = User(id = "user1", username = "organizer", passwordHash = "hash")
        val normalUser = User(id = "user3", username = "pleb", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = organizer)
        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "AAAAAA")

        `when`(matchRepository.findById("m1")).thenReturn(Optional.of(match))
        
        val exception = assertThrows(IllegalAccessException::class.java) {
            service.reportMatchResult("m1", "tt1", "pleb")
        }
        assertTrue(exception.message!!.contains("organizer or admins"))
    }
}
