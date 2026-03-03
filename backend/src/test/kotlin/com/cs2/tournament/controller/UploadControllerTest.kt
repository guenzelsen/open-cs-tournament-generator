package com.cs2.tournament.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

class UploadControllerTest {

    private val controller = UploadController()

    @Test
    fun `should reject file with disallowed extension`() {
        val file = MockMultipartFile("file", "malware.exe", "application/octet-stream", "binary content".toByteArray())
        val response = controller.uploadFile(file)
        assertEquals(400, response.statusCode.value())
        assertTrue(response.body!!["error"]!!.contains("File type not allowed"))
    }

    @Test
    fun `should reject file with wrong content type`() {
        val file = MockMultipartFile("file", "image.png", "application/octet-stream", "fake image".toByteArray())
        val response = controller.uploadFile(file)
        assertEquals(400, response.statusCode.value())
        assertTrue(response.body!!["error"]!!.contains("Invalid content type"))
    }

    @Test
    fun `should accept valid image file`() {
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "jpeg content".toByteArray())
        val response = controller.uploadFile(file)
        assertEquals(200, response.statusCode.value())
        assertTrue(response.body!!["url"]!!.endsWith(".jpg"))
    }

    @Test
    fun `should reject empty file`() {
        val file = MockMultipartFile("file", "empty.jpg", "image/jpeg", ByteArray(0))
        val response = controller.uploadFile(file)
        assertEquals(400, response.statusCode.value())
    }
}
