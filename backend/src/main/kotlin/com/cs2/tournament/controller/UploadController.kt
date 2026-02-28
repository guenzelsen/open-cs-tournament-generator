package com.cs2.tournament.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@RestController
@RequestMapping("/api/upload")
class UploadController {

    private val uploadDir: Path = Paths.get("uploads")

    init {
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir)
        }
    }

    @PostMapping
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, String>> {
        if (file.isEmpty) {
            return ResponseEntity.badRequest().body(mapOf("error" to "File is empty"))
        }

        val originalFilename = file.originalFilename ?: "file"
        val extension = originalFilename.substringAfterLast(".", "")
        val newFilename = "${UUID.randomUUID()}.${extension}"
        val targetPath = uploadDir.resolve(newFilename)

        Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

        val fileUrl = "/uploads/$newFilename"
        return ResponseEntity.ok(mapOf("url" to fileUrl))
    }
}
