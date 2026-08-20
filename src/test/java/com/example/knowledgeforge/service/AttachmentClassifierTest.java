package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.attachment.AttachmentType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentClassifierTest {

    private static byte[] mp4Header() {
        byte[] h = new byte[16];
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, h, 4, 4);
        System.arraycopy("isom".getBytes(StandardCharsets.US_ASCII), 0, h, 8, 4);
        return h;
    }

    @Test
    void looksLikeMp4Signature_true_for_valid_ftyp_box() {
        assertTrue(AttachmentClassifier.looksLikeMp4Signature(mp4Header(), 16));
    }

    @Test
    void looksLikeMp4Signature_false_for_random_bytes() {
        byte[] header = "this is definitely not mp4 content!".getBytes(StandardCharsets.UTF_8);
        assertFalse(AttachmentClassifier.looksLikeMp4Signature(header, header.length));
    }

    @Test
    void looksLikeMp4Signature_false_when_header_too_short() {
        assertFalse(AttachmentClassifier.looksLikeMp4Signature(new byte[]{1, 2, 3}, 3));
    }

    @Test
    void classify_video_when_extension_signature_and_contentType_all_agree() {
        AttachmentType type = AttachmentClassifier.classify("mp4", "video/mp4", null, mp4Header(), 16);
        assertEquals(AttachmentType.VIDEO, type);
    }

    @Test
    void classify_video_when_only_probed_contentType_matches() {
        AttachmentType type = AttachmentClassifier.classify("mp4", "application/octet-stream", "video/mp4", mp4Header(), 16);
        assertEquals(AttachmentType.VIDEO, type);
    }

    @Test
    void classify_file_when_extension_is_not_mp4_even_with_valid_signature_and_contentType() {
        AttachmentType type = AttachmentClassifier.classify("mov", "video/mp4", "video/mp4", mp4Header(), 16);
        assertEquals(AttachmentType.FILE, type);
    }

    @Test
    void classify_file_when_signature_missing_even_if_extension_and_contentType_claim_mp4() {
        // Nazwa/Content-Type mogą kłamać (np. przemianowany plik) — bez prawdziwej sygnatury MP4 nigdy VIDEO.
        byte[] fakeHeader = "not really an mp4 payload".getBytes(StandardCharsets.UTF_8);
        AttachmentType type = AttachmentClassifier.classify("mp4", "video/mp4", "video/mp4", fakeHeader, fakeHeader.length);
        assertEquals(AttachmentType.FILE, type);
    }

    @Test
    void classify_file_when_no_contentType_claims_mp4_even_with_extension_and_signature() {
        AttachmentType type = AttachmentClassifier.classify("mp4", "application/octet-stream", "application/octet-stream", mp4Header(), 16);
        assertEquals(AttachmentType.FILE, type);
    }

    @Test
    void classify_file_for_ordinary_pdf_document() {
        byte[] pdfHeader = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        AttachmentType type = AttachmentClassifier.classify("pdf", "application/pdf", "application/pdf", pdfHeader, pdfHeader.length);
        assertEquals(AttachmentType.FILE, type);
    }
}
