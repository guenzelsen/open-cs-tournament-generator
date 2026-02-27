import { Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TournamentService } from './core/services/tournament.service';
import { AuthService } from './core/services/auth.service';
import { LobbyService } from './core/services/lobby.service';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './app.component.html'
})
export class AppComponent {
    authService = inject(AuthService);
    tournamentService = inject(TournamentService);
    lobbyService = inject(LobbyService);

    // View States
    isLoginMode = signal(true);
    authUsername = signal('');
    authPassword = signal('');

    newTournamentName = signal('');
    newTeamName = signal('');

    // Service Signals Exposed
    isLoggedIn = this.authService.isLoggedIn;
    currentUser = this.authService.currentUser;

    tournaments = this.tournamentService.allTournaments;
    activeTournament = this.tournamentService.activeTournament;

    status = this.tournamentService.status;
    teams = this.tournamentService.teams;
    standings = this.tournamentService.standings;
    currentRound = this.tournamentService.currentRound;
    activeMatches = this.tournamentService.activeMatches;
    maxRounds = this.tournamentService.maxRounds;

    activeLobby = this.lobbyService.activeLobby;
    selectedMap = this.lobbyService.selectedMap;

    // Available Maps
    cs2Maps = ['Mirage', 'Inferno', 'Nuke', 'Overpass', 'Vertigo', 'Ancient', 'Anubis', 'Dust II'];

    constructor() {
        effect(() => {
            if (this.isLoggedIn()) {
                this.tournamentService.loadTournaments();
            }
        });
    }

    // --- AUTH --- //
    async login() {
        await this.authService.login(this.authUsername(), this.authPassword());
    }

    async register() {
        await this.authService.register(this.authUsername(), this.authPassword());
    }

    logout() {
        this.authService.logout();
        this.tournamentService.activeTournamentDetails.set(null);
        this.lobbyService.clearLobby();
    }

    // --- DASHBOARD --- //
    async createTournament() {
        if (this.newTournamentName().trim()) {
            await this.tournamentService.createTournament(this.newTournamentName());
            this.newTournamentName.set('');
        }
    }

    async viewTournament(id: string) {
        await this.tournamentService.loadTournament(id);
    }

    closeTournament() {
        this.tournamentService.activeTournamentDetails.set(null);
    }

    // --- TOURNAMENT --- //
    async addTeam() {
        const tId = this.activeTournament()?.id;
        if (tId && this.newTeamName().trim()) {
            await this.tournamentService.addTeam(tId, this.newTeamName().trim());
            this.newTeamName.set('');
        }
    }

    async removeTeam(teamId: string) {
        const tId = this.activeTournament()?.id;
        if (tId) await this.tournamentService.removeTeam(tId, teamId);
    }

    async startTournament() {
        const tId = this.activeTournament()?.id;
        if (tId) {
            try {
                await this.tournamentService.startTournament(tId);
            } catch (e: any) { alert(e.message); }
        }
    }

    async reportWin(matchId: string, teamId: string) {
        try {
            await this.tournamentService.reportMatchResult(matchId, teamId);
        } catch (e: any) { alert(e.message); }
    }

    async advanceRound() {
        const tId = this.activeTournament()?.id;
        if (tId) {
            try {
                await this.tournamentService.advanceRound(tId);
            } catch (e: any) { alert(e.message); }
        }
    }

    getTeamName(id: string): string {
        return this.teams().find(t => t.id === id)?.name || 'Unknown';
    }

    // --- LOBBY --- //
    async openLobby(matchId: string) {
        await this.lobbyService.loadLobby(matchId);
    }

    closeLobby() {
        this.lobbyService.clearLobby();
    }

    async voteForMap(mapName: string) {
        const lId = this.activeLobby()?.matchId;
        if (lId) {
            await this.lobbyService.voteMap(lId, mapName);
        }
    }

    getMapVotes(mapName: string): number {
        return this.activeLobby()?.mapVotes[mapName] || 0;
    }
}
