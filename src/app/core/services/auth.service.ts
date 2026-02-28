import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface AuthResponse {
    token: string;
    userId: string;
    username: string;
}

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
        const cloned = req.clone({
            headers: req.headers.set('Authorization', `Bearer ${token}`)
        });
        return next(cloned);
    }
    return next(req);
};

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private apiUrl = 'http://localhost:8080/api/auth';

    private state = signal<{ token: string | null, username: string | null, userId: string | null, pictureUrl: string | null }>({
        token: localStorage.getItem('jwt_token'),
        username: localStorage.getItem('username'),
        userId: localStorage.getItem('user_id'),
        pictureUrl: localStorage.getItem('picture_url')
    });

    readonly isLoggedIn = computed(() => !!this.state().token);
    readonly currentUser = computed(() => this.state().username);
    readonly currentUserId = computed(() => this.state().userId);
    readonly currentUserPicture = computed(() => this.state().pictureUrl);

    async fetchUserProfile() {
        if (!this.isLoggedIn()) return;
        try {
            const res = await firstValueFrom(this.http.get<{ username: string, pictureUrl: string | null }>('http://localhost:8080/api/users/me'));
            localStorage.setItem('picture_url', res.pictureUrl || '');
            this.state.update(s => ({ ...s, pictureUrl: res.pictureUrl }));
        } catch (e) {
            console.error('Failed to fetch profile', e);
        }
    }

    async updateProfilePicture(pictureUrl: string) {
        try {
            const res = await firstValueFrom(this.http.put<{ username: string, pictureUrl: string | null }>('http://localhost:8080/api/users/me', { pictureUrl }));
            localStorage.setItem('picture_url', res.pictureUrl || '');
            this.state.update(s => ({ ...s, pictureUrl: res.pictureUrl }));
        } catch (e) {
            console.error('Failed to update profile', e);
        }
    }

    async register(username: string, password: string): Promise<boolean> {
        try {
            const res = await firstValueFrom(this.http.post<AuthResponse>(`${this.apiUrl}/register`, { username, password }));
            this.handleAuthResponse(res);
            return true;
        } catch (e) {
            console.error('Registration failed', e);
            return false;
        }
    }

    async login(username: string, password: string): Promise<boolean> {
        try {
            const res = await firstValueFrom(this.http.post<AuthResponse>(`${this.apiUrl}/login`, { username, password }));
            this.handleAuthResponse(res);
            return true;
        } catch (e) {
            console.error('Login failed', e);
            return false;
        }
    }

    loginWithToken(token: string): boolean {
        try {
            // Minimal JWT parser for the subject (username) and userId
            const payload = JSON.parse(atob(token.split('.')[1]));
            this.handleAuthResponse({ token, username: payload.sub, userId: payload.userId });

            // Clean URL from the token param
            window.history.replaceState({}, document.title, window.location.pathname);
            return true;
        } catch (e) {
            console.error('Failed to parse token', e);
            return false;
        }
    }

    logout() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('username');
        localStorage.removeItem('user_id');
        localStorage.removeItem('picture_url');
        this.state.set({ token: null, username: null, userId: null, pictureUrl: null });
    }

    private handleAuthResponse(res: AuthResponse) {
        localStorage.setItem('jwt_token', res.token);
        localStorage.setItem('username', res.username);
        localStorage.setItem('user_id', res.userId);
        this.state.update(s => ({ ...s, token: res.token, username: res.username, userId: res.userId }));
        this.fetchUserProfile();
    }
}
