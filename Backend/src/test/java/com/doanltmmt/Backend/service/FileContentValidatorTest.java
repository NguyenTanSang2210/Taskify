package com.doanltmmt.Backend.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Whitelist đuôi + magic bytes + làm sạch tên file (thuần đơn vị). */
class FileContentValidatorTest {

    private static byte[] bytes(int... v) {
        byte[] b = new byte[v.length + 16];
        for (int i = 0; i < v.length; i++) b[i] = (byte) v[i];
        return b;
    }

    private static void rejects(String name, byte[] content) {
        assertThrows(IllegalArgumentException.class, () -> FileContentValidator.validate(name, content), name);
    }

    @Test
    void acceptsEachAllowedTypeWithMatchingContent() {
        assertEquals("pdf", FileContentValidator.validate("a.pdf", bytes('%', 'P', 'D', 'F', '-', '1')));
        for (String e : new String[]{"doc", "xls", "ppt"}) {
            assertEquals(e, FileContentValidator.validate("a." + e, bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)));
        }
        for (String e : new String[]{"docx", "xlsx", "pptx", "zip"}) {
            assertEquals(e, FileContentValidator.validate("a." + e, bytes('P', 'K', 3, 4)));
        }
        assertEquals("rar", FileContentValidator.validate("a.rar", bytes('R', 'a', 'r', '!', 0x1A, 0x07)));
        assertEquals("png", FileContentValidator.validate("a.png", bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals("jpg", FileContentValidator.validate("a.jpg", bytes(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("jpeg", FileContentValidator.validate("A.JPEG", bytes(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("txt", FileContentValidator.validate("ghi chú.txt", "Xin chào — tiếng Việt".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsDisallowedExtensions() {
        byte[] mz = bytes('M', 'Z');
        for (String n : new String[]{"virus.exe", "a.bat", "a.js", "a.html", "a.svg", "a.php", "a.jar", "a.sh", "noext", "a."}) {
            rejects(n, mz);
        }
    }

    @Test
    void rejectsContentThatDoesNotMatchExtension() {
        rejects("fake.pdf", "đây không phải pdf".getBytes(StandardCharsets.UTF_8));
        rejects("fake.png", bytes(0xFF, 0xD8, 0xFF));          // JPEG đội lốt PNG
        rejects("fake.docx", bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)); // OLE2 đội lốt docx
        rejects("fake.zip", bytes('M', 'Z'));
        rejects("exe-as.pdf", bytes('M', 'Z', 0x90, 0));
    }

    @Test
    void rejectsBinaryOrInvalidTextAsTxt() {
        rejects("a.txt", new byte[]{'a', 0, 'b'});                       // có byte NUL
        rejects("a.txt", new byte[]{(byte) 0xC3, (byte) 0x28});          // UTF-8 sai
    }

    @Test
    void rejectsEmptyFile() {
        rejects("a.pdf", new byte[0]);
    }

    @Test
    void sanitizeRemovesPathsControlCharsAndDangerousCharacters() {
        assertEquals("passwd", FileContentValidator.sanitizeOriginalName("../../etc/passwd"));
        assertEquals("a.pdf", FileContentValidator.sanitizeOriginalName("C:\\Users\\x\\a.pdf"));
        assertEquals("ab.txt", FileContentValidator.sanitizeOriginalName("a\u0000b\n.txt"));
        assertEquals("Báo cáo tuần 1.docx", FileContentValidator.sanitizeOriginalName("Báo cáo tuần 1.docx"));
        assertFalse(FileContentValidator.sanitizeOriginalName("<script>\"x\".pdf").contains("<"));
        assertEquals("hidden.txt", FileContentValidator.sanitizeOriginalName("..hidden.txt"));
    }

    @Test
    void sanitizeCapsLengthButKeepsExtension() {
        String longName = "a".repeat(500) + ".pdf";
        String out = FileContentValidator.sanitizeOriginalName(longName);
        assertEquals(200, out.length());
        assertEquals("pdf", FileContentValidator.extensionOf(out));
    }

    @Test
    void sanitizeHandlesNull() {
        assertEquals("", FileContentValidator.sanitizeOriginalName(null));
    }
}
