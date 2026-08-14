import { useApp } from '../context/AppContext';

export function SourceList({ sources }: { sources: { fileName: string; pageNumber?: number; chunkNumber?: number }[] }) {
  const { t } = useApp();

  return (
    <div className="sources">
      <strong>{t('sourcesTitle')}:</strong>
      <ul>
        {sources.map((source, idx) => (
          <li key={idx}>
            <svg className="icon" style={{ width: 14, height: 14 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            {source.fileName}
            {source.pageNumber != null && <span> - Página {source.pageNumber}</span>}
            {source.chunkNumber != null && <span> - Chunk {source.chunkNumber}</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}
