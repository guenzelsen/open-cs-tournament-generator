import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TournamentService } from './core/services/tournament.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
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
