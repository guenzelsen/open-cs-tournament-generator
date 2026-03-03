package com.cs2.tournament.service

import com.cs2.tournament.model.MatchLobby
import com.cs2.tournament.model.Match
import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.User
import com.cs2.tournament.repository.MatchLobbyRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDateTime

class MapBanSchedulerServiceTest {

    private lateinit var lobbyRepository: MatchLobbyRepository
    private lateinit var scheduler: MapBanSchedulerService

    @BeforeEach
    fun setup() {
        lobbyRepository = mock(MatchLobbyRepository::class.java)
        scheduler = MapBanSchedulerService(lobbyRepository)
    }

    @Test
    fun `should auto-ban a map if 60 seconds have passed`() {
        val user = User(id = "1", username = "u1", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Cup", organizer = user)
        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "ABCDEF")
        val lobby = MatchLobby(match = match, lastBanTime = LocalDateTime.now().minusSeconds(65))

        `when`(lobbyRepository.findAll()).thenReturn(listOf(lobby))

        scheduler.processAutoBans()

        verify(lobbyRepository, times(1)).save(lobby)
        assertEquals(1, lobby.bannedMaps.size)
        // Verify the auto-banned map uses de_ names (Bug #1 fix)
        val expectedMaps = listOf("de_ancient", "de_dust2", "de_inferno", "de_mirage", "de_nuke", "de_overpass", "de_anubis")
        assertTrue(expectedMaps.contains(lobby.bannedMaps[0]), "Auto-banned map should use de_ names, got: ${lobby.bannedMaps[0]}")
    }

    @Test
    fun `should completely skip if time has not passed`() {
        val user = User(id = "1", username = "u1", passwordHash = "hash")
        val tournament = Tournament(id = "t1", name = "Cup", organizer = user)
        val match = Match(id = "m1", tournament = tournament, team1Id = "tt1", team2Id = "tt2", round = 1, privateMatchCode = "ABCDEF")
        val lobby = MatchLobby(match = match, lastBanTime = LocalDateTime.now().minusSeconds(30))

        `when`(lobbyRepository.findAll()).thenReturn(listOf(lobby))

        scheduler.processAutoBans()

        verify(lobbyRepository, never()).save(lobby)
        assertEquals(0, lobby.bannedMaps.size)
    }
}
