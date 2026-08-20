package com.example.knowledgeforge.domain.attachment;

/**
 * VIDEO — wyłącznie nagrania spotkań Teams w MP4, odtwarzane inline w przeglądarce
 * (zob. AttachmentClassifier — rozszerzenie + sygnatura pliku + Content-Type muszą się zgadzać).
 * FILE — wszystko inne (xlsx, docx, pdf, zip, obrazy, ...), zawsze tylko do pobrania.
 */
public enum AttachmentType {
    VIDEO, FILE
}
