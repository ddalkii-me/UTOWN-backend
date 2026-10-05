package com.utown.utownbackend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@ConditionalOnProperty(
        name = "aws.s3.enabled",
        havingValue = "false"
)
public class DisabledFileStorageService implements FileStorageService {

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        throw new IllegalStateException(
                "File uploads are disabled because AWS S3 is not enabled."
        );
    }
}
