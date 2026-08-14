import { useState } from 'react';
import { ChatMessage } from './ChatMessage';
import { ChatSource } from '../types/chat.types';
import { LoadingIndicator } from './LoadingIndicator';
import { useApp } from '../context/AppContext';

export interface ChatWindowProps {
  messages: { role: 'user' | 'assistant'; text: string; sources?: ChatSource[] }[];
  loading: boolean;
  onSend: (question: string) => void;
  disabled?: boolean;
}

export function ChatWindow({ messages, loading, onSend, disabled }: ChatWindowProps) {
  const [input, setInput] = useState('');
  const { t } = useApp();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const question = input.trim();
    if (!question || loading) return;
    onSend(question);
    setInput('');
  };

  return (
    <div className="chat-window">
      <h3>
        <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
        </svg>
        {t('chatTitle')}
      </h3>
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="empty-state">
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p>{t('chatPlaceholder')}</p>
          </div>
        )}
        {messages.map((msg, idx) => (
          <ChatMessage key={idx} role={msg.role} text={msg.text} sources={msg.sources} />
        ))}
        {loading && <LoadingIndicator />}
      </div>
      <form onSubmit={handleSubmit} className="chat-form">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={disabled ? t('chatDisabled') : t('chatPlaceholder')}
          disabled={disabled || loading}
        />
        <button type="submit" disabled={disabled || loading || !input.trim()}>
          <svg className="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
          </svg>
          {t('send')}
        </button>
      </form>
    </div>
  );
}
