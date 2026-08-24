import axios from 'axios';
import { ChatResponse } from '../types/chat.types';

const api = axios.create({
  baseURL: '/api',
  timeout: 120000,
});

export const chatApi = {
  ask: async (question: string, language?: string, conversationId?: string): Promise<ChatResponse> => {
    try {
      const response = await api.post('/chat', { question, language, conversationId });
      return response.data;
    } catch (error: any) {
      if (axios.isAxiosError(error)) {
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          throw new Error('La solicitud tardo demasiado. El modelo puede estar ocupado o el servidor no responde.');
        }
        const message = error.response?.data?.message || 'Error al conectar con el servidor.';
        throw new Error(message);
      }
      throw error;
    }
  },
};
