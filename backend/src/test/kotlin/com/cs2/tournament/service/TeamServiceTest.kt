package com.cs2.tournament.service

import com.cs2.tournament.model.Team
import com.cs2.tournament.model.User
import com.cs2.tournament.repository.TeamRepository
import com.cs2.tournament.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.*

class TeamServiceTest {

    private lateinit var teamRepository: TeamRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: TeamService

    @BeforeEach
    fun setup() {
        teamRepository = mock(TeamRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        service = TeamService(teamRepository, userRepository)
    }

    @Test
    fun `should reject adding duplicate player to team`() {
        val owner = User(id = "user1", username = "owner", passwordHash = "hash")
        val player = User(id = "user2", username = "player1", passwordHash = "hash")
        val team = Team(id = "t1", name = "TestTeam", owner = owner, players = mutableSetOf(owner, player))

        `when`(teamRepository.findById("t1")).thenReturn(Optional.of(team))
        `when`(userRepository.findByUsername("player1")).thenReturn(Optional.of(player))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.addPlayerToTeam("t1", "player1", "owner")
        }
        assertTrue(exception.message!!.contains("already on this team"))
    }

    @Test
    fun `should add new player to team successfully`() {
        val owner = User(id = "user1", username = "owner", passwordHash = "hash")
        val newPlayer = User(id = "user2", username = "newplayer", passwordHash = "hash")
        val team = Team(id = "t1", name = "TestTeam", owner = owner, players = mutableSetOf(owner))

        `when`(teamRepository.findById("t1")).thenReturn(Optional.of(team))
        `when`(userRepository.findByUsername("newplayer")).thenReturn(Optional.of(newPlayer))
        `when`(teamRepository.save(any())).thenReturn(team)

        val result = service.addPlayerToTeam("t1", "newplayer", "owner")
        assertEquals("TestTeam", result.name)
        assertTrue(team.players.contains(newPlayer))
    }
}
