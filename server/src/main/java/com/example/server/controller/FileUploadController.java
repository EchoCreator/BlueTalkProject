package com.example.server.controller;

import com.example.common.properties.AliOssProperties;
import com.example.common.result.Result;
import com.example.common.utils.AliOssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class FileUploadController {
    @Autowired
    private AliOssProperties aliOssProperties;

    @PostMapping("/upload")
    public Result<String> FileUpload(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String url = AliOssUtil.uploadFile(fileName, file.getInputStream(), aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret(), aliOssProperties.getBucketName());
        return Result.success(url);
    }
}
