import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TournamentService } from './core/services/tournament.service';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './app.component.html',
    styleUrl: './app.component.css'
})
export class AppComponent {
    tournamentService = inject(TournamentService);

    newTeamName = signal('');

    // Expose signals to template
    status = this.tournamentService.status;
    teams = this.tournamentService.teams;
    standings = this.tournamentService.standings;
    currentRound = this.tournamentService.currentRound;
    activeMatches = this.tournamentService.activeMatches;
    allMatches = this.tournamentService.allMatches;
    maxRounds = this.tournamentService.maxRounds;

    addTeam() {
        const name = this.newTeamName().trim();
        if (name) {
            this.tournamentService.addTeam(name);
            this.newTeamName.set('');
        }
    }

    removeTeam(id: string) {
        this.tournamentService.removeTeam(id);
    }

    startTournament() {
        try {
            this.tournamentService.startTournament();
        } catch (e: any) {
            alert(e.message);
        }
    }

    reportWin(matchId: string, teamId: string) {
        this.tournamentService.reportMatchResult(matchId, teamId);

        // Check if we should advance round automatically if all matches are done
        // Or we provide a button to advance manually. Let's do it manually via a button.
    }

    advanceRound() {
        try {
            this.tournamentService.advanceRound();
        } catch (e: any) {
            alert(e.message);
        }
    }

    getTeamName(id: string): string {
        return this.teams().find(t => t.id === id)?.name || 'Unknown';
    }
}
