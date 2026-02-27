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
        req.flush({ matchId: 'match-1', team1Id: 't1', team2Id: 't2', mapVotes: { 'Mirage': 1 }, selectedMap: null });

        await p;
        expect(service.activeLobby()?.mapVotes['Mirage']).toBe(1);
    });

    it('should submit map vote', async () => {
        const p = service.voteMap('match-1', 'Dust II');

        const req = httpTestingController.expectOne('http://localhost:8080/api/lobbies/match-1/vote');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({ mapName: 'Dust II' });
        req.flush({ matchId: 'match-1', team1Id: 't1', team2Id: 't2', mapVotes: { 'Dust II': 2 }, selectedMap: 'Dust II' });

        await p;
        expect(service.selectedMap()).toBe('Dust II');
    });
});
