package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.enums.ProgressStatus;
import com.doanltmmt.Backend.config.StoredFileMaintenance;
import com.doanltmmt.Backend.entity.ProgressReport;
import com.doanltmmt.Backend.entity.StoredFile;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.ProgressReportRepository;
import com.doanltmmt.Backend.repository.StoredFileRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Upload/download an toàn: test bắt buộc #13 (SV A không tải được file SV B) và #14 (upload .exe bị từ chối),
 * cộng magic bytes, giới hạn kích thước, gắn file của người khác, gắn trùng, tên tiếng Việt, backfill, dọn rác.
 */
@SuppressWarnings("null")
class FileAccessIT extends AbstractTopicScenarioIT {

    @Autowired private StoredFileRepository fileRepo;
    @Autowired private ProgressReportRepository progressRepo;
    @Autowired private StoredFileMaintenance maintenance;

    private static final byte[] PDF = "%PDF-1.4 noi dung thu nghiem".getBytes(StandardCharsets.UTF_8);

    private ResultActions upload(User as, String name, byte[] content) throws Exception {
        return mvc.perform(multipart("/api/upload")
                .file(new MockMultipartFile("file", name, "application/octet-stream", content))
                .header(AUTH, bearer(as)));
    }

    /** Tải lên một PDF hợp lệ và trả về fileUrl. */
    private String uploadPdf(User as, String name) throws Exception {
        String body = upload(as, name, PDF).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.fileUrl");
    }

    private ResultActions submitReport(User student, long topicId, String fileUrl) throws Exception {
        return post("/api/progress/create?studentId={s}&topicId={t}", student,
                Map.of("title", "Báo cáo", "content", "Nội dung", "fileUrl", fileUrl),
                student.getId(), topicId);
    }

    // ------------------------------------------------------------------ upload (#14)

    @Test
    void upload_executable_isRejected() throws Exception {
        upload(userOf(stuA1), "virus.exe", new byte[]{'M', 'Z', (byte) 0x90, 0, 3}).andExpect(status().isBadRequest());
        assertEquals(0, fileRepo.count());
    }

