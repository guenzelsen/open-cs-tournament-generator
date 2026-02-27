package com.cs2.tournament.controller

import com.cs2.tournament.repository.MatchLobbyRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/lobbies")
class LobbyController(
    private val lobbyRepository: MatchLobbyRepository
) {
    data class VoteRequest(val mapName: String)
    data class LobbyResponse(
        val matchId: String,
        val team1Id: String,
        val team2Id: String,
        val mapVotes: Map<String, Int>,
        val selectedMap: String?
    )

    @GetMapping("/{matchId}")
    fun getLobby(@PathVariable matchId: String): ResponseEntity<LobbyResponse> {
        val lobby = lobbyRepository.findByMatchId(matchId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        return ResponseEntity.ok(
            LobbyResponse(
                matchId = lobby.match.id,
                team1Id = lobby.match.team1Id,
                team2Id = lobby.match.team2Id,
                mapVotes = lobby.mapVotes,
                selectedMap = lobby.selectedMap
            )
        )
    }

    @PostMapping("/{matchId}/vote")
    fun voteMap(@PathVariable matchId: String, @RequestBody request: VoteRequest): ResponseEntity<LobbyResponse> {
        val lobby = lobbyRepository.findByMatchId(matchId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (lobby.selectedMap == null) {
            val validMaps = listOf("Mirage", "Inferno", "Nuke", "Overpass", "Vertigo", "Ancient", "Anubis", "Dust II")
            if (validMaps.contains(request.mapName)) {
                lobby.mapVotes[request.mapName] = (lobby.mapVotes[request.mapName] ?: 0) + 1
                
                // Simple MVP voting: if a map gets 2 votes, it is selected.
                if ((lobby.mapVotes[request.mapName] ?: 0) >= 2) {
                    lobby.selectedMap = request.mapName
                }
                lobbyRepository.save(lobby)
            }
        }
        
        return ResponseEntity.ok(
            LobbyResponse(
                matchId = lobby.match.id,
                team1Id = lobby.match.team1Id,
                team2Id = lobby.match.team2Id,
                mapVotes = lobby.mapVotes,
                selectedMap = lobby.selectedMap
            )
        )
    }
}
