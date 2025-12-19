package com.clause.app.domain.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${clause.storage.root}") String rootPath) {
        this.rootLocation = Paths.get(rootPath);
        try {
            Files.createDirectories(rootLocation);
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize storage", e);
        }
    }

    @Override
    public Path store(MultipartFile file) throws Exception {
        String filename = UUID.randomUUID().toString() + "_" + sanitizeFilename(file.getOriginalFilename());
        Path destinationFile = rootLocation.resolve(Paths.get(filename))
                .normalize()
                .toAbsolutePath();

        if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
            throw new SecurityException("Cannot store file outside current directory");
        }

        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
        return destinationFile;
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filename, e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

