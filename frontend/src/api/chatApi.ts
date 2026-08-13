import axios from 'axios';
import { ChatResponse } from '../types/chat.types';

const api = axios.create({
  baseURL: '/api',
});

export const chatApi = {
  ask: async (question: string): Promise<ChatResponse> => {
    const response = await api.post('/chat', { question });
    return response.data;
  },
};
