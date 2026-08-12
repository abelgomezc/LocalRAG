package com.localrag.dto.response;

public class HealthResponse {
    private String application;
    private String ollama;
    private String database;

    public HealthResponse() {
    }

    public HealthResponse(String application, String ollama, String database) {
        this.application = application;
        this.ollama = ollama;
        this.database = database;
    }

    public String getApplication() {
        return application;
    }

    public String getOllama() {
        return ollama;
    }

    public String getDatabase() {
        return database;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setOllama(String ollama) {
        this.ollama = ollama;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