    @Test
    void upload_pdfWithWrongContent_isRejected() throws Exception {
        upload(userOf(stuA1), "fake.pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_tooLarge_isRejected() throws Exception {
        byte[] big = new byte[21 * 1024 * 1024];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        upload(userOf(stuA1), "big.pdf", big).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void upload_requiresAuthentication() throws Exception {
        mvc.perform(multipart("/api/upload").file(new MockMultipartFile("file", "a.pdf", "application/pdf", PDF)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_pathTraversalInName_isNeutralised() throws Exception {
        String body = upload(userOf(stuA1), "../../etc/evil.pdf", PDF).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String storedName = JsonPath.read(body, "$.fileName");
        assertTrue(storedName.matches("[0-9a-f-]{36}\\.pdf"), storedName);
        assertEquals("evil.pdf", JsonPath.read(body, "$.originalName"));
        assertTrue(((String) JsonPath.read(body, "$.fileUrl")).startsWith("/api/secure/files/"));
    }

    // ------------------------------------------------------------------ download (#13)

    @Test
    void download_ownerAndTopicLecturerAllowed_otherStudentDenied() throws Exception {
        String url = uploadPdf(userOf(stuA1), "bao-cao.pdf");
        submitReport(userOf(stuA1), topicA.getId(), url).andExpect(status().isOk());

        get(url, userOf(stuA1)).andExpect(status().isOk());          // chủ file
        get(url, userOf(lecA)).andExpect(status().isOk());           // giảng viên hướng dẫn
        get(url, deptAdminA).andExpect(status().isOk());             // quản trị khoa của đề tài
        get(url, admin).andExpect(status().isOk());

        get(url, userOf(stuA2)).andExpect(status().isForbidden());   // sinh viên khác cùng đề tài
        get(url, userOf(stuB1)).andExpect(status().isForbidden());   // sinh viên đề tài khác
        get(url, userOf(lecB)).andExpect(status().isForbidden());    // giảng viên khác
        get(url, deptAdminB).andExpect(status().isForbidden());      // quản trị khoa khác
    }

    @Test
    void download_unattachedFile_onlyOwnerAndAdmin() throws Exception {
        String url = uploadPdf(userOf(stuA1), "chua-gan.pdf");
        get(url, userOf(stuA1)).andExpect(status().isOk());
        get(url, admin).andExpect(status().isOk());
        get(url, userOf(lecA)).andExpect(status().isForbidden());
        get(url, userOf(stuA2)).andExpect(status().isForbidden());
    }

    @Test
    void download_unknownFile_is404_andTraversalDoesNotEscape() throws Exception {
        get("/api/secure/files/khong-ton-tai.pdf", admin).andExpect(status().isNotFound());
        get("/api/secure/files/{n}", admin, "..%2F..%2Fpom.xml").andExpect(status().is4xxClientError());
    }

    @Test
    void download_withoutToken_is401() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/secure/files/x.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_vietnameseFileName_isEncodedAndNosniff() throws Exception {
        String url = uploadPdf(userOf(stuA1), "Báo cáo tuần 1.pdf");
        get(url, userOf(stuA1))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("filename*=UTF-8''B%C3%A1o%20c%C3%A1o")));
    }

    // ------------------------------------------------------------------ gắn file vào báo cáo

    @Test
    void attach_someoneElsesFile_isForbidden() throws Exception {
        String othersFile = uploadPdf(userOf(stuA2), "cua-nguoi-khac.pdf");
        submitReport(userOf(stuA1), topicA.getId(), othersFile).andExpect(status().isForbidden());
        assertEquals(0, progressRepo.count());
    }

    @Test
    void attach_unknownFileUrl_isRejected() throws Exception {
        submitReport(userOf(stuA1), topicA.getId(), "/api/secure/files/tu-bia-ra.pdf").andExpect(status().isBadRequest());
    }

    @Test
    void attach_sameFileTwice_secondIsRejected() throws Exception {
        String url = uploadPdf(userOf(stuA1), "mot-lan.pdf");
        submitReport(userOf(stuA1), topicA.getId(), url).andExpect(status().isOk());
        submitReport(userOf(stuA1), topicA.getId(), url).andExpect(status().isConflict());
        assertEquals(1, progressRepo.count());
    }

    @Test
    void attach_marksFileAsReferencedByReport() throws Exception {
        String url = uploadPdf(userOf(stuA1), "gan.pdf");
        submitReport(userOf(stuA1), topicA.getId(), url).andExpect(status().isOk());
        StoredFile f = fileRepo.findByStoredName(url.substring(url.lastIndexOf('/') + 1)).orElseThrow();
        assertEquals(StoredFile.REF_PROGRESS_REPORT, f.getRefType());
        assertEquals(progressRepo.findAll().get(0).getId(), f.getRefId());
    }

    // ------------------------------------------------------------------ backfill + dọn rác

    @Test
    void backfill_createsRecordsForLegacyReports_andIsIdempotent() {
        ProgressReport pr = new ProgressReport();
        pr.setTitle("cũ");
        pr.setContent("cũ");
        pr.setStatus(ProgressStatus.TODO);
        pr.setTopic(topicA);
        pr.setStudent(stuA1);
        pr.setFileUrl("/api/secure/files/123e4567-e89b-12d3-a456-426614174000_Báo cáo cũ.doc");
        pr = progressRepo.saveAndFlush(pr);

        assertEquals(1, maintenance.backfill());
        assertEquals(0, maintenance.backfill());

        StoredFile f = fileRepo.findByStoredName("123e4567-e89b-12d3-a456-426614174000_Báo cáo cũ.doc").orElseThrow();
        assertEquals(stuA1.getId(), f.getOwnerId());
        assertEquals("Báo cáo cũ.doc", f.getOriginalName());
        assertEquals(pr.getId(), f.getRefId());
    }

    @Test
    void purge_removesOnlyOldUnattachedFiles() throws Exception {
        String oldUrl = uploadPdf(userOf(stuA1), "cu.pdf");
        String freshUrl = uploadPdf(userOf(stuA1), "moi.pdf");
        String attachedUrl = uploadPdf(userOf(stuA1), "da-gan.pdf");
        submitReport(userOf(stuA1), topicA.getId(), attachedUrl).andExpect(status().isOk());

        for (String u : new String[]{oldUrl, attachedUrl}) {
            StoredFile f = fileRepo.findByStoredName(u.substring(u.lastIndexOf('/') + 1)).orElseThrow();
            f.setCreatedAt(LocalDateTime.now().minusHours(48));
            fileRepo.saveAndFlush(f);
        }

        assertEquals(1, maintenance.purgeOrphans());
        assertTrue(fileRepo.findByStoredName(oldUrl.substring(oldUrl.lastIndexOf('/') + 1)).isEmpty());
        assertFalse(fileRepo.findByStoredName(freshUrl.substring(freshUrl.lastIndexOf('/') + 1)).isEmpty());
        assertFalse(fileRepo.findByStoredName(attachedUrl.substring(attachedUrl.lastIndexOf('/') + 1)).isEmpty());
    }
}
