import { TestBed } from '@angular/core/testing';
import { TournamentService } from './tournament.service';

describe('TournamentService', () => {
    let service: TournamentService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(TournamentService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('Private Match Code Generator', () => {
        it('should generate a 6-character code', () => {
            const code = service.generateMatchCode();
            expect(code.length).toBe(6);
            expect(/^[A-Z0-9]{6}$/.test(code)).toBe(true);
        });
    });

    describe('Tournament Logic Setup', () => {
        it('should add teams initially', () => {
            service.addTeam('Navi');
            service.addTeam('Vitality');
            expect(service.teams().length).toBe(2);
            expect(service.teams()[0].name).toBe('Navi');
        });

        it('should not allow starting with odd number of teams', () => {
            service.addTeam('Navi');
            expect(() => service.startTournament()).toThrowError("Need an even number of teams to start.");
        });
    });

    describe('Swiss Pairing Logic', () => {
        beforeEach(() => {
            service.addTeam('Team A');
            service.addTeam('Team B');
            service.addTeam('Team C');
            service.addTeam('Team D');
        });

        it('should generate initial pairings correctly on start', () => {
            service.startTournament();
            expect(service.status()).toBe('ACTIVE');
            expect(service.currentRound()).toBe(1);
            expect(service.activeMatches().length).toBe(2);

            const match1 = service.activeMatches()[0];
            expect(match1.privateMatchCode.length).toBe(6);
            expect(match1.round).toBe(1);
        });

        it('should advance round when all matches have winners', () => {
            service.startTournament();
            const matches = service.activeMatches();

            // Match 1: Team 1 wins
            service.reportMatchResult(matches[0].id, matches[0].team1Id);
            // Match 2: Team 1 wins
            service.reportMatchResult(matches[1].id, matches[1].team2Id);

            // Advance Round
            service.advanceRound();

            expect(service.currentRound()).toBe(2);
            expect(service.activeMatches().length).toBe(2);
            expect(service.activeMatches()[0].round).toBe(2);
        });

        it('should finish tournament when max rounds reached', () => {
            service.startTournament();

            for (let round = 1; round <= 4; round++) {
                const matches = service.activeMatches();
                matches.forEach(m => service.reportMatchResult(m.id, m.team1Id));
                service.advanceRound();
            }

            expect(service.status()).toBe('FINISHED');
        });
    });
});
