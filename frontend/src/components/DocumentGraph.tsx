import { useEffect, useState } from 'react';
import { documentsApi, DocumentRelation } from '../api/documentsApi';
import { useApp } from '../context/AppContext';

interface SimpleDocument {
  id: number;
  fileName: string;
  status: string;
}

interface Node {
  id: number;
  name: string;
  x: number;
  y: number;
}

interface Edge {
  id: number;
  source: number;
  target: number;
  description: string;
}

export function DocumentGraph() {
  const [documents, setDocuments] = useState<SimpleDocument[]>([]);
  const [relations, setRelations] = useState<DocumentRelation[]>([]);
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });
  const [selectedSource, setSelectedSource] = useState<number | null>(null);
  const [hoveredNode, setHoveredNode] = useState<number | null>(null);
  const { t } = useApp();

  useEffect(() => {
    loadData();
    const onResize = () => {
      const w = Math.min(window.innerWidth - 100, 800);
      setDimensions({ width: w, height: 420 });
    };
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const loadData = async () => {
    try {
      const [docs, rels] = await Promise.all([
        documentsApi.list(),
        documentsApi.listRelations(),
      ]);
      setDocuments(docs);
      setRelations(rels);
    } catch {
      setDocuments([]);
      setRelations([]);
    }
  };

  const handleNodeClick = async (nodeId: number) => {
    if (selectedSource === null) {
      setSelectedSource(nodeId);
    } else if (selectedSource !== nodeId) {
      const description = window.prompt('Descripcion de la relacion (opcional):') || '';
      try {
        await documentsApi.createRelation(selectedSource, nodeId, description);
        await loadData();
        setSelectedSource(null);
      } catch {
        window.alert('Error al crear la relacion');
      }
    } else {
      setSelectedSource(null);
    }
  };

  const processedDocs = documents.filter((d) => d.status === 'PROCESSED');
  const nodes: Node[] = processedDocs.map((doc, index) => {
    const total = processedDocs.length;
    const centerX = dimensions.width / 2;
    const centerY = dimensions.height / 2;
    const radius = Math.min(dimensions.width, dimensions.height) * 0.32;
    const angle = total === 1 ? 0 : (2 * Math.PI * index) / total - Math.PI / 2;
    return {
      id: doc.id,
      name: doc.fileName,
      x: centerX + radius * Math.cos(angle),
      y: centerY + radius * Math.sin(angle),
    };
  });

  const edges: Edge[] = relations.map((rel) => ({
    id: rel.id,
    source: rel.sourceDocumentId,
    target: rel.targetDocumentId,
    description: rel.description,
  }));

  const getNodeById = (id: number) => nodes.find((n) => n.id === id);

  return (
    <div className="document-graph">
      <h3>
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
        </svg>
        {t('graphTitle')}
      </h3>
      <p style={{ fontSize: '0.8rem', color: '#6b7280', marginBottom: '0.75rem' }}>
        {t('graphDescription')}
      </p>

      {selectedSource && (
        <div style={{
          background: '#2563eb',
          color: 'white',
          padding: '0.5rem 1rem',
          borderRadius: '0.5rem',
          fontSize: '0.85rem',
          marginBottom: '0.75rem',
          textAlign: 'center',
        }}>
          Haz clic en el libro destino para crear la relacion
        </div>
      )}

      <div style={{
        background: '#f8fafc',
        borderRadius: '0.75rem',
        border: '1px solid #e5e7eb',
        padding: '1rem',
        marginBottom: '1rem',
        overflow: 'hidden',
        position: 'relative',
      }}>
        <svg
          width="100%"
          height={dimensions.height}
          viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
          style={{ display: 'block' }}
        >
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="8" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#2563eb" />
            </marker>
          </defs>

          {edges.map((edge) => {
            const sourceNode = getNodeById(edge.source);
            const targetNode = getNodeById(edge.target);
            if (!sourceNode || !targetNode) return null;

            const dx = targetNode.x - sourceNode.x;
            const dy = targetNode.y - sourceNode.y;
            const length = Math.sqrt(dx * dx + dy * dy) || 1;
            const unitX = dx / length;
            const unitY = dy / length;

            const startX = sourceNode.x + 22 * unitX;
            const startY = sourceNode.y + 22 * unitY;
            const endX = targetNode.x - 28 * unitX;
            const endY = targetNode.y - 28 * unitY;

            const midX = (startX + endX) / 2;
            const midY = (startY + endY) / 2;

            return (
              <g key={edge.id}>
                <line
                  x1={startX}
                  y1={startY}
                  x2={endX}
                  y2={endY}
                  stroke="#2563eb"
                  strokeWidth="2.5"
                  markerEnd="url(#arrowhead)"
                />
                <foreignObject x={midX - 50} y={midY - 12} width="100" height="24">
                  <div style={{
                    fontSize: '0.7rem',
                    color: '#4b5563',
                    background: 'white',
                    padding: '2px 6px',
                    borderRadius: '4px',
                    border: '1px solid #e5e7eb',
                    textAlign: 'center',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}>
                    {edge.description || 'Sin descripcion'}
                  </div>
                </foreignObject>
              </g>
            );
          })}

          {nodes.map((node) => {
            const isSelected = selectedSource === node.id;
            return (
              <g
                key={node.id}
                onClick={() => handleNodeClick(node.id)}
                onMouseEnter={() => setHoveredNode(node.id)}
                onMouseLeave={() => setHoveredNode(null)}
                style={{ cursor: 'pointer' }}
              >
                <g transform={`translate(${node.x}, ${node.y})`}>
                  {isSelected && (
                    <circle cx="0" cy="0" r="30" fill="none" stroke="#2563eb" strokeWidth="2" strokeDasharray="4 2" />
                  )}
                  <g transform="scale(1.6)" style={{ color: isSelected ? '#2563eb' : '#3b82f6' }}>
                    <path
                      d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"
                      stroke="currentColor"
                      strokeWidth="1.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      fill="none"
                    />
                    <path
                      d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"
                      stroke="currentColor"
                      strokeWidth="1.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      fill="currentColor"
                      fillOpacity="0.15"
                    />
                    <line x1="8" y1="6" x2="17" y2="6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                    <line x1="8" y1="10" x2="17" y2="10" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                    <line x1="8" y1="14" x2="14" y2="14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                  </g>
                </g>

                {hoveredNode === node.id && (
                  <g>
                    <rect
                      x={node.x - 70}
                      y={node.y + 32}
                      width="140"
                      height="24"
                      rx="4"
                      fill="#1f2937"
                    />
                    <text
                      x={node.x}
                      y={node.y + 48}
                      textAnchor="middle"
                      fill="white"
                      fontSize="11"
                      fontWeight="500"
                    >
                      {node.name.length > 22 ? node.name.slice(0, 20) + '..' : node.name}
                    </text>
                  </g>
                )}
              </g>
            );
          })}
        </svg>
      </div>

      {processedDocs.length === 0 && (
        <div className="empty-state">
          <p>{t('noDocuments')}</p>
        </div>
      )}
    </div>
  );
}

