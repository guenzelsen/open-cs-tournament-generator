package com.cs2.tournament.config

import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import com.cs2.tournament.repository.TeamRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("debug")
class DataInitializerTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var teamRepository: TeamRepository

    @Autowired
    lateinit var tournamentRepository: TournamentRepository

    @Test
    fun `data initializer creates 41 users, 8 teams and one tournament`() {
        // DataInitializer should run on context load.
        // We verify the database is populated.
        assertEquals(41, userRepository.count(), "Should have 41 users (1 admin + 40 players)")
        assertEquals(8, teamRepository.count(), "Should have 8 teams")
        assertEquals(1, tournamentRepository.count(), "Should have 1 tournament")
    }
}
