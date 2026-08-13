import { useEffect, useState } from 'react';
import { DocumentUpload } from '../components/DocumentUpload';
import { DocumentList } from '../components/DocumentList';
import { DocumentRelations } from '../components/DocumentRelations';
import { ChatWindow } from '../components/ChatWindow';
import { documentsApi } from '../api/documentsApi';
import { chatApi } from '../api/chatApi';
import { ChatSource } from '../types/chat.types';

export function HomePage() {
  const [messages, setMessages] = useState<{ role: 'user' | 'assistant'; text: string; sources?: ChatSource[] }[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasDocuments, setHasDocuments] = useState(false);
  const [documents, setDocuments] = useState<{ id: number; fileName: string; status: string }[]>([]);
  const [health, setHealth] = useState<{ application: string; ollama: string; database: string } | null>(null);

  useEffect(() => {
    checkDocuments();
    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const checkHealth = async () => {
    try {
      const response = await fetch('/api/health');
      const data = await response.json();
      setHealth(data);
    } catch {
      setHealth({ application: 'DOWN', ollama: 'DOWN', database: 'DOWN' });
    }
  };

  const checkDocuments = async () => {
    try {
      const docs = await documentsApi.list();
      setDocuments(docs.map((d) => ({ id: d.id, fileName: d.fileName, status: d.status })));
      setHasDocuments(docs.some((d) => d.status === 'PROCESSED'));
    } catch {
      setHasDocuments(false);
      setDocuments([]);
    }
  };

  const handleSend = async (question: string) => {
    setMessages((prev) => [...prev, { role: 'user', text: question }]);
    setLoading(true);

    try {
      const response = await chatApi.ask(question);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: response.answer, sources: response.sources },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: 'Error al obtener respuesta. Por favor, intenta nuevamente.',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    if (status === 'UP') return '#10b981';
    return '#ef4444';
  };

  return (
    <div className="home-page">
      <header>
        <h1>Local RAG Assistant</h1>
        <p>Ask questions about your documents</p>
        {health && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.application) }}>
              ? App: {health.application}
            </span>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.ollama) }}>
              ? Ollama: {health.ollama}
            </span>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.database) }}>
              ? DB: {health.database}
            </span>
          </div>
        )}
      </header>
      <div className="main-layout">
        <aside className="sidebar">
          <DocumentUpload onUploaded={checkDocuments} />
          <DocumentList onDeleted={checkDocuments} />
          <DocumentRelations documents={documents} />
        </aside>
        <main className="chat-area">
          <ChatWindow messages={messages} loading={loading} onSend={handleSend} disabled={!hasDocuments} />
        </main>
      </div>
    </div>
  );
}
