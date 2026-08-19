import { useEffect, useState } from 'react';
import { DocumentUpload } from '../components/DocumentUpload';
import { DocumentList } from '../components/DocumentList';
import { DocumentGraph } from '../components/DocumentGraph';
import { ChatWindow } from '../components/ChatWindow';
import { documentsApi } from '../api/documentsApi';
import { chatApi } from '../api/chatApi';
import { ChatSource } from '../types/chat.types';
import { useApp } from '../context/AppContext';

type View = 'chat' | 'documents' | 'relations';

export function HomePage() {
  const { language, setLanguage, theme, setTheme, t } = useApp();
  const [view, setView] = useState<View>('chat');
  const [messages, setMessages] = useState<{ role: 'user' | 'assistant'; text: string; sources?: ChatSource[] }[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasDocuments, setHasDocuments] = useState(false);
  const [documents, setDocuments] = useState<{ id: number; fileName: string; status: string }[]>([]);
  const [health, setHealth] = useState<{ application: string; ollama: string; database: string } | null>(null);

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark' || savedTheme === 'light') {
      setTheme(savedTheme);
    }
  }, []);

  useEffect(() => {
    checkDocuments();
    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
  }, [theme]);

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
      const response = await chatApi.ask(question, language);
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: response.answer, sources: response.sources },
      ]);
    } catch {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: language === 'es'
            ? 'Error al obtener respuesta. Por favor, intenta nuevamente.'
            : 'Error getting response. Please try again.',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const toggleTheme = () => {
    const newTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(newTheme);
    localStorage.setItem('theme', newTheme);
  };

  const handleClearSystem = async () => {
    if (loading) {
      alert(language === 'es' ? 'No se puede limpiar el sistema mientras hay una consulta en curso.' : 'Cannot clear system while a query is in progress.');
      return;
    }
    if (!documents.length) {
      alert(language === 'es' ? 'No hay documentos para eliminar.' : 'No documents to delete.');
      return;
    }
    const confirmMessage = language === 'es'
      ? `¿Estás seguro? Se eliminarán ${documents.length} documento(s) permanentemente.`
      : `Are you sure? ${documents.length} document(s) will be permanently deleted.`;
    if (!window.confirm(confirmMessage)) {
      return;
    }
    try {
      await documentsApi.deleteAll();
      await checkDocuments();
      setMessages([]);
      alert(language === 'es' ? 'Sistema limpiado correctamente.' : 'System cleared successfully.');
    } catch (err: any) {
      alert(err?.message || (language === 'es' ? 'Error al limpiar el sistema' : 'Error clearing system'));
    }
  };

  const getStatusColor = (status: string) => {
    if (status === 'UP') return '#10b981';
    return '#ef4444';
  };

  const navItems: { key: View; label: string }[] = [
    { key: 'chat', label: t('chatTitle') },
    { key: 'documents', label: t('documentsTitle') },
    { key: 'relations', label: t('relationsTitle') },
  ];

  return (
    <div className="home-page">
      <header>
        <h1>{t('appName')}</h1>
        <p>{t('appSubtitle')}</p>
        {health && (
          <div style={{ display: 'flex', justifyContent: 'center', gap: '1rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.application), display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
              <svg className="icon" style={{ width: 14, height: 14 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3a2 2 0 00-2 2v7a2 2 0 002 2h18a2 2 0 002-2v-7a2 2 0 00-2-2h-2M5 9h14a2 2 0 012 2v7a2 2 0 01-2 2H5a2 2 0 01-2-2v-7a2 2 0 012-2z" />
              </svg>
              App: {health.application}
            </span>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.ollama), display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
              <svg className="icon" style={{ width: 14, height: 14 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
              Ollama: {health.ollama}
            </span>
            <span style={{ fontSize: '0.75rem', color: getStatusColor(health.database), display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
              <svg className="icon" style={{ width: 14, height: 14 }} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2 1 3 3 3h10c2 0 3-1 3-3V7c0-2-1-3-3-3H7C5 4 4 5 4 7z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h8M12 8v8" />
              </svg>
              DB: {health.database}
            </span>
          </div>
        )}
      </header>

      <nav>
        {navItems.map((item) => (
          <button
            key={item.key}
            onClick={() => setView(item.key)}
            style={{
              background: view === item.key ? '#2563eb' : 'white',
              color: view === item.key ? 'white' : '#1f2937',
            }}
          >
            {item.label}
          </button>
        ))}
        <button
          className="theme-toggle"
          onClick={toggleTheme}
        >
          {theme === 'light' ? (
            <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.0 0 0012 21a9.003 9.0 0 008.354-5.646z" />
            </svg>
          ) : (
            <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          )}
          {t('themeToggle')}
        </button>
        <button
          className="language-toggle"
          onClick={() => setLanguage(language === 'es' ? 'en' : 'es')}
        >
          <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5h12M9 3v2m1.5 4.5l1.5 1.5M4.5 9.5h15a1.5 1.5 0 011.5 1.5v9a1.5 1.5 0 01-1.5 1.5h-15A1.5 1.5 0 013 20v-9A1.5 1.5 0 014.5 9.5z" />
          </svg>
          {language === 'es' ? 'ES' : 'EN'}
        </button>
        {documents.length > 0 && (
          <button
            onClick={handleClearSystem}
            disabled={loading}
            style={{
              background: loading ? '#9ca3af' : '#ef4444',
              color: 'white',
              border: 'none',
              padding: '0.5rem 1rem',
              borderRadius: '0.5rem',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontSize: '0.85rem',
              fontWeight: 500,
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
            }}
          >
            🗑 {language === 'es' ? 'Limpiar sistema' : 'Clear system'}
          </button>
        )}
      </nav>

      {view === 'chat' && (
        <main className="chat-area">
          <ChatWindow
            messages={messages}
            loading={loading}
            onSend={handleSend}
            disabled={!hasDocuments}
          />
        </main>
      )}

      {view === 'documents' && (
        <div className="main-layout" style={{ gridTemplateColumns: '1fr' }}>
          <DocumentUpload onUploaded={checkDocuments} />
          <DocumentList onDeleted={checkDocuments} />
        </div>
      )}

      {view === 'relations' && (
        <div className="main-layout" style={{ gridTemplateColumns: '1fr' }}>
          <DocumentGraph />
        </div>
      )}

      <footer>
        <p>Abel Gomez. Todos los derechos reservados.</p>
      </footer>
    </div>
  );
}

