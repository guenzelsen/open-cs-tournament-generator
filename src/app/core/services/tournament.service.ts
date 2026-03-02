import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Team, Match, TournamentState } from '../models/tournament.model';
import { firstValueFrom } from 'rxjs';

export interface TournamentListResponse {
    id: string;
    name: string;
    organizerName: string;
    currentRound: number;
    status: string;
    maxRounds: number;
    startTime?: string;
    pictureUrl?: string;
    teams: Team[];
    matches: Match[];
    adminUsernames: string[];
}

@Injectable({
    providedIn: 'root'
})
export class TournamentService {
    private http = inject(HttpClient);
    private apiUrl = 'http://localhost:8080/api/tournaments';

    private tournamentsList = signal<TournamentListResponse[]>([]);
    readonly allTournaments = computed(() => this.tournamentsList());

    activeTournamentDetails = signal<TournamentListResponse | null>(null);
    readonly activeTournament = computed(() => this.activeTournamentDetails());

    // Wrappers to map the selected tournament to the old structure
    readonly teams = computed(() => this.activeTournamentDetails()?.teams || []);
    readonly allMatches = computed(() => this.activeTournamentDetails()?.matches || []);
    readonly activeMatches = computed(() => this.allMatches().filter(m => m.round === this.activeTournamentDetails()?.currentRound));
    readonly currentRound = computed(() => this.activeTournamentDetails()?.currentRound || 0);
    readonly maxRounds = computed(() => this.activeTournamentDetails()?.maxRounds || 4);
    readonly status = computed(() => this.activeTournamentDetails()?.status || 'SETUP');
    readonly adminUsernames = computed(() => this.activeTournamentDetails()?.adminUsernames || []);
    readonly standings = computed(() => {
        const t = this.activeTournamentDetails();
        if (!t) return [];
        return [...t.teams].sort((a, b) => {
            if (a.wins !== b.wins) return b.wins - a.wins;
            return a.losses - b.losses;
        });
    });

    // 0. Loading
    async loadTournaments() {
        try {
            const data = await firstValueFrom(this.http.get<TournamentListResponse[]>(this.apiUrl));
            this.tournamentsList.set(data);
        } catch (e) {
            console.error('Failed to load tournaments', e);
        }
    }

    async loadTournament(id: string) {
        try {
            const data = await firstValueFrom(this.http.get<TournamentListResponse>(`${this.apiUrl}/${id}`));
            this.activeTournamentDetails.set(data);
        } catch (e) {
            console.error('Failed to load tournament details', e);
            throw e;
        }
    }

    clearData() {
        this.tournamentsList.set([]);
        this.activeTournamentDetails.set(null);
    }

    // 1. Setup Phase Methods
    async createTournament(name: string, startTime?: string, pictureUrl?: string) {
        try {
            await firstValueFrom(this.http.post<TournamentListResponse>(this.apiUrl, { name, startTime, pictureUrl }));
            await this.loadTournaments();
        } catch (e) {
            console.error('Failed to create tournament', e);
            throw e;
        }
    }

    async addTeam(tournamentId: string, globalTeamId: string) {
        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/${tournamentId}/teams`, { globalTeamId }));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to add team. Are you the organizer? Does it have 5-6 players?");
        }
    }

    async removeTeam(tournamentId: string, teamId: string) {
        try {
            await firstValueFrom(this.http.delete(`${this.apiUrl}/${tournamentId}/teams/${teamId}`));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to remove team. Are you the organizer?");
        }
    }

    async startTournament(tournamentId: string) {
        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/${tournamentId}/start`, {}));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to start tournament. Check teams or permissions.");
        }
    }

    // 2. CS2 Private Match Code Gen is backend-computed now.

    // 3. Swiss Logic & Round progression
    async reportMatchResult(matchId: string, winnerId: string) {
        const active = this.activeTournamentDetails();
        if (!active) return;

        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/matches/${matchId}/result`, { winnerId }));
            await this.loadTournament(active.id);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to report win. Are you the organizer?");
        }
    }

    async proposeMatchResult(matchId: string, reportedWinnerId: string, reportedScore: string) {
        const active = this.activeTournamentDetails();
        if (!active) return;

        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/matches/${matchId}/propose-result`, { reportedWinnerId, reportedScore }));
            await this.loadTournament(active.id);
        } catch (e: any) {
            console.error(e);
            throw new Error(e.error?.message || "Failed to propose match result.");
        }
    }

    async confirmMatchResult(matchId: string) {
        const active = this.activeTournamentDetails();
        if (!active) return;

        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/matches/${matchId}/confirm-result`, {}));
            await this.loadTournament(active.id);
        } catch (e: any) {
            console.error(e);
            throw new Error(e.error?.message || "Failed to confirm match result.");
        }
    }

    async advanceRound(tournamentId: string) {
        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/${tournamentId}/advance`, {}));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to advance round.");
        }
    }

    // 4. Admin Management
    async addAdmin(tournamentId: string, username: string) {
        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/${tournamentId}/admins`, { username }));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to add admin.");
        }
    }

    async removeAdmin(tournamentId: string, username: string) {
        try {
            await firstValueFrom(this.http.delete(`${this.apiUrl}/${tournamentId}/admins/${username}`));
            await this.loadTournament(tournamentId);
        } catch (e) {
            console.error(e);
            throw new Error("Failed to remove admin.");
        }
    }
}
