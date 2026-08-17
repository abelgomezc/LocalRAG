import axios from 'axios';
import { Documento } from '../types/document.types';

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
});

export interface DocumentRelation {
  id: number;
  sourceDocumentId: number;
  sourceDocumentName: string;
  targetDocumentId: number;
  targetDocumentName: string;
  description: string;
}

export const documentsApi = {
  uploadMultiple: async (files: File[]): Promise<{ id: number; fileName: string; status: string; chunks: number }[]> => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    const response = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  list: async (): Promise<Documento[]> => {
    const response = await api.get('/documents');
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/documents/${id}`);
  },

  deleteAll: async (): Promise<void> => {
    await api.delete('/documents');
  },

  createRelation: async (sourceDocumentId: number, targetDocumentId: number, description: string): Promise<DocumentRelation> => {
    const response = await api.post('/documents/relations', {
      sourceDocumentId,
      targetDocumentId,
      description,
    });
    return response.data;
  },

  listRelations: async (): Promise<DocumentRelation[]> => {
    const response = await api.get('/documents/relations');
    return response.data;
  },

  deleteRelation: async (relationId: number): Promise<void> => {
    await api.delete(`/documents/relations/${relationId}`);
  },
};
