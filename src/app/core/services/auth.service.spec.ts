import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
    let service: AuthService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                AuthService,
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        service = TestBed.inject(AuthService);
        httpTestingController = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTestingController.verify();
        localStorage.clear();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should perform login and save token', async () => {
        const p = service.login('admin', 'password');

        const req = httpTestingController.expectOne('http://localhost:8080/api/auth/login');
        expect(req.request.method).toBe('POST');
        req.flush({ token: 'mock-jwt-token', userId: '1', username: 'admin' });

        const success = await p;
        expect(success).toBe(true);
        expect(service.isLoggedIn()).toBe(true);
        expect(service.currentUser()).toBe('admin');
        expect(localStorage.getItem('jwt_token')).toBe('mock-jwt-token');
    });

    it('should perform logout and clear token', () => {
        localStorage.setItem('jwt_token', 'mock');
        service.logout();
        expect(service.isLoggedIn()).toBe(false);
        expect(localStorage.getItem('jwt_token')).toBeNull();
    });
});
