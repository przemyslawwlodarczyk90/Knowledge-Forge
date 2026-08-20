package com.example.knowledgeforge.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttachmentServletRangeTest {

    private static final long FILE_SIZE = 1000;

    @Test
    void noRangeHeader_returnsNull_meaningWholeFile() {
        assertNull(AttachmentServlet.parseRange(null, FILE_SIZE));
    }

    @Test
    void simpleRange_startEnd() {
        assertArrayEquals(new long[]{100, 199}, AttachmentServlet.parseRange("bytes=100-199", FILE_SIZE));
    }

    @Test
    void openEndedRange_goesToEndOfFile() {
        assertArrayEquals(new long[]{900, 999}, AttachmentServlet.parseRange("bytes=900-", FILE_SIZE));
    }

    @Test
    void suffixRange_lastNBytes() {
        assertArrayEquals(new long[]{900, 999}, AttachmentServlet.parseRange("bytes=-100", FILE_SIZE));
    }

    @Test
    void endBeyondFileSize_isClampedToLastByte() {
        assertArrayEquals(new long[]{500, 999}, AttachmentServlet.parseRange("bytes=500-999999", FILE_SIZE));
    }

    @Test
    void startBeyondFileSize_signalsRangeNotSatisfiable() {
        assertArrayEquals(new long[]{-1, -1}, AttachmentServlet.parseRange("bytes=5000-6000", FILE_SIZE));
    }

    @Test
    void malformedRange_returnsNull_fallsBackToWholeFile() {
        assertNull(AttachmentServlet.parseRange("bytes=abc-def", FILE_SIZE));
        assertNull(AttachmentServlet.parseRange("not-bytes-at-all", FILE_SIZE));
        assertNull(AttachmentServlet.parseRange("bytes=", FILE_SIZE));
    }

    @Test
    void multiRange_notSupported_fallsBackToWholeFile() {
        assertNull(AttachmentServlet.parseRange("bytes=0-99,200-299", FILE_SIZE));
    }
}
