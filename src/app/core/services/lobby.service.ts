import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface LobbyResponse {
    matchId: string;
    team1Id: string;
    team2Id: string;
    mapVotes: { [key: string]: number };
    selectedMap: string | null;
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

    async loadLobby(matchId: string) {
        try {
            const data = await firstValueFrom(this.http.get<LobbyResponse>(`${this.apiUrl}/${matchId}`));
            this.activeLobbyState.set(data);
        } catch (e) {
            console.error('Failed to load lobby', e);
        }
    }

    async voteMap(matchId: string, mapName: string) {
        try {
            const data = await firstValueFrom(this.http.post<LobbyResponse>(`${this.apiUrl}/${matchId}/vote`, { mapName }));
            this.activeLobbyState.set(data);
        } catch (e) {
            console.error('Failed to vote', e);
            throw new Error("Failed to cast vote.");
        }
    }

    clearLobby() {
        this.activeLobbyState.set(null);
    }
}
