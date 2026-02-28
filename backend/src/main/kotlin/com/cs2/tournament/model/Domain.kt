package com.cs2.tournament.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(unique = true) val username: String,
    val passwordHash: String,
    @Column(unique = true) val steamId: String? = null
)

@Entity
@Table(name = "tournaments")
data class Tournament(
    @Id val id: String = UUID.randomUUID().toString(),
    val name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    @JsonIgnore
    val organizer: User,
    var currentRound: Int = 0,
    @Enumerated(EnumType.STRING) var status: TournamentStatus = TournamentStatus.SETUP,
    var maxRounds: Int = 4,

    @OneToMany(mappedBy = "tournament", cascade = [CascadeType.ALL], orphanRemoval = true)
    var teams: MutableList<TournamentTeam> = mutableListOf(),

    @OneToMany(mappedBy = "tournament", cascade = [CascadeType.ALL], orphanRemoval = true)
    var matches: MutableList<Match> = mutableListOf()
)

@Entity
@Table(name = "teams")
data class Team(
    @Id val id: String = UUID.randomUUID().toString(),
    @Column(unique = true) val name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    val owner: User,
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "team_players",
        joinColumns = [JoinColumn(name = "team_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var players: MutableSet<User> = mutableSetOf()
)

@Entity
@Table(name = "tournament_teams")
data class TournamentTeam(
    @Id val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    val tournament: Tournament,
    val name: String,
    val globalTeamId: String,
    var isComplete: Boolean = true,
    var wins: Int = 0,
    var losses: Int = 0,
    var buchholzScore: Int = 0
)

@Entity
@Table(name = "matches")
data class Match(
    @Id val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    val tournament: Tournament,
    val team1Id: String,
    val team2Id: String,
    var team1Score: Int? = null,
    var team2Score: Int? = null,
    var winnerId: String? = null,
    val privateMatchCode: String,
    val round: Int,

    @OneToOne(mappedBy = "match", cascade = [CascadeType.ALL])
    var lobby: MatchLobby? = null
)

@Entity
@Table(name = "match_lobbies")
data class MatchLobby(
    @Id val id: String = UUID.randomUUID().toString(),
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    @JsonIgnore
    val match: Match,
    
    // Simple state mapping to track votes for team 1 vs team 2 
    // We can use an element collection to store map votes.
    @ElementCollection
    @CollectionTable(name = "match_map_votes", joinColumns = [JoinColumn(name = "lobby_id")])
    @MapKeyColumn(name = "map_name")
    @Column(name = "vote_count")
    val mapVotes: MutableMap<String, Int> = mutableMapOf(),

    var selectedMap: String? = null
)

enum class TournamentStatus {
    SETUP, ACTIVE, FINISHED
}
