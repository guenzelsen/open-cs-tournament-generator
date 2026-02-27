package com.cs2.tournament.service

import com.cs2.tournament.model.TournamentStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TournamentServiceTest {

    private val service = TournamentService()

    @Test
    fun `should add teams correctly`() {
        val team = service.addTeam(TournamentService.AddTeamRequest("Navi"))
        assertEquals("Navi", team.name)
        assertEquals(1, service.getState().teams.size)
    }

    @Test
    fun `should fail to start with odd number of teams`() {
        service.addTeam(TournamentService.AddTeamRequest("Navi"))
        val exception = assertThrows<IllegalStateException> {
            service.startTournament()
        }
        assertTrue(exception.message!!.contains("even number of teams"))
    }

    @Test
    fun `should start tournament and generate pairings`() {
        service.addTeam(TournamentService.AddTeamRequest("Navi"))
        service.addTeam(TournamentService.AddTeamRequest("FaZe"))
        service.addTeam(TournamentService.AddTeamRequest("Vitality"))
        service.addTeam(TournamentService.AddTeamRequest("G2"))

        service.startTournament()

        val state = service.getState()
        assertEquals(TournamentStatus.ACTIVE, state.status)
        assertEquals(1, state.currentRound)
        assertEquals(2, state.matches.size)
        // Check private match codes
        assertEquals(6, state.matches[0].privateMatchCode.length)
    }
}
