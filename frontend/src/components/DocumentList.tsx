import { useEffect, useState } from 'react';
import { documentsApi } from '../api/documentsApi';
import { Documento } from '../types/document.types';
import { useApp } from '../context/AppContext';

export function DocumentList({ onDeleted }: { onDeleted: () => void }) {
  const [documents, setDocuments] = useState<Documento[]>([]);
  const { t } = useApp();

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      const data = await documentsApi.list();
      setDocuments(data);
    } catch {
      setDocuments([]);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm(t('confirmDelete'))) return;
    try {
      await documentsApi.delete(id);
      onDeleted();
    } catch {
      alert(t('errorDelete'));
    }
  };

  return (
    <div className="document-list">
      <h3>
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2 2h6a2 2 0 012 2v1M5 19h14a2 2 0 002-2v-5a2 2 0 00-2-2H9a2 2 0 00-2 2v5a2 2 0 01-2 2z" />
        </svg>
        {t('documentsTitle')}
      </h3>
      {documents.length === 0 ? (
        <div className="empty-state">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 13h6m-3-3v6m-9 1h18a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <p>{t('noDocuments')}</p>
        </div>
      ) : (
        <ul>
          {documents.map((doc) => (
            <li key={doc.id}>
              <div className="doc-name">
                <svg className="icon" style={{ width: 16, height: 16 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                {doc.fileName}
              </div>
              <div className="meta">
                <svg className="icon" style={{ width: 14, height: 14 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
                {doc.totalChunks} {t('chunks')}
              </div>
              <div className="meta">
                <span className={`status-badge ${doc.status === 'PROCESSED' ? 'processed' : 'processing'}`}>
                  {doc.status === 'PROCESSED' ? t('ready') : t('processing_status')}
                </span>
              </div>
              <div className="doc-actions">
                <button onClick={() => handleDelete(doc.id)}>
                  {t('delete')}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
