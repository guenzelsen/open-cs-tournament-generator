import { Injectable, signal, computed } from '@angular/core';
import { Team, Match, TournamentState } from '../models/tournament.model';

@Injectable({
    providedIn: 'root'
})
export class TournamentService {
    // State 
    private state = signal<TournamentState>({
        teams: [],
        matches: [],
        currentRound: 0,
        status: 'SETUP',
        maxRounds: 4 // Core requirement: Swiss 4 system
    });

    // Selectors
    readonly teams = computed(() => this.state().teams);
    readonly activeMatches = computed(() => this.state().matches.filter(m => m.round === this.state().currentRound));
    readonly allMatches = computed(() => this.state().matches);
    readonly currentRound = computed(() => this.state().currentRound);
    readonly maxRounds = computed(() => this.state().maxRounds);
    readonly status = computed(() => this.state().status);

    // Gets sorted standings based on wins > least losses
    readonly standings = computed(() => {
        return [...this.state().teams].sort((a, b) => {
            if (a.wins !== b.wins) return b.wins - a.wins;
            if (a.losses !== b.losses) return a.losses - b.losses;
            return 0;
        });
    });

    constructor() { }

    // 1. Setup Phase Methods
    addTeam(name: string) {
        if (this.state().status !== 'SETUP') return;
        const newTeam: Team = {
            id: crypto.randomUUID(),
            name,
            wins: 0,
            losses: 0,
            buchholzScore: 0
        };
        this.state.update(s => ({ ...s, teams: [...s.teams, newTeam] }));
    }

    removeTeam(id: string) {
        if (this.state().status !== 'SETUP') return;
        this.state.update(s => ({ ...s, teams: s.teams.filter(t => t.id !== id) }));
    }

    startTournament() {
        const teams = this.state().teams;
        if (teams.length < 2 || teams.length % 2 !== 0) {
            throw new Error("Need an even number of teams to start.");
        }

        this.state.update(s => ({ ...s, status: 'ACTIVE', currentRound: 1 }));
        this.generatePairings();
    }

    // 2. CS2 Private Match Code Gen
    generateMatchCode(): string {
        // 6 alphanumeric characters
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = '';
        for (let i = 0; i < 6; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return result;
    }

    // 3. Swiss Logic & Round progression
    reportMatchResult(matchId: string, winnerId: string) {
        this.state.update(s => {
            const matchIndex = s.matches.findIndex(m => m.id === matchId);
            if (matchIndex === -1) return s;

            const match = s.matches[matchIndex];
            if (match.winnerId) return s; // already reported

            const updatedMatch = { ...match, winnerId };
            const newMatches = [...s.matches];
            newMatches[matchIndex] = updatedMatch;

            // Update team records
            const loserId = winnerId === match.team1Id ? match.team2Id : match.team1Id;
            const newTeams = s.teams.map(t => {
                if (t.id === winnerId) return { ...t, wins: t.wins + 1 };
                if (t.id === loserId) return { ...t, losses: t.losses + 1 };
                return t;
            });

            return { ...s, matches: newMatches, teams: newTeams };
        });
    }

    advanceRound() {
        const currentActiveMatches = this.activeMatches();
        if (currentActiveMatches.some(m => !m.winnerId)) {
            throw new Error("All matches must have a result before advancing.");
        }

        this.state.update(s => {
            if (s.currentRound >= s.maxRounds) {
                return { ...s, status: 'FINISHED' };
            }
            return { ...s, currentRound: s.currentRound + 1 };
        });

        if (this.state().status === 'ACTIVE') {
            this.generatePairings();
        }
    }

    private generatePairings() {
        this.state.update(s => {
            const teamsToPair = [...this.standings()];
            const newMatches: Match[] = [];
            const pastMatches = s.matches;

            // Attempt to pair based on similar scores.
            // In a real sophisticated Swiss system, we'd avoid rematches using a graph algorithm (e.g. Blossom).
            // Here we use a simpler greedy approach grouped by score.

            // Note: we just iterate and pair adjacent teams. If they played before, we try to swap with the next one.
            while (teamsToPair.length >= 2) {
                const team1 = teamsToPair.shift()!;

                let opponentIndex = 0;
                for (let i = 0; i < teamsToPair.length; i++) {
                    const team2 = teamsToPair[i];
                    const hasPlayed = pastMatches.some(m =>
                        (m.team1Id === team1.id && m.team2Id === team2.id) ||
                        (m.team1Id === team2.id && m.team2Id === team1.id)
                    );

                    if (!hasPlayed) {
                        opponentIndex = i;
                        break;
                    }
                }

                const team2 = teamsToPair.splice(opponentIndex, 1)[0];

                newMatches.push({
                    id: crypto.randomUUID(),
                    team1Id: team1.id,
                    team2Id: team2.id,
                    round: s.currentRound,
                    privateMatchCode: this.generateMatchCode()
                });
            }

            return { ...s, matches: [...s.matches, ...newMatches] };
        });
    }
}
