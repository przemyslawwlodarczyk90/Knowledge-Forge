package com.example.knowledgeforge.service;

import com.example.knowledgeforge.domain.attachment.AttachmentType;

import java.nio.charset.StandardCharsets;

/**
 * W aplikacji odtwarzamy w przeglądarce WYŁĄCZNIE nagrania spotkań Teams w MP4.
 * Żeby oznaczyć plik jako VIDEO, muszą się zgodzić WSZYSTKIE trzy niezależne sygnały:
 *  1. rozszerzenie pliku == "mp4",
 *  2. deklarowany (klient) LUB wykryty (Files.probeContentType) Content-Type == video/mp4,
 *  3. pierwsze bajty pliku mają sygnaturę ISO-BMFF/MP4 ("ftyp" w offsecie 4).
 * Żadnemu z tych sygnałów z osobna nie ufamy — Content-Type z żądania klienta jest
 * trywialny do sfałszowania, a samo rozszerzenie nic nie mówi o rzeczywistej zawartości.
 * To nie jest pełny parser kontenera MP4 (projekt świadomie nie dodaje do tego ciężkiej
 * zależności) — to tania, ale skuteczna obrona przed najbardziej oczywistym nadużyciem
 * (przemianowanie dowolnego pliku na .mp4, żeby wymusić inline-odtwarzanie w /view).
 * Każdy plik, który nie przejdzie kompletu testów, ląduje jako zwykły FILE (do pobrania) —
 * upload nie jest odrzucany, po prostu nie jest traktowany jak odtwarzalne wideo.
 */
public final class AttachmentClassifier {

    private static final String MP4_CONTENT_TYPE = "video/mp4";

    /** Offset w pliku, gdzie w kontenerze ISO-BMFF (MP4/MOV/...) zaczyna się box "ftyp". */
    private static final int FTYP_OFFSET = 4;
    private static final byte[] FTYP_MAGIC = "ftyp".getBytes(StandardCharsets.US_ASCII);

    private AttachmentClassifier() {
    }

    /** Sprawdza bajty 4..7 wg specyfikacji ISO-BMFF ("ftyp" box header). */
    public static boolean looksLikeMp4Signature(byte[] header, int length) {
        if (header == null || length < FTYP_OFFSET + FTYP_MAGIC.length) return false;
        for (int i = 0; i < FTYP_MAGIC.length; i++) {
            if (header[FTYP_OFFSET + i] != FTYP_MAGIC[i]) return false;
        }
        return true;
    }

    private static boolean looksLikeMp4ContentType(String contentType) {
        return contentType != null && MP4_CONTENT_TYPE.equalsIgnoreCase(contentType.trim());
    }

    /**
     * Klasyfikuje plik jako VIDEO tylko gdy rozszerzenie, sygnatura pliku i przynajmniej
     * jedno źródło Content-Type (zadeklarowany przez klienta albo wykryty przez
     * {@link java.nio.file.Files#probeContentType}) zgodnie wskazują na MP4.
     */
    public static AttachmentType classify(String extension, String declaredContentType,
                                           String probedContentType, byte[] header, int headerLength) {
        boolean extensionOk = "mp4".equals(extension);
        boolean signatureOk = looksLikeMp4Signature(header, headerLength);
        boolean contentTypeOk = looksLikeMp4ContentType(declaredContentType) || looksLikeMp4ContentType(probedContentType);

        return (extensionOk && signatureOk && contentTypeOk) ? AttachmentType.VIDEO : AttachmentType.FILE;
    }
}
