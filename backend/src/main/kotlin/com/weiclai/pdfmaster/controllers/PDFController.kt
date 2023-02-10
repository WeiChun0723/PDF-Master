package com.weiclai.pdfmaster.controllers

import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element.ALIGN_CENTER
import com.itextpdf.text.Font
import com.itextpdf.text.Phrase
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfGState
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
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
    fun uploadPdf(@RequestParam(value = "files", required = true) file: MultipartFile) = "The file name ${file.originalFilename}, size ${file.size} uploaded successfully."

    @PostMapping("/combine", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Upload pdf files to combine",
        responses = [
            ApiResponse(responseCode = "200", description = "Success"),
            ApiResponse(responseCode = "500", description = "Not a file")
        ]
    )
    fun combinePdf(@RequestParam(value = "files", required = true) files: List<MultipartFile>): ResponseEntity<ByteArrayResource> {
        val outputStream = ByteArrayOutputStream()
        val document = Document()
        val writer = PdfCopy(document, outputStream)
        document.open()
        for (file in files) {
            val pdfReader = PdfReader(file.inputStream)
            for (page in 1..pdfReader.numberOfPages) {
                val importedPage = writer.getImportedPage(pdfReader, page)
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

    @PostMapping("/add-watermark", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Add water mark to pdf",
        responses = [
            ApiResponse(responseCode = "200", description = "Success"),
            ApiResponse(responseCode = "500", description = "Not a file")
        ]
    )
    fun addWaterMark(
        @RequestParam(value = "file", required = true) file: MultipartFile,
        @RequestParam(value = "watermarkText", defaultValue = "Testing") watermarkText: String = "Testing",
        @RequestParam(value = "fontSize", defaultValue = "45f") fontSize: Float = 45f,
        @RequestParam(value = "rotation", defaultValue = "45f") rotation: Float = 45f
    ): ResponseEntity<ByteArrayResource> {
        val reader = PdfReader(file.inputStream)
        val outputStream = ByteArrayOutputStream()
        val stamper = PdfStamper(reader, outputStream)
        val baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED)
        val font = Font(Font.FontFamily.HELVETICA, fontSize, Font.NORMAL, BaseColor.LIGHT_GRAY)
        val phrase = Phrase(watermarkText, font)

        for (i in 1..reader.numberOfPages) {
            val pageSize = reader.getPageSize(i)
            val x = (pageSize.left + pageSize.right) / 2
            val y = (pageSize.top + pageSize.bottom) / 2
            val content = stamper.getOverContent(i)

            val pdfGState = PdfGState()
            pdfGState.setFillOpacity(0.2f)
            content.setGState(pdfGState)

            content.beginText()
            content.setFontAndSize(baseFont, font.size)
            content.showTextAligned(ALIGN_CENTER, phrase.content, x, y, rotation)
            content.endText()
        }
        stamper.close()
        reader.close()
        val resource = ByteArrayResource(outputStream.toByteArray())
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=watermarked.pdf")
            .contentLength(resource.contentLength())
            .contentType(MediaType.APPLICATION_PDF)
            .body(resource)
    }
}
