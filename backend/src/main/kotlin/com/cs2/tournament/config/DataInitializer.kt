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
            val users = mutableListOf<User>(admin)
            for (i in 1..40) {
                users.add(User(
                    username = "user$i",
                    passwordHash = passwordEncoder.encode("user")!!,
                    pictureUrl = "https://ui-avatars.com/api/?name=User+$i"
                ))
            }
            userRepository.saveAll(users)

            val teams = mutableListOf<Team>()
            var userIndex = 1
            for (i in 1..8) {
                val teamPlayers = mutableSetOf<User>()
                val owner = users[userIndex]
                for (j in 1..5) {
                    teamPlayers.add(users[userIndex])
                    userIndex++
                }
                teams.add(
                    Team(
                        name = "Debug Team $i",
                        owner = owner,
                        players = teamPlayers
                    )
                )
            }
            teamRepository.saveAll(teams)

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
            println("Users: admin(pass:admin), user1 to user40 (pass:user)")
            println("Teams: 8 teams with 5 players each have been generated")
        }
    }
}
