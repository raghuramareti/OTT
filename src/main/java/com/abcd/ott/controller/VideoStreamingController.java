package com.abcd.ott.controller;

import com.abcd.ott.service.DashSegmentationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class VideoStreamingController {

    private final Path videoLocation = Paths.get("videos");

    @Autowired
    private DashSegmentationService dashSegmentationService;

    @GetMapping("/video/{filename}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename, @RequestHeader HttpHeaders headers) throws IOException {
        Path filePath = videoLocation.resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        long fileLength = resource.contentLength();
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);

        if (range != null) {
            long start = range.getRangeStart(fileLength);
            long end = range.getRangeEnd(fileLength);
            long contentLength = end - start + 1;

            return ResponseEntity.status(206)
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                    .body(resource);
        } else {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                    .body(resource);
        }
    }

    @GetMapping("/segment")
    public ResponseEntity<String> segmentVideo(@RequestParam String inputFilePath, @RequestParam String outputDirPath) {
        try {
            dashSegmentationService.segmentVideoForDash(inputFilePath, outputDirPath);
            return ResponseEntity.ok("Video segmented successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error during video segmentation: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
