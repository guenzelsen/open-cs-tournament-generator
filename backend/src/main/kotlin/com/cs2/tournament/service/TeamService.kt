package com.cs2.tournament.service

import com.cs2.tournament.model.Team
import com.cs2.tournament.repository.TeamRepository
import com.cs2.tournament.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) {

    data class TeamResponse(
        val id: String,
        val name: String,
        val ownerUsername: String,
        val playerUsernames: List<String>
    )

    fun toResponse(t: Team): TeamResponse {
        return TeamResponse(
            id = t.id,
            name = t.name,
            ownerUsername = t.owner.username,
            playerUsernames = t.players.map { it.username }
        )
    }

    @Transactional(readOnly = true)
    fun searchTeams(name: String): List<TeamResponse> {
        return teamRepository.findByNameContainingIgnoreCase(name).map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getMyTeams(username: String): List<TeamResponse> {
        val user = userRepository.findByUsername(username).orElseThrow { IllegalArgumentException("User not found") }
        return teamRepository.findByOwner(user).map { toResponse(it) }
    }

    @Transactional
    fun createTeam(name: String, ownerUsername: String): TeamResponse {
        val user = userRepository.findByUsername(ownerUsername).orElseThrow { IllegalArgumentException("User not found") }
        val t = Team(name = name, owner = user)
        // Add owner as a player by default? Yes, let's assume the owner also plays.
        t.players.add(user)
        return toResponse(teamRepository.save(t))
    }

    @Transactional
    fun addPlayerToTeam(teamId: String, playerUsername: String, ownerUsername: String): TeamResponse {
        val t = teamRepository.findById(teamId).orElseThrow { IllegalArgumentException("Team not found") }
        if (t.owner.username != ownerUsername) throw IllegalAccessException("Only owner can add players")
        
        val player = userRepository.findByUsername(playerUsername).orElseThrow { IllegalArgumentException("Player user not found") }
        t.players.add(player)
        return toResponse(teamRepository.save(t))
    }

    @Transactional
    fun removePlayerFromTeam(teamId: String, playerUsername: String, ownerUsername: String): TeamResponse {
        val t = teamRepository.findById(teamId).orElseThrow { IllegalArgumentException("Team not found") }
        if (t.owner.username != ownerUsername) throw IllegalAccessException("Only owner can remove players")
        
        t.players.removeIf { it.username == playerUsername }
        return toResponse(teamRepository.save(t))
    }
}
