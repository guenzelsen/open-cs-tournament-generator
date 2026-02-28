package com.cs2.tournament.config

import com.cs2.tournament.model.Tournament
import com.cs2.tournament.model.TournamentStatus
import com.cs2.tournament.model.User
import com.cs2.tournament.model.Team
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import com.cs2.tournament.repository.TeamRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@Profile("debug")
class DataInitializer(
    private val userRepository: UserRepository,
    private val tournamentRepository: TournamentRepository,
    private val teamRepository: TeamRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (userRepository.count() == 0L) {
            val admin = User(
                username = "admin",
                passwordHash = passwordEncoder.encode("admin")!!,
                pictureUrl = "https://ui-avatars.com/api/?name=Admin"
            )
            val user1 = User(
                username = "user1",
                passwordHash = passwordEncoder.encode("user")!!,
                pictureUrl = "https://ui-avatars.com/api/?name=User+One"
            )
            val user2 = User(
                username = "user2",
                passwordHash = passwordEncoder.encode("user")!!,
                pictureUrl = "https://ui-avatars.com/api/?name=User+Two"
            )
            userRepository.saveAll(listOf(admin, user1, user2))

            val team1 = Team(
                name = "Debug Team Alpha",
                owner = user1,
                players = mutableSetOf(user1, admin)
            )
            
            val team2 = Team(
                name = "Debug Team Bravo",
                owner = user2,
                players = mutableSetOf(user2)
            )
            teamRepository.saveAll(listOf(team1, team2))

            val dummyTournament = Tournament(
                name = "Debug CS2 Championship",
                organizer = admin,
                status = TournamentStatus.SETUP,
                maxRounds = 4,
                startTime = LocalDateTime.now().plusDays(1)
            )
            tournamentRepository.save(dummyTournament)
            
            println("==============================")
            println("=== Debug Data Initialized ===")
            println("==============================")
            println("Users: admin(pass:admin), user1(pass:user), user2(pass:user)")
        }
    }
}
