import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
});

export const logsApi = {
  getLogs: async (): Promise<string[]> => {
    const response = await api.get('/logs');
    return response.data;
  },
};
