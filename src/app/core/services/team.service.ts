import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GlobalTeam {
    id: string;
    name: string;
    ownerUsername: string;
    playerUsernames: string[];
    pictureUrl?: string;
}

@Injectable({
    providedIn: 'root'
})
export class TeamService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/api/teams`;

    myTeams = signal<GlobalTeam[]>([]);
    searchedTeams = signal<GlobalTeam[]>([]);

    async loadMyTeams() {
        try {
            const data = await firstValueFrom(this.http.get<GlobalTeam[]>(`${this.apiUrl}/my`));
            this.myTeams.set(data);
        } catch (e) {
            console.error('Failed to load my teams', e);
        }
    }

    async searchTeams(name: string) {
        try {
            const data = await firstValueFrom(this.http.get<GlobalTeam[]>(`${this.apiUrl}/search?name=${name}`));
            this.searchedTeams.set(data);
        } catch (e) {
            console.error('Failed to search teams', e);
        }
    }

    async createTeam(name: string, pictureUrl?: string) {
        try {
            await firstValueFrom(this.http.post<GlobalTeam>(this.apiUrl, { name, pictureUrl }));
            await this.loadMyTeams();
        } catch (e: any) {
            console.error('Failed to create team', e);
            throw e;
        }
    }

    async addPlayer(teamId: string, username: string) {
        try {
            await firstValueFrom(this.http.post<GlobalTeam>(`${this.apiUrl}/${teamId}/players`, { username }));
            await this.loadMyTeams();
        } catch (e: any) {
            console.error('Failed to add player', e);
            throw e;
        }
    }

    async removePlayer(teamId: string, username: string) {
        try {
            await firstValueFrom(this.http.delete<GlobalTeam>(`${this.apiUrl}/${teamId}/players/${username}`));
            await this.loadMyTeams();
        } catch (e: any) {
            console.error('Failed to remove player', e);
            throw e;
        }
    }
}
