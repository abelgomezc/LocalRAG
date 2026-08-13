import { useEffect, useState } from 'react';
import { documentsApi, DocumentRelation } from '../api/documentsApi';

interface SimpleDocument {
  id: number;
  fileName: string;
  status: string;
}

export function DocumentRelations({ documents }: { documents: SimpleDocument[] }) {
  const [relations, setRelations] = useState<DocumentRelation[]>([]);
  const [sourceId, setSourceId] = useState('');
  const [targetId, setTargetId] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    loadRelations();
  }, []);

  const loadRelations = async () => {
    try {
      const data = await documentsApi.listRelations();
      setRelations(data);
    } catch {
      setRelations([]);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!sourceId || !targetId) return;
    try {
      await documentsApi.createRelation(Number(sourceId), Number(targetId), description);
      setSourceId('');
      setTargetId('');
      setDescription('');
      loadRelations();
    } catch {
      alert('Error al crear relación.');
    }
  };

  const handleDelete = async (relationId: number) => {
    try {
      await documentsApi.deleteRelation(relationId);
      loadRelations();
    } catch {
      alert('Error al eliminar relación.');
    }
  };

  const processedDocs = documents.filter((d) => d.status === 'PROCESSED');

  return (
    <div className="document-relations">
      <h3>
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>
        Relaciones entre documentos
      </h3>
      <p style={{ fontSize: '0.8rem', color: '#6b7280', marginBottom: '0.75rem' }}>
        Indica cómo se complementan los documentos para que el asistente use información de ambos.
      </p>

      <form onSubmit={handleCreate} style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem', display: 'block' }}>
              Documento origen
            </label>
            <select
              value={sourceId}
              onChange={(e) => setSourceId(e.target.value)}
              style={{
                width: '100%',
                padding: '0.5rem',
                borderRadius: '0.5rem',
                border: '1px solid #d1d5db',
                fontSize: '0.9rem',
              }}
            >
              <option value="">Selecciona un documento</option>
              {processedDocs.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  {doc.fileName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem', display: 'block' }}>
              Documento destino
            </label>
            <select
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
              style={{
                width: '100%',
                padding: '0.5rem',
                borderRadius: '0.5rem',
                border: '1px solid #d1d5db',
                fontSize: '0.9rem',
              }}
            >
              <option value="">Selecciona un documento</option>
              {processedDocs.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  {doc.fileName}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '0.8rem', fontWeight: 600, color: '#374151', marginBottom: '0.25rem', display: 'block' }}>
              Descripción de la relación
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Ej: Complementa con ejemplos prácticos"
              style={{
                width: '100%',
                padding: '0.5rem',
                borderRadius: '0.5rem',
                border: '1px solid #d1d5db',
                fontSize: '0.9rem',
              }}
            />
          </div>
          <button
            type="submit"
            disabled={!sourceId || !targetId || sourceId === targetId}
            style={{
              padding: '0.5rem',
              border: 'none',
              background: '#2563eb',
              color: 'white',
              borderRadius: '0.5rem',
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.9rem',
              opacity: (!sourceId || !targetId || sourceId === targetId) ? 0.5 : 1,
            }}
          >
            Crear relación
          </button>
        </div>
      </form>

      {relations.length === 0 ? (
        <div className="empty-state">
          <p>No hay relaciones creadas.</p>
        </div>
      ) : (
        <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
          {relations.map((relation) => (
            <li
              key={relation.id}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '0.75rem',
                background: '#f9fafb',
                borderRadius: '0.5rem',
                border: '1px solid #e5e7eb',
              }}
            >
              <div>
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>
                  {relation.sourceDocumentName} ? {relation.targetDocumentName}
                </div>
                {relation.description && (
                  <div style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: '0.2rem' }}>
                    {relation.description}
                  </div>
                )}
              </div>
              <button
                onClick={() => handleDelete(relation.id)}
                style={{
                  padding: '0.3rem 0.6rem',
                  border: 'none',
                  background: '#ef4444',
                  color: 'white',
                  borderRadius: '0.4rem',
                  cursor: 'pointer',
                  fontSize: '0.8rem',
                }}
              >
                Eliminar
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
