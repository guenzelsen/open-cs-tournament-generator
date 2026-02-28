package com.cs2.tournament.service

import com.cs2.tournament.model.Team
import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.TournamentStatus
import com.cs2.tournament.model.User
import com.cs2.tournament.repository.MatchRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
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
    private lateinit var service: TournamentService

    @BeforeEach
    fun setup() {
        tournamentRepository = mock(TournamentRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        matchRepository = mock(MatchRepository::class.java)
        service = TournamentService(tournamentRepository, userRepository, matchRepository)
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
    fun `should fail to start with odd number of teams`() {
        val user = User(id = "user1", username = "organizer", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user)
        tournament.teams = mutableListOf(Team(name = "Navi", tournament = tournament))

        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))

        val exception = assertThrows(IllegalStateException::class.java) {
            service.startTournament("t1", "organizer")
        }
        assertTrue(exception.message!!.contains("Need even number of teams"))
    }

    @Test
    fun `should start tournament and update status to ACTIVE`() {
        val user = User(id = "user1", username = "organizer", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Test Cup", organizer = user)
        tournament.teams = mutableListOf(
            Team(name = "Navi", tournament = tournament),
            Team(name = "FaZe", tournament = tournament)
        )

        `when`(tournamentRepository.findById("t1")).thenReturn(Optional.of(tournament))

        service.startTournament("t1", "organizer")

        assertEquals(TournamentStatus.ACTIVE, tournament.status)
        assertEquals(1, tournament.currentRound)
        assertEquals(1, tournament.matches.size)
    }
}
