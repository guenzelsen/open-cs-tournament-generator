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
    cs2Maps = ['de_ancient', 'de_dust2', 'de_inferno', 'de_mirage', 'de_nuke', 'de_overpass', 'de_anubis'];

    // Map Vote Timer
    banTimer = signal<number>(0);
    private timerInterval: any;

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

        // Effect to manage the lobby timer
        effect((onCleanup) => {
            const lobbyTimeStr = this.lobbyService.lastBanTime();
            const lobbyMatchId = this.activeLobby()?.matchId;
            const selectedMap = this.selectedMap();

            clearInterval(this.timerInterval);

            if (lobbyTimeStr && !selectedMap && lobbyMatchId) {
                const startTime = new Date(lobbyTimeStr).getTime();
                this.timerInterval = setInterval(async () => {
                    const now = new Date().getTime();
                    const diffSeconds = Math.floor((now - startTime) / 1000);
                    const remaining = Math.max(0, 60 - diffSeconds);
                    this.banTimer.set(remaining);

                    if (remaining === 0) {
                        // Backend handles the actual ban, but we poll to get the update
                        if (diffSeconds > 60 && diffSeconds % 3 === 0) {
                            await this.lobbyService.loadLobby(lobbyMatchId);
                        }
                    }
                }, 1000);
            } else {
                this.banTimer.set(0);
            }

            onCleanup(() => {
                clearInterval(this.timerInterval);
            });
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

    async proposeScore(matchId: string, teamId: string) {
        try {
            const team = this.activeTournament()?.teams.find(t => t.id === teamId);
            const score = prompt(`Enter score for this match (e.g. 13-10). You are reporting ${team?.name} as the WINNER:`);
            if (score) {
                await this.tournamentService.proposeMatchResult(matchId, teamId, score);
            }
        } catch (e: any) { alert(e.message); }
    }

    async confirmWin(matchId: string) {
        try {
            await this.tournamentService.confirmMatchResult(matchId);
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

    hasUnfinishedMatches(): boolean {
        return this.activeMatches().some(m => !m.winnerId);
    }

    getTeamName(id: string): string {
        return this.teams().find(t => t.id === id)?.name || 'Unknown';
    }

    // --- ADMIN LOGIC --- //
    isOrganizerOrAdmin(): boolean {
        const t = this.activeTournament();
        if (!t) return false;
        const username = this.currentUser();
        return t.organizerName === username || (t.adminUsernames?.includes(username || '') ?? false);
    }

    async addAdmin(username: string) {
        const tId = this.activeTournament()?.id;
        if (tId && username.trim()) {
            try {
                await this.tournamentService.addAdmin(tId, username.trim());
            } catch (e: any) { alert(e.message); }
        }
    }

    async removeAdmin(username: string) {
        const tId = this.activeTournament()?.id;
        if (tId) {
            try {
                await this.tournamentService.removeAdmin(tId, username);
            } catch (e: any) { alert(e.message); }
        }
    }

    async forceWin(matchId: string, teamId: string) {
        if (!confirm("Are you sure you want to force this win?")) return;
        try {
            await this.tournamentService.reportMatchResult(matchId, teamId);
        } catch (e: any) { alert(e.message); }
    }

    async restartVote() {
        const matchId = this.activeLobby()?.matchId;
        if (!confirm("Are you sure you want to restart the map vote?")) return;
        if (matchId) {
            try {
                await this.lobbyService.restartLobby(matchId);
            } catch (e: any) { alert(e.message); }
        }
    }

    getTeamIdForCurrentUser(): string | null {
        const tournament = this.activeTournament();
        if (!tournament) return null;

        // Find which global team the current user owns
        const userGlobalTeamsIds = this.myTeams()
            .filter(t => t.ownerUsername === this.currentUser())
            .map(t => t.id);

        if (userGlobalTeamsIds.length === 0) return null;

        // Find the matching tournament team ID
        const tt = tournament.teams.find(t => userGlobalTeamsIds.includes(t.globalTeamId));
        return tt ? tt.id : null;
    }

    // --- LOBBY --- //
    async openLobby(matchId: string) {
        await this.lobbyService.loadLobby(matchId);
    }

    refreshLobbyInterval: any;

    closeLobby() {
        this.lobbyService.clearLobby();
        clearInterval(this.refreshLobbyInterval);
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
