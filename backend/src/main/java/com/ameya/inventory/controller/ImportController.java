package com.ameya.inventory.controller;

import com.ameya.inventory.dto.migration.ImportDtos;
import com.ameya.inventory.migration.ExcelImportService;
import com.ameya.inventory.migration.ImportFileType;
import com.ameya.inventory.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ImportController {

    private final ExcelImportService importService;

    @PostMapping("/preview")
    public ImportDtos.ImportResult preview(@RequestPart MultipartFile file,
                                             @RequestParam ImportFileType fileType,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return importService.preview(fileType, bytes(file), file.getOriginalFilename(), principal.getId());
    }

    @PostMapping("/commit")
    public ImportDtos.ImportResult commit(@RequestPart MultipartFile file,
                                            @RequestParam ImportFileType fileType,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return importService.commit(fileType, bytes(file), file.getOriginalFilename(), principal.getId());
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
