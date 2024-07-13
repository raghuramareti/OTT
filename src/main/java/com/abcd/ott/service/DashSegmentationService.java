package com.abcd.ott.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class DashSegmentationService {

    private static final String FFMPEG_PATH = "ffmpeg";
    private static final String MP4BOX_PATH = "MP4Box";

    public void segmentVideoForDash(String inputFilePath, String outputDirPath) throws IOException, InterruptedException {
        // Ensure the output directory exists
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Convert the video to fragmented MP4 using ffmpeg
        String fragmentedMp4Path = outputDirPath + File.separator + "output.mp4";
        ProcessBuilder ffmpegBuilder = new ProcessBuilder(
                FFMPEG_PATH, "-i", inputFilePath, "-codec:", "copy", "-f", "mp4", "-movflags",
                "frag_keyframe" +
                "+empty_moov", fragmentedMp4Path
        );
        Process ffmpegProcess = ffmpegBuilder.start();
        ffmpegProcess.waitFor();

        // Segment the video using MP4Box
        ProcessBuilder mp4boxBuilder = new ProcessBuilder(
                MP4BOX_PATH, "-dash", "4000", "-frag", "4000", "-rap", "-segment-name", "segment_", "-out", outputDirPath + File.separator + "manifest.mpd", fragmentedMp4Path
        );
        Process mp4boxProcess = mp4boxBuilder.start();
        mp4boxProcess.waitFor();
    }

    private void generateDashManifest(String outputDirPath) throws IOException {
        // Placeholder for generating DASH manifest (MPD) files
        // In a real-world scenario, you would use a library or tool to generate the MPD files
        File mpdFile = new File(outputDirPath, "output.mpd");
        try (FileOutputStream fos = new FileOutputStream(mpdFile)) {
            String mpdContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "                    <MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" minBufferTime=\"PT1.5S\" profiles=\"urn:mpeg:dash:profile:isoff-live:2011\" type=\"static\">\n" +
                    "                      <Period>\n" +
                    "                        <AdaptationSet mimeType=\"video/mp4\" segmentAlignment=\"true\">\n" +
                    "                          <Representation id=\"1\" codecs=\"avc1.42E01E\" width=\"640\" height=\"360\" frameRate=\"30\" bandwidth=\"500000\">\n" +
                    "                            <BaseURL>output.mp4</BaseURL>\n" +
                    "                            <SegmentBase>\n" +
                    "                              <Initialization range=\"0-674\"/>\n" +
                    "                              <IndexRange range=\"675-999\"/>\n" +
                    "                            </SegmentBase>\n" +
                    "                          </Representation>\n" +
                    "                        </AdaptationSet>\n" +
                    "                      </Period>\n" +
                    "                    </MPD>";
            fos.write(mpdContent.getBytes());
        }
    }
}
