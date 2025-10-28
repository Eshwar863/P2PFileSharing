package org.example.Service;

import java.util.Arrays;

public class MultipartParser {
    final long MAX_FILE_SIZE = 1024L * 1024 * 1024; // 1 GB
    private final byte[] data;
    private final String boundary;

    public MultipartParser(byte[] data, String boundary) {
        this.data = data;
        this.boundary = boundary;
    }

    public static class ParseResult {

        public final String filename;
        public final String contentType;
        public final byte[] fileContent;

        public ParseResult(
                String filename,
                String contentType,
                byte[] fileContent
        ) {
            this.filename = filename;
            this.contentType = contentType;
            this.fileContent = fileContent;
        }
    }


    public ParseResult parse() {
        try {
            byte[] headerSeparator = { 13, 10, 13, 10 }; // \r\n\r\n
            int headerEndIndex = findSequence(data, headerSeparator, 0);
            if (headerEndIndex == -1) {
                return null;
            }
            int contentStartIndex = headerEndIndex + headerSeparator.length;

            String headers = new String(data, 0, headerEndIndex);

            byte[] boundarySeparator =
                    ("\r\n--" + this.boundary).getBytes();
            int contentEndIndex = findSequence(
                    data,
                    boundarySeparator,
                    contentStartIndex
            );

            if (contentEndIndex == -1) {
                boundarySeparator = ("\r\n--" +
                        this.boundary +
                        "--").getBytes();
                contentEndIndex = findSequence(
                        data,
                        boundarySeparator,
                        contentStartIndex
                );
            }

            if (contentEndIndex == -1) {
                return null;
            }

            byte[] fileContent = Arrays.copyOfRange(
                    data,
                    contentStartIndex,
                    contentEndIndex
            );

            String filename = "unnamed-file";
            String filenameMarker = "filename=\"";
            int filenameStart = headers.indexOf(filenameMarker);
            if (filenameStart != -1) {
                filenameStart += filenameMarker.length();
                int filenameEnd = headers.indexOf("\"", filenameStart);
                if (filenameEnd != -1) {
                    filename = headers.substring(
                            filenameStart,
                            filenameEnd
                    );
                }
            }

            String contentType = "application/octet-stream";
            String contentTypeMarker = "Content-Type: ";
            int contentTypeStart = headers.indexOf(contentTypeMarker);
            if (contentTypeStart != -1) {
                contentTypeStart += contentTypeMarker.length();
                int contentTypeEnd = headers.indexOf(
                        "\r\n",
                        contentTypeStart
                );
                if (contentTypeEnd != -1) {
                    contentType = headers.substring(
                            contentTypeStart,
                            contentTypeEnd
                    );
                }
            }

            return new ParseResult(filename, contentType, fileContent);
        } catch (Exception e) {
            System.err.println(
                    "Error parsing multipart data: " + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }
    private int findSequence(byte[] data, byte[] sequence, int startPos) {
        outer: for (
                int i = startPos;
                i <= data.length - sequence.length;
                i++
        ) {
            for (int j = 0; j < sequence.length; j++) {
                if (data[i + j] != sequence[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

}
