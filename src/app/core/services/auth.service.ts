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

    private state = signal<{ token: string | null, username: string | null, userId: string | null }>({
        token: localStorage.getItem('jwt_token'),
        username: localStorage.getItem('username'),
        userId: localStorage.getItem('user_id')
    });

    readonly isLoggedIn = computed(() => !!this.state().token);
    readonly currentUser = computed(() => this.state().username);
    readonly currentUserId = computed(() => this.state().userId);

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

    logout() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('username');
        localStorage.removeItem('user_id');
        this.state.set({ token: null, username: null, userId: null });
    }

    private handleAuthResponse(res: AuthResponse) {
        localStorage.setItem('jwt_token', res.token);
        localStorage.setItem('username', res.username);
        localStorage.setItem('user_id', res.userId);
        this.state.set({ token: res.token, username: res.username, userId: res.userId });
    }
}
