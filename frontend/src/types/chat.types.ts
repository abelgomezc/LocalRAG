export interface ChatRequest {
  question: string;
  language?: string;
}

export interface ChatSource {
  fileName: string;
  pageNumber?: number;
  chunkNumber?: number;
}

export interface ChatResponse {
  answer: string;
  sources: ChatSource[];
}
