import { Component, inject, signal, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TournamentService } from './core/services/tournament.service';
import { AuthService } from './core/services/auth.service';
import { LobbyService } from './core/services/lobby.service';
import { TeamService } from './core/services/team.service';

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
    teamService = inject(TeamService);

    // View States
    activeTab = signal<'TOURNAMENTS' | 'TEAMS'>('TOURNAMENTS');
    isLoginMode = signal(true);
    authUsername = signal('');
    authPassword = signal('');
    loginError = signal('');

    newTournamentName = signal('');

    // Team States
    newGlobalTeamName = signal('');
    newPlayerUsername = signal('');
    teamSearchTerm = signal('');

    // Service Signals Exposed
    isLoggedIn = this.authService.isLoggedIn;
    currentUser = this.authService.currentUser;

    tournaments = this.tournamentService.allTournaments;

    // State Filter for Tournaments
    stateFilter = signal<'ALL' | 'SETUP' | 'ACTIVE' | 'FINISHED'>('ALL');

    filteredTournaments = computed(() => {
        const all = this.tournaments();
        const filter = this.stateFilter();
        if (filter === 'ALL') return all;
        return all.filter(t => t.status === filter);
    });

    activeTournament = this.tournamentService.activeTournament;

    status = this.tournamentService.status;
    teams = this.tournamentService.teams;
    standings = this.tournamentService.standings;
    currentRound = this.tournamentService.currentRound;
    activeMatches = this.tournamentService.activeMatches;
    maxRounds = this.tournamentService.maxRounds;

    // Global Teams
    myTeams = this.teamService.myTeams;
    searchedTeams = this.teamService.searchedTeams;

    activeLobby = this.lobbyService.activeLobby;
    selectedMap = this.lobbyService.selectedMap;

    // Available Maps
    cs2Maps = ['Mirage', 'Inferno', 'Nuke', 'Overpass', 'Vertigo', 'Ancient', 'Anubis', 'Dust II'];

    constructor() {
        const params = new URLSearchParams(window.location.search);
        const token = params.get('token');
        if (token) {
            this.authService.loginWithToken(token);
        }

        effect(() => {
            if (this.isLoggedIn()) {
                this.tournamentService.loadTournaments();
                this.teamService.loadMyTeams();
            }
        });
    }

    loginWithSteam() {
        window.location.href = 'http://localhost:8080/api/auth/steam';
    }

    setTab(tab: 'TOURNAMENTS' | 'TEAMS') {
        this.activeTab.set(tab);
        this.tournamentService.activeTournamentDetails.set(null);
    }

    // --- AUTH --- //
    async login() {
        this.loginError.set('');
        if (!this.authUsername() || !this.authPassword()) {
            this.loginError.set('Please enter username and password');
            return;
        }
        const success = await this.authService.login(this.authUsername(), this.authPassword());
        if (!success) {
            this.loginError.set('Invalid credentials');
        }
    }

    async register() {
        this.loginError.set('');
        const success = await this.authService.register(this.authUsername(), this.authPassword());
        if (!success) {
            this.loginError.set('Registration failed');
        }
    }

    logout() {
        this.authService.logout();
        this.tournamentService.clearData();
        this.teamService.myTeams.set([]);
        this.teamService.searchedTeams.set([]);
        this.lobbyService.clearLobby();

        // Clear forms
        this.authUsername.set('');
        this.authPassword.set('');
        this.newTournamentName.set('');
        this.newGlobalTeamName.set('');
        this.newPlayerUsername.set('');
        this.teamSearchTerm.set('');
        this.loginError.set('');
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

    // --- GLOBAL TEAMS DASHBOARD --- //
    async createGlobalTeam() {
        if (this.newGlobalTeamName().trim()) {
            await this.teamService.createTeam(this.newGlobalTeamName());
            this.newGlobalTeamName.set('');
        }
    }

    async addPlayer(teamId: string) {
        if (this.newPlayerUsername().trim()) {
            await this.teamService.addPlayer(teamId, this.newPlayerUsername());
            this.newPlayerUsername.set('');
        }
    }

    async removePlayer(teamId: string, username: string) {
        await this.teamService.removePlayer(teamId, username);
    }

    // --- TOURNAMENT --- //
    async searchTeamsForTournament() {
        if (this.teamSearchTerm().trim()) {
            await this.teamService.searchTeams(this.teamSearchTerm());
        }
    }

    async addTeam(globalTeamId: string) {
        const tId = this.activeTournament()?.id;
        if (tId && globalTeamId) {
            try {
                await this.tournamentService.addTeam(tId, globalTeamId);
                this.teamSearchTerm.set('');
                this.teamService.searchedTeams.set([]);
            } catch (e: any) {
                alert(e.message);
            }
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
