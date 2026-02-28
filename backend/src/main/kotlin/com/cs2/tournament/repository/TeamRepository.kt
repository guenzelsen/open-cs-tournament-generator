package com.cs2.tournament.repository

import com.cs2.tournament.model.Team
import com.cs2.tournament.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TeamRepository : JpaRepository<Team, String> {
    fun findByNameContainingIgnoreCase(name: String): List<Team>
    fun findByOwner(owner: User): List<Team>
}
