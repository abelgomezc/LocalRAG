import { useEffect, useRef, useState } from 'react';
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

export function DocumentGraph({ documents }: { documents: SimpleDocument[] }) {
  const [relations, setRelations] = useState<DocumentRelation[]>([]);
  const svgRef = useRef<SVGSVGElement>(null);
  const [dimensions, setDimensions] = useState({ width: 600, height: 400 });
  const { t } = useApp();

  useEffect(() => {
    loadRelations();
    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  const updateDimensions = () => {
    if (svgRef.current) {
      const rect = svgRef.current.getBoundingClientRect();
      setDimensions({ width: rect.width || 600, height: 400 });
    }
  };

  const loadRelations = async () => {
    try {
      const data = await documentsApi.listRelations();
      setRelations(data);
    } catch {
      setRelations([]);
    }
  };

  const processedDocs = documents.filter((d) => d.status === 'PROCESSED');
  const nodes: Node[] = processedDocs.map((doc, index) => {
    const total = processedDocs.length;
    const centerX = dimensions.width / 2;
    const centerY = dimensions.height / 2;
    const radius = Math.min(dimensions.width, dimensions.height) * 0.35;
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

      <div style={{
        background: '#f8fafc',
        borderRadius: '0.75rem',
        border: '1px solid #e5e7eb',
        padding: '1rem',
        marginBottom: '1rem',
        overflow: 'hidden',
      }}>
        <svg
          ref={svgRef}
          width="100%"
          height={dimensions.height}
          viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
          style={{ display: 'block' }}
        >
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#2563eb" />
            </marker>
          </defs>

          {edges.map((edge) => {
            const sourceNode = getNodeById(edge.source);
            const targetNode = getNodeById(edge.target);
            if (!sourceNode || !targetNode) return null;

            const dx = targetNode.x - sourceNode.x;
            const dy = targetNode.y - sourceNode.y;
            const length = Math.sqrt(dx * dx + dy * dy);
            const unitX = dx / length;
            const unitY = dy / length;

            const startX = sourceNode.x + 25 * unitX;
            const startY = sourceNode.y + 25 * unitY;
            const endX = targetNode.x - 30 * unitX;
            const endY = targetNode.y - 30 * unitY;

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
                  strokeWidth="2"
                  markerEnd="url(#arrowhead)"
                />
                <foreignObject x={midX - 40} y={midY - 12} width="80" height="24">
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

          {nodes.map((node) => (
            <g key={node.id}>
              <circle
                cx={node.x}
                cy={node.y}
                r="24"
                fill="#2563eb"
                stroke="white"
                strokeWidth="3"
                style={{ filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.15))' }}
              />
              <text
                x={node.x}
                y={node.y + 1}
                textAnchor="middle"
                dominantBaseline="middle"
                fill="white"
                fontSize="12"
                fontWeight="bold"
              >
                {node.name.length > 8 ? node.name.slice(0, 6) + '..' : node.name}
              </text>
              <text
                x={node.x}
                y={node.y + 40}
                textAnchor="middle"
                fill="#374151"
                fontSize="11"
                fontWeight="500"
              >
                {node.name.length > 18 ? node.name.slice(0, 16) + '..' : node.name}
              </text>
            </g>
          ))}
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
