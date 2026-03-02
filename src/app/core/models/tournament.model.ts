export interface Team {
    id: string;
    globalTeamId: string;
    name: string;
    isComplete: boolean;
    wins: number;
    losses: number;
    buchholzScore: number; // for tiebreakers if needed
    pictureUrl?: string;
}

export interface Match {
    id: string;
    team1Id: string;
    team2Id: string;
    team1Score?: number;
    team2Score?: number;
    winnerId?: string;
    reportedWinnerId?: string;
    reportedScore?: string;
    privateMatchCode: string; // 6 character code
    round: number;
}

export interface TournamentState {
    id: string;
    name: string;
    organizerName: string;
    teams: Team[];
    matches: Match[];
    currentRound: number;
    status: 'SETUP' | 'ACTIVE' | 'FINISHED';
    maxRounds: number;
    startTime?: string;
    pictureUrl?: string;
    adminUsernames: string[];
}
