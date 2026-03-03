import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class UploadService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/api/upload`;

    async uploadImage(file: File): Promise<string> {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const res = await firstValueFrom(this.http.post<{ url: string }>(this.apiUrl, formData));
            return `${environment.apiUrl}${res.url}`;
        } catch (e) {
            console.error('File upload failed', e);
            throw e;
        }
    }
}
