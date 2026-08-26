import { useState, useCallback } from 'react';
import { documentsApi } from '../api/documentsApi';
import { useApp } from '../context/AppContext';

const ALLOWED_TYPES = [
  'application/pdf',
  'text/plain',
  'text/markdown',
  'text/x-markdown',
  'text/md',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'text/csv',
];

const ALLOWED_EXTENSIONS = ['.pdf', '.txt', '.md', '.markdown', '.docx', '.xlsx', '.csv'];

export function DocumentUpload({ onUploaded }: { onUploaded: () => void }) {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const { t } = useApp();

  const validateFile = (file: File): string | null => {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      return `Tipo de archivo no soportado: ${ext}. Solo se permiten PDF, TXT, Markdown, Word, Excel y CSV.`;
    }
    const type = file.type || '';
    const typeOk = ALLOWED_TYPES.includes(type) || type === 'application/octet-stream';
    if (!typeOk) {
      return `Tipo de archivo no soportado: ${type || 'desconocido'}. Solo se permiten PDF, TXT, Markdown, Word, Excel y CSV.`;
    }
    if (file.size > 500 * 1024 * 1024) {
      return 'El archivo supera el tamaño máximo permitido de 500MB.';
    }
    return null;
  };

  const handleFiles = useCallback(async (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const invalidFiles: string[] = [];
    const validFiles: File[] = [];

    for (const file of Array.from(files)) {
      const validationError = validateFile(file);
      if (validationError) {
        invalidFiles.push(`${file.name}: ${validationError}`);
      } else {
        validFiles.push(file);
      }
    }

    if (invalidFiles.length > 0) {
      setError(invalidFiles.join('\n'));
      return;
    }

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
      await documentsApi.uploadMultiple(validFiles);
      setProgress(100);
      setTimeout(() => {
        onUploaded();
        setProgress(0);
      }, 500);
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || t('errorUpload');
      setError(message);
      setProgress(0);
    } finally {
      setUploading(false);
    }
  }, [onUploaded, t]);

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
        {t('uploadTitle')}
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
          {uploading ? t('processing') : t('uploadText')}
        </p>
        <p style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
          {t('uploadFormats')}
        </p>
        <input
          id="file-input"
          type="file"
          accept=".pdf,.txt,.md,.markdown,.docx,.xlsx,.csv"
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
            <div style={{ whiteSpace: 'pre-line' }}>{error}</div>
          </div>
        </div>
      )}
    </div>
  );
}
