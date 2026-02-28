import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class UploadService {
    private http = inject(HttpClient);
    private apiUrl = 'http://localhost:8080/api/upload';

    async uploadImage(file: File): Promise<string> {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const res = await firstValueFrom(this.http.post<{ url: string }>(this.apiUrl, formData));
            return 'http://localhost:8080' + res.url;
        } catch (e) {
            console.error('File upload failed', e);
            throw e;
        }
    }
}
