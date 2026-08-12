package com.localrag.dto.response;

public class ChatResponse {
    private String answer;
    private java.util.List<Source> sources;

    public ChatResponse() {
    }

    public ChatResponse(String answer, java.util.List<Source> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public java.util.List<Source> getSources() {
        return sources;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setSources(java.util.List<Source> sources) {
        this.sources = sources;
    }

    public static class Source {
        private String fileName;
        private Integer pageNumber;
        private Integer chunkNumber;

        public Source() {
        }

        public Source(String fileName, Integer pageNumber, Integer chunkNumber) {
            this.fileName = fileName;
            this.pageNumber = pageNumber;
            this.chunkNumber = chunkNumber;
        }

        public String getFileName() {
            return fileName;
        }

        public Integer getPageNumber() {
            return pageNumber;
        }

        public Integer getChunkNumber() {
            return chunkNumber;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public void setPageNumber(Integer pageNumber) {
            this.pageNumber = pageNumber;
        }

        public void setChunkNumber(Integer chunkNumber) {
            this.chunkNumber = chunkNumber;
        }
    }
}
