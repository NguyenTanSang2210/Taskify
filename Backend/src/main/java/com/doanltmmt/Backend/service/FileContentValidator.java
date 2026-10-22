package com.doanltmmt.Backend.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm tra file tải lên: đuôi nằm trong danh sách cho phép VÀ nội dung (magic bytes) khớp với đuôi.
 * Tự viết (thay vì Apache Tika) để ít phụ thuộc và kiểm soát được: chỉ có 13 loại file được chấp nhận.
 */
public final class FileContentValidator {

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar", "png", "jpg", "jpeg", "txt");

    private static final byte[] OLE2 = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] ZIP = {'P', 'K', 3, 4};
    private static final byte[] ZIP_EMPTY = {'P', 'K', 5, 6};
    private static final byte[] RAR = {'R', 'a', 'r', '!', 0x1A, 0x07};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("txt", "text/plain"));

    private FileContentValidator() {}

    /** Đuôi file (chữ thường, không dấu chấm) của tên đã làm sạch; chuỗi rỗng nếu không có. */
    public static String extensionOf(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static String contentTypeFor(String extension) {
        return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * @return đuôi hợp lệ (chữ thường)
     * @throws IllegalArgumentException (thông báo tiếng Việt) nếu đuôi không được phép hoặc nội dung không khớp đuôi
     */
    public static String validate(String sanitizedName, byte[] content) {
        String ext = extensionOf(sanitizedName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ chấp nhận: "
                    + String.join(", ", ALLOWED_EXTENSIONS.stream().sorted().toList()) + ".");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File rỗng.");
        }
        boolean ok = switch (ext) {
            case "pdf" -> startsWith(content, PDF);
            case "doc", "xls", "ppt" -> startsWith(content, OLE2);
            case "docx", "xlsx", "pptx", "zip" -> startsWith(content, ZIP) || startsWith(content, ZIP_EMPTY);
            case "rar" -> startsWith(content, RAR);
            case "png" -> startsWith(content, PNG);
            case "jpg", "jpeg" -> startsWith(content, JPG);
            case "txt" -> isPlainUtf8Text(content);
            default -> false;
        };
        if (!ok) {
            throw new IllegalArgumentException("Nội dung file không khớp với đuôi ." + ext + ".");
        }
        return ext;
    }

    /** Làm sạch tên gốc: bỏ đường dẫn, ký tự điều khiển và ký tự nguy hiểm; tối đa 200 ký tự. */
    public static String sanitizeOriginalName(String raw) {
        String name = raw == null ? "" : raw;
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isISOControl(c) || "\"<>:*?|".indexOf(c) >= 0) continue;
            sb.append(c);
        }
        name = sb.toString().trim();
        while (name.startsWith(".")) name = name.substring(1);
        if (name.length() > 200) {
            String ext = extensionOf(name);
            String base = name.substring(0, name.length() - (ext.isEmpty() ? 0 : ext.length() + 1));
            int keep = 200 - (ext.isEmpty() ? 0 : ext.length() + 1);
            name = base.substring(0, Math.max(1, keep)) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name;
    }

    private static boolean startsWith(byte[] content, byte[] magic) {
        if (content.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (content[i] != magic[i]) return false;
        }
        return true;
    }

    private static boolean isPlainUtf8Text(byte[] content) {
        for (byte b : content) {
            if (b == 0) return false;
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
