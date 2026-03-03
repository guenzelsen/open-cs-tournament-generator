package com.cs2.tournament.service

import com.cs2.tournament.repository.MatchLobbyRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class MapBanSchedulerService(
    private val lobbyRepository: MatchLobbyRepository
) {
    private val cs2Maps = listOf(
        "de_ancient",
        "de_dust2",
        "de_inferno",
        "de_mirage",
        "de_nuke",
        "de_overpass",
        "de_anubis"
    )

    /**
     * Periodically checks all active match lobbies for expired map ban turns.
     * If a lobby has gone more than 60 seconds without banning a map, this
     * method automatically bans a random available map for the team, advancing
     * the vote forward, or finalizing the map selection if only one remains.
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    fun processAutoBans() {
        val activeLobbies = lobbyRepository.findAll().filter { it.selectedMap == null && it.lastBanTime != null }
        
        val now = LocalDateTime.now()
        for (lobby in activeLobbies) {
            val lastTime = lobby.lastBanTime!!
            val secondsElapsed = ChronoUnit.SECONDS.between(lastTime, now)
            
            if (secondsElapsed >= 60) {
                // Time expired! Randomly ban a map.
                val availableMaps = cs2Maps.filter { !lobby.bannedMaps.contains(it) }
                if (availableMaps.isNotEmpty()) {
                    lobby.bannedMaps.add(availableMaps.random())
                }

                // If only 1 map remains, select it
                if (lobby.bannedMaps.size == cs2Maps.size - 1) {
                    val remainingMap = cs2Maps.first { !lobby.bannedMaps.contains(it) }
                    lobby.selectedMap = remainingMap
                    lobby.lastBanTime = null
                } else {
                    lobby.lastBanTime = LocalDateTime.now()
                }

                lobbyRepository.save(lobby)
            }
        }
    }
}
