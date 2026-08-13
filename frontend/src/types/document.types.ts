export interface Documento {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  status: string;
  totalChunks: number;
  createdAt: string;
  updatedAt: string;
}
