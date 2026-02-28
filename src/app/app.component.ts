import { Component, inject, signal, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TournamentService } from './core/services/tournament.service';
import { AuthService } from './core/services/auth.service';
import { LobbyService } from './core/services/lobby.service';
import { TeamService } from './core/services/team.service';
import { UploadService } from './core/services/upload.service';

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
    uploadService = inject(UploadService);

    // Dialog States
    isCreateTournamentModalOpen = signal(false);
    newTournamentStartTime = signal('');
    newTournamentPicture = signal<File | null>(null);

    isCreateTeamModalOpen = signal(false);
    newGlobalTeamPicture = signal<File | null>(null);

    isProfileModalOpen = signal(false);
    profilePicture = signal<File | null>(null);

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
    currentUserPicture = this.authService.currentUserPicture;

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

    // Available Maps (Reduced to 7 for Pick/Ban where 6 are banned and 1 remains)
    cs2Maps = ['Mirage', 'Inferno', 'Nuke', 'Overpass', 'Vertigo', 'Ancient', 'Anubis'];

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
    openCreateTournamentModal() {
        this.newTournamentName.set('');
        this.newTournamentStartTime.set('');
        this.newTournamentPicture.set(null);
        this.isCreateTournamentModalOpen.set(true);
    }

    onTournamentPictureSelected(event: Event) {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (file) this.newTournamentPicture.set(file);
    }

    async submitCreateTournament() {
        if (this.newTournamentName().trim()) {
            let picUrl: string | undefined;
            if (this.newTournamentPicture()) {
                picUrl = await this.uploadService.uploadImage(this.newTournamentPicture()!);
            }
            await this.tournamentService.createTournament(this.newTournamentName(), this.newTournamentStartTime() || undefined, picUrl);
            this.isCreateTournamentModalOpen.set(false);
        }
    }

    async viewTournament(id: string) {
        await this.tournamentService.loadTournament(id);
    }

    closeTournament() {
        this.tournamentService.activeTournamentDetails.set(null);
    }

    // --- GLOBAL TEAMS DASHBOARD --- //
    openCreateTeamModal() {
        this.newGlobalTeamName.set('');
        this.newGlobalTeamPicture.set(null);
        this.isCreateTeamModalOpen.set(true);
    }

    onTeamPictureSelected(event: Event) {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (file) this.newGlobalTeamPicture.set(file);
    }

    async submitCreateGlobalTeam() {
        if (this.newGlobalTeamName().trim()) {
            let picUrl: string | undefined;
            if (this.newGlobalTeamPicture()) {
                picUrl = await this.uploadService.uploadImage(this.newGlobalTeamPicture()!);
            }
            await this.teamService.createTeam(this.newGlobalTeamName(), picUrl);
            this.isCreateTeamModalOpen.set(false);
        }
    }

    onProfilePictureSelected(event: Event) {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (file) this.profilePicture.set(file);
    }

    async submitUpdateProfile() {
        if (this.profilePicture()) {
            const picUrl = await this.uploadService.uploadImage(this.profilePicture()!);
            await this.authService.updateProfilePicture(picUrl);
            this.profilePicture.set(null);
            this.isProfileModalOpen.set(false);
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

    async banMap(mapName: string) {
        const lId = this.activeLobby()?.matchId;
        if (lId) {
            try {
                await this.lobbyService.banMap(lId, mapName);
            } catch (e: any) { alert(e.message); }
        }
    }

    isMapBanned(mapName: string): boolean {
        return this.activeLobby()?.bannedMaps?.includes(mapName) || false;
    }

    isMyTurnToBan(): boolean {
        const lobby = this.activeLobby();
        if (!lobby || lobby.selectedMap) return false;

        const isTeam1Turn = (lobby.bannedMaps?.length || 0) % 2 === 0;
        const activeTeamId = isTeam1Turn ? lobby.team1Id : lobby.team2Id;

        // Since only team captains can ban, we check if the logged in user owns the global team linked to this tournament team.
        // We find the active tournament team from the tournament state.
        const tournamentTeam = this.teams().find(t => t.id === activeTeamId);
        if (!tournamentTeam) return false;

        // Verify if it maps to one of my own global teams where I am the owner
        const isMyGlobalTeam = this.myTeams().some(gt => gt.id === tournamentTeam.globalTeamId && gt.ownerUsername === this.currentUser());
        return isMyGlobalTeam;
    }
}
