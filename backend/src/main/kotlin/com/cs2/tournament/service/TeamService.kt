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

    /**
     * Searches for global teams by name (case-insensitive).
     * @param name The search term to match against team names
     * @return List of matching team responses
     */
    @Transactional(readOnly = true)
    fun searchTeams(name: String): List<TeamResponse> {
        return teamRepository.findByNameContainingIgnoreCase(name).map { toResponse(it) }
    }

    /**
     * Retrieves all teams owned by the specified user.
     * @param username The exact username of the team owner
     * @return List of team responses owned by the user
     */
    @Transactional(readOnly = true)
    fun getMyTeams(username: String): List<TeamResponse> {
        val user = userRepository.findByUsername(username).orElseThrow { IllegalArgumentException("User not found") }
        return teamRepository.findByOwner(user).map { toResponse(it) }
    }

    /**
     * Creates a new global team and assigns the creator as the owner and a default player.
     * @param name The unique name of the team
     * @param ownerUsername The username of the user creating the team
     * @return The created team response
     */
    @Transactional
    fun createTeam(name: String, ownerUsername: String): TeamResponse {
        val user = userRepository.findByUsername(ownerUsername).orElseThrow { IllegalArgumentException("User not found") }
        val t = Team(name = name, owner = user)
        // Add owner as a player by default? Yes, let's assume the owner also plays.
        t.players.add(user)
        return toResponse(teamRepository.save(t))
    }

    /**
     * Adds a player to the team. Only the team owner can perform this action.
     * @param teamId The global team's UUID
     * @param playerUsername The new player to add
     * @param ownerUsername The owner authorizing the addition
     * @return The updated team response
     */
    @Transactional
    fun addPlayerToTeam(teamId: String, playerUsername: String, ownerUsername: String): TeamResponse {
        val t = teamRepository.findById(teamId).orElseThrow { IllegalArgumentException("Team not found") }
        if (t.owner.username != ownerUsername) throw IllegalAccessException("Only owner can add players")
        
        val player = userRepository.findByUsername(playerUsername).orElseThrow { IllegalArgumentException("Player user not found") }
        t.players.add(player)
        return toResponse(teamRepository.save(t))
    }

    /**
     * Removes a player from the team. Only the team owner can perform this action.
     * @param teamId The global team's UUID
     * @param playerUsername The player to remove
     * @param ownerUsername The owner authorizing the removal
     * @return The updated team response
     */
    @Transactional
    fun removePlayerFromTeam(teamId: String, playerUsername: String, ownerUsername: String): TeamResponse {
        val t = teamRepository.findById(teamId).orElseThrow { IllegalArgumentException("Team not found") }
        if (t.owner.username != ownerUsername) throw IllegalAccessException("Only owner can remove players")
        
        t.players.removeIf { it.username == playerUsername }
        return toResponse(teamRepository.save(t))
    }
}
