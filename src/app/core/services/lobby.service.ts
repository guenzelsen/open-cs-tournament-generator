import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface LobbyResponse {
    matchId: string;
    team1Id: string;
    team2Id: string;
    bannedMaps: string[];
    selectedMap: string | null;
    lastBanTime: string | null;
}

@Injectable({
    providedIn: 'root'
})
export class LobbyService {
    private http = inject(HttpClient);
    private apiUrl = 'http://localhost:8080/api/lobbies';

    private activeLobbyState = signal<LobbyResponse | null>(null);
    readonly activeLobby = computed(() => this.activeLobbyState());
    readonly selectedMap = computed(() => this.activeLobbyState()?.selectedMap || null);
    readonly lastBanTime = computed(() => this.activeLobbyState()?.lastBanTime || null);

    async loadLobby(matchId: string) {
        try {
            const data = await firstValueFrom(this.http.get<LobbyResponse>(`${this.apiUrl}/${matchId}`));
            this.activeLobbyState.set(data);
        } catch (e) {
            console.error('Failed to load lobby', e);
        }
    }

    async banMap(matchId: string, mapName: string) {
        try {
            const data = await firstValueFrom(this.http.post<LobbyResponse>(`${this.apiUrl}/${matchId}/ban`, { mapName }));
            this.activeLobbyState.set(data);
        } catch (e: any) {
            console.error('Failed to ban map', e);
            if (e.status === 403) throw new Error("Not your turn to ban.");
            throw new Error("Failed to ban map.");
        }
    }

    async autoBan(matchId: string) {
        try {
            const data = await firstValueFrom(this.http.post<LobbyResponse>(`${this.apiUrl}/${matchId}/auto-ban`, {}));
            this.activeLobbyState.set(data);
        } catch (e: any) {
            console.error('Failed to auto-ban map', e);
        }
    }

    async restartLobby(matchId: string) {
        try {
            const data = await firstValueFrom(this.http.post<LobbyResponse>(`${this.apiUrl}/${matchId}/restart`, {}));
            this.activeLobbyState.set(data);
        } catch (e: any) {
            console.error('Failed to restart lobby', e);
            throw new Error("Failed to restart lobby. Are you an admin?");
        }
    }

    clearLobby() {
        this.activeLobbyState.set(null);
    }
}
