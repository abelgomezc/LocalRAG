import { SourceList } from './SourceList';

export function ChatMessage({
  role,
  text,
  sources,
}: {
  role: 'user' | 'assistant';
  text: string;
  sources?: { fileName: string; pageNumber?: number; chunkNumber?: number }[];
}) {
  return (
    <div className={`chat-message ${role}`}>
      <div className="message-text">{text}</div>
      {role === 'assistant' && sources && sources.length > 0 && (
        <SourceList sources={sources} />
      )}
    </div>
  );
}
