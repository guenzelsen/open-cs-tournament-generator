package com.cs2.tournament.model

data class Team(
    val id: String,
    val name: String,
    var wins: Int = 0,
    var losses: Int = 0,
    var buchholzScore: Int = 0
)

data class Match(
    val id: String,
    val team1Id: String,
    val team2Id: String,
    var team1Score: Int? = null,
    var team2Score: Int? = null,
    var winnerId: String? = null,
    val privateMatchCode: String,
    val round: Int
)

data class TournamentState(
    var teams: MutableList<Team> = mutableListOf(),
    var matches: MutableList<Match> = mutableListOf(),
    var currentRound: Int = 0,
    var status: TournamentStatus = TournamentStatus.SETUP,
    var maxRounds: Int = 4
)

enum class TournamentStatus {
    SETUP, ACTIVE, FINISHED
}
