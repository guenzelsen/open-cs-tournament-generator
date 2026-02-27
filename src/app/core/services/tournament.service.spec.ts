import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TournamentService } from './tournament.service';

describe('TournamentService', () => {
    let service: TournamentService;
    let httpTestingController: HttpTestingController;

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

        // Ignore the initial refreshState() call
        const req = httpTestingController.expectOne('http://localhost:8080/api/tournament/state');
        req.flush({ teams: [], matches: [], currentRound: 0, status: 'SETUP', maxRounds: 4 });
    });

    afterEach(() => {
        httpTestingController.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should call POST /teams when adding a team', async () => {
        const p = service.addTeam('Navi');

        const req = httpTestingController.expectOne('http://localhost:8080/api/tournament/teams');
        expect(req.request.method).toBe('POST');
        req.flush({ id: '1', name: 'Navi', wins: 0, losses: 0, buchholzScore: 0 });

        // Allow promise resolution to flush to event loop
        await new Promise(r => setTimeout(r, 0));

        const refreshReq = httpTestingController.expectOne('http://localhost:8080/api/tournament/state');
        expect(refreshReq.request.method).toBe('GET');
        refreshReq.flush({ teams: [{ id: '1', name: 'Navi', wins: 0, losses: 0, buchholzScore: 0 }], matches: [], currentRound: 0, status: 'SETUP', maxRounds: 4 });

        await p;
    });

});
