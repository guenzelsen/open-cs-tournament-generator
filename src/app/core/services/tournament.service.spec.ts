import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TournamentService, TournamentListResponse } from './tournament.service';
import { vi } from 'vitest';

describe('TournamentService', () => {
    let service: TournamentService;
    let httpTestingController: HttpTestingController;

    const mockResponse: TournamentListResponse = {
        id: '1', name: 'Test', organizerName: 'user1', currentRound: 0, status: 'SETUP', maxRounds: 4, teams: [], matches: [], adminUsernames: []
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                TournamentService,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(TournamentService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should POST to create a tournament', async () => {
        vi.spyOn(service, 'loadTournaments').mockResolvedValue(undefined as any);

        const p = service.createTournament('Major 2026');

        const req = httpTestingController.expectOne('http://localhost:8080/api/tournaments');
        expect(req.request.method).toBe('POST');
        req.flush(mockResponse);

        await p;
        expect(service.loadTournaments).toHaveBeenCalled();
    });

    it('should POST to add a team to active tournament', async () => {
        vi.spyOn(service, 'loadTournament').mockResolvedValue(undefined as any);

        const p = service.addTeam('1', 'Navi');

        const req = httpTestingController.expectOne('http://localhost:8080/api/tournaments/1/teams');
        expect(req.request.method).toBe('POST');
        req.flush({ id: '2', name: 'Navi' });

        await p;
        expect(service.loadTournament).toHaveBeenCalledWith('1');
    });
});
