package com.cs2.tournament.controller

import com.cs2.tournament.repository.MatchLobbyRepository
import com.cs2.tournament.repository.TeamRepository
import com.cs2.tournament.repository.TournamentRepository
import com.cs2.tournament.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/lobbies")
class LobbyController(
    private val lobbyRepository: MatchLobbyRepository,
    private val tournamentRepository: TournamentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) {
    data class BanRequest(val mapName: String)
    data class LobbyResponse(
        val matchId: String,
        val team1Id: String,
        val team2Id: String,
        val bannedMaps: List<String>,
        val selectedMap: String?
    )

    private val cs2Maps = listOf("Mirage", "Inferno", "Nuke", "Overpass", "Vertigo", "Ancient", "Anubis")

    /**
     * Retrieves the current state of a match lobby.
     * @param matchId The unique ID of the match.
     * @return A response containing the teams, banned maps, and selected map.
     */
    @GetMapping("/{matchId}")
    fun getLobby(@PathVariable matchId: String): ResponseEntity<LobbyResponse> {
        val lobby = lobbyRepository.findByMatchId(matchId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        return ResponseEntity.ok(
            LobbyResponse(
                matchId = lobby.match.id,
                team1Id = lobby.match.team1Id,
                team2Id = lobby.match.team2Id,
                bannedMaps = lobby.bannedMaps,
                selectedMap = lobby.selectedMap
            )
        )
    }

    /**
     * Handles a map ban request during the map veto phase.
     * Only the team captain (owner) whose turn it is may submit a ban.
     * Alternates turns until only 1 map remains, which becomes the selected map.
     * @param matchId The unique ID of the match.
     * @param request The ban request payload containing the map name.
     * @param principal The security principal used to verify user identity.
     * @return The updated lobby state.
     */
    @PostMapping("/{matchId}/ban")
    fun banMap(
        @PathVariable matchId: String, 
        @RequestBody request: BanRequest,
        principal: Principal
    ): ResponseEntity<LobbyResponse> {
        val lobby = lobbyRepository.findByMatchId(matchId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (lobby.selectedMap != null) {
            return ResponseEntity.badRequest().build() // Voting already finished
        }

        if (!cs2Maps.contains(request.mapName) || lobby.bannedMaps.contains(request.mapName)) {
            return ResponseEntity.badRequest().build() // Invalid or already banned map
        }

        // Authenticate Principal is the team owner for whose turn it is
        val user = userRepository.findByUsername(principal.name).orElse(null)
            ?: return ResponseEntity.status(401).build()

        // Turn Logic: Even number of bans means it's Team 1's turn
        val isTeam1Turn = lobby.bannedMaps.size % 2 == 0
        val activeTournamentTeamId = if (isTeam1Turn) lobby.match.team1Id else lobby.match.team2Id

        val tournament = tournamentRepository.findById(lobby.match.tournament.id).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        val activeTournamentTeam = tournament.teams.find { it.id == activeTournamentTeamId }
            ?: return ResponseEntity.notFound().build()

        val globalTeam = teamRepository.findById(activeTournamentTeam.globalTeamId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        // Verify the logged-in user is the owner of the team whose turn it is
        if (globalTeam.owner.id != user.id) {
            return ResponseEntity.status(403).build() // Forbidden - not this user's turn
        }

        // Apply Ban
        lobby.bannedMaps.add(request.mapName)

        // If 6 maps are banned, the 7th is automatically selected
        if (lobby.bannedMaps.size == cs2Maps.size - 1) {
            val remainingMap = cs2Maps.first { !lobby.bannedMaps.contains(it) }
            lobby.selectedMap = remainingMap
        }

        lobbyRepository.save(lobby)
        
        return ResponseEntity.ok(
            LobbyResponse(
                matchId = lobby.match.id,
                team1Id = lobby.match.team1Id,
                team2Id = lobby.match.team2Id,
                bannedMaps = lobby.bannedMaps,
                selectedMap = lobby.selectedMap
            )
        )
    }
}
