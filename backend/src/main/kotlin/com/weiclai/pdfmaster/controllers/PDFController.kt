package com.weiclai.pdfmaster.controllers

import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream

@RestController
@RequestMapping("/api/pdf")
class PDFController {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload pdf file",
        responses = [
            ApiResponse(responseCode = "200", description = "Success"),
            ApiResponse(responseCode = "500", description = "Not a file")
        ]
    )
    fun uploadPdf(@RequestParam("file") file: MultipartFile) = "The file name ${file.originalFilename}, size ${file.size} uploaded successfully."

    @PostMapping("/combine", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload pdf files to combine",
        responses = [
            ApiResponse(responseCode = "200", description = "Success"),
            ApiResponse(responseCode = "500", description = "Not a file")
        ]
    )
    fun combinePdf(@RequestParam("files") files: List<MultipartFile>): ResponseEntity<ByteArrayResource> {
        val outputStream = ByteArrayOutputStream()
        val document = Document()
        val writer = PdfCopy(document, outputStream)
        document.open()
        for (file in files) {
            val pdfReader = PdfReader(file.inputStream)
            for (page in 0 until pdfReader.numberOfPages) {
                val importedPage = writer.getImportedPage(pdfReader, page + 1)
                writer.addPage(importedPage)
            }
        }
        document.close()
        val resource = ByteArrayResource(outputStream.toByteArray())
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=combined.pdf")
            .contentLength(resource.contentLength())
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }
}
