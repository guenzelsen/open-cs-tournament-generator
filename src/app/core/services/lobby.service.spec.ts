import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LobbyService } from './lobby.service';

describe('LobbyService', () => {
    let service: LobbyService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                LobbyService,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(LobbyService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should load lobby data', async () => {
        const p = service.loadLobby('match-1');

        const req = httpTestingController.expectOne('http://localhost:8080/api/lobbies/match-1');
        expect(req.request.method).toBe('GET');
        req.flush({ matchId: 'match-1', team1Id: 't1', team2Id: 't2', bannedMaps: ['Mirage'], selectedMap: null, lastBanTime: null });

        await p;
        expect(service.activeLobby()?.bannedMaps).toContain('Mirage');
    });

    it('should submit map ban', async () => {
        const p = service.banMap('match-1', 'Dust II');

        const req = httpTestingController.expectOne('http://localhost:8080/api/lobbies/match-1/ban');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ mapName: 'Dust II' });
        req.flush({ matchId: 'match-1', team1Id: 't1', team2Id: 't2', bannedMaps: ['Dust II'], selectedMap: null, lastBanTime: null });

        await p;
        expect(service.activeLobby()?.bannedMaps).toContain('Dust II');
    });
});
