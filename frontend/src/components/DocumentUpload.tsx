import { useState, useCallback } from 'react';
import { documentsApi } from '../api/documentsApi';

export function DocumentUpload({ onUploaded }: { onUploaded: () => void }) {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const handleFiles = useCallback(async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    setUploading(true);
    setProgress(0);
    setError(null);

    const interval = setInterval(() => {
      setProgress((prev) => {
        if (prev >= 90) {
          clearInterval(interval);
          return 90;
        }
        return prev + 10;
      });
    }, 200);

    try {
      await documentsApi.uploadMultiple(Array.from(files));
      setProgress(100);
      setTimeout(() => {
        onUploaded();
        setProgress(0);
      }, 500);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al subir los documentos.');
      setProgress(0);
    } finally {
      setUploading(false);
    }
  }, [onUploaded]);

  const handleFileChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    handleFiles(e.target.files);
    e.target.value = '';
  }, [handleFiles]);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
  }, []);

  const handleDrop = useCallback(async (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    handleFiles(e.dataTransfer.files);
  }, [handleFiles]);

  return (
    <div className="document-upload">
      <h3>
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        Subir documentos
      </h3>
      <div
        className={`upload-area ${dragOver ? 'drag-over' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => document.getElementById('file-input')?.click()}
      >
        <svg className="icon icon-lg" style={{ margin: '0 auto 0.5rem', opacity: 0.4 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <p style={{ fontSize: '0.9rem', color: '#6b7280', marginBottom: '0.5rem' }}>
          {uploading ? 'Procesando documentos...' : 'Arrastra archivos aquí o haz clic para seleccionar'}
        </p>
        <p style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
          PDF, TXT o Markdown — máximo 5 documentos
        </p>
        <input
          id="file-input"
          type="file"
          accept=".pdf,.txt,.md,.markdown"
          multiple
          onChange={handleFileChange}
          disabled={uploading}
        />
      </div>
      {uploading && (
        <div className="progress-bar">
          <div className="progress-bar-fill" style={{ width: `${progress}%` }} />
        </div>
      )}
      {error && (
        <div className="error-alert fade-in" style={{ marginTop: '0.75rem' }}>
          <svg className="error-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div>
            <div className="error-title">Error</div>
            <div>{error}</div>
          </div>
        </div>
      )}
    </div>
  );
}
