import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Team, Match, TournamentState } from '../models/tournament.model';
import { firstValueFrom } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class TournamentService {
    private http = inject(HttpClient);
    // Default to localhost:8080. If deploying to Pi, this could be configured via environment variables.
    private apiUrl = 'http://localhost:8080/api/tournament';

    // State 
    private state = signal<TournamentState>({
        teams: [],
        matches: [],
        currentRound: 0,
        status: 'SETUP',
        maxRounds: 4
    });

    // Selectors
    readonly teams = computed(() => this.state().teams);
    readonly activeMatches = computed(() => this.state().matches.filter(m => m.round === this.state().currentRound));
    readonly allMatches = computed(() => this.state().matches);
    readonly currentRound = computed(() => this.state().currentRound);
    readonly maxRounds = computed(() => this.state().maxRounds);
    readonly status = computed(() => this.state().status);

    readonly standings = computed(() => {
        return [...this.state().teams].sort((a, b) => {
            if (a.wins !== b.wins) return b.wins - a.wins;
            if (a.losses !== b.losses) return a.losses - b.losses;
            return 0;
        });
    });

    constructor() {
        this.refreshState(); // Load initial state from server
    }

    async refreshState() {
        try {
            const state = await firstValueFrom(this.http.get<TournamentState>(`${this.apiUrl}/state`));
            this.state.set(state);
        } catch (e) {
            console.error("Failed to load state from backend", e);
        }
    }

    async addTeam(name: string) {
        if (this.state().status !== 'SETUP') return;
        try {
            await firstValueFrom(this.http.post<Team>(`${this.apiUrl}/teams`, { name }));
            await this.refreshState();
        } catch (e) {
            console.error(e);
            alert("Failed to add team. Ensure backend is running.");
        }
    }

    async removeTeam(id: string) {
        if (this.state().status !== 'SETUP') return;
        try {
            await firstValueFrom(this.http.delete(`${this.apiUrl}/teams/${id}`));
            await this.refreshState();
        } catch (e) {
            console.error(e);
        }
    }

    async startTournament() {
        const teams = this.state().teams;
        if (teams.length < 2 || teams.length % 2 !== 0) {
            throw new Error("Need an even number of teams to start.");
        }

        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/start`, {}));
            await this.refreshState();
        } catch (e) {
            console.error(e);
            throw new Error("Failed to start tournament on server.");
        }
    }

    async reportMatchResult(matchId: string, winnerId: string) {
        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/matches/${matchId}/result`, { winnerId }));
            await this.refreshState();
        } catch (e) {
            console.error(e);
            alert("Failed to report win.");
        }
    }

    async advanceRound() {
        const currentActiveMatches = this.activeMatches();
        if (currentActiveMatches.some(m => !m.winnerId)) {
            throw new Error("All matches must have a result before advancing.");
        }

        try {
            await firstValueFrom(this.http.post(`${this.apiUrl}/advance`, {}));
            await this.refreshState();
        } catch (e) {
            console.error(e);
            throw new Error("Failed to advance round.");
        }
    }
}
