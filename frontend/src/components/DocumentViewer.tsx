import { useEffect, useState } from 'react';

export interface DocumentViewerProps {
  fileName: string;
  fileType: string;
  documentId?: number;
}

export function DocumentViewer({ fileName, fileType, documentId }: DocumentViewerProps) {
  const [textContent, setTextContent] = useState<string | null>(null);
  const fileUrl = `/uploads/${encodeURIComponent(fileName)}`;
  const isText = fileType === 'text/plain' || fileType === 'text/markdown' || fileType === 'text/x-markdown' || fileType === 'text/md'
    || fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    || fileType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    || fileType === 'text/csv'
    || fileName.toLowerCase().endsWith('.docx')
    || fileName.toLowerCase().endsWith('.xlsx')
    || fileName.toLowerCase().endsWith('.csv');

  useEffect(() => {
    if (!isText) return;
    const url = documentId ? `/api/documents/${documentId}/content` : fileUrl;
    fetch(url)
      .then((res) => res.text())
      .then(setTextContent)
      .catch(() => setTextContent('No se pudo cargar el contenido del documento.'));
  }, [fileUrl, isText, documentId]);

  if (isText) {
    return (
      <div className="document-viewer">
        <div className="viewer-header">
          <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <span className="viewer-title">{fileName}</span>
        </div>
        <div className="viewer-body">
          <pre>{textContent || 'Cargando...'}</pre>
        </div>
      </div>
    );
  }

  return (
    <div className="document-viewer">
      <div className="viewer-header">
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <span className="viewer-title">{fileName}</span>
      </div>
      <div className="viewer-body">
        <iframe src={fileUrl} title={fileName} />
      </div>
    </div>
  );
}
