import { useMutation, useQuery } from '@tanstack/react-query';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';

import {
  exportReportBytes,
  getMonthlyReport,
  getReportMovements,
  type MonthlyReportFilters,
} from '@/lib/services/report.service';

export const monthlyReportQueryKey = (filters: MonthlyReportFilters = {}) => ['monthly-report', filters] as const;
export const reportMovementsQueryKey = (filters: MonthlyReportFilters = {}) =>
  ['report-movements', filters] as const;

export function useMonthlyReport(year?: number, month?: number) {
  const filters: MonthlyReportFilters = { year, month };
  return useQuery({
    queryKey: monthlyReportQueryKey(filters),
    queryFn: () => getMonthlyReport(filters),
  });
}

export function useReportMovements(year?: number, month?: number) {
  const filters: MonthlyReportFilters = { year, month };
  return useQuery({
    queryKey: reportMovementsQueryKey(filters),
    queryFn: () => getReportMovements(filters),
  });
}

const BASE64_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/**
 * Codifica un ArrayBuffer a base64 a mano — sin depender de `btoa` (no está garantizado como
 * global en Hermes) ni de una librería nueva solo para esto.
 */
export function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let result = '';
  let i = 0;

  for (; i + 2 < bytes.length; i += 3) {
    result +=
      BASE64_CHARS[bytes[i] >> 2] +
      BASE64_CHARS[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)] +
      BASE64_CHARS[((bytes[i + 1] & 15) << 2) | (bytes[i + 2] >> 6)] +
      BASE64_CHARS[bytes[i + 2] & 63];
  }

  const remaining = bytes.length - i;
  if (remaining === 1) {
    result += BASE64_CHARS[bytes[i] >> 2] + BASE64_CHARS[(bytes[i] & 3) << 4] + '==';
  } else if (remaining === 2) {
    result +=
      BASE64_CHARS[bytes[i] >> 2] +
      BASE64_CHARS[((bytes[i] & 3) << 4) | (bytes[i + 1] >> 4)] +
      BASE64_CHARS[(bytes[i + 1] & 15) << 2] +
      '=';
  }

  return result;
}

export interface ExportReportCsvParams {
  year: number;
  month: number;
}

export class SharingUnavailableError extends Error {
  constructor() {
    super('SHARING_UNAVAILABLE');
    this.name = 'SharingUnavailableError';
  }
}

/**
 * Descarga el CSV del período, lo escribe en el directorio de cache (`expo-file-system`) y abre
 * el hoja de compartir del sistema (`expo-sharing`) para que el usuario lo guarde o lo envíe —
 * no existe un equivalente a la descarga de navegador de la versión web en RN.
 */
export function useExportReportCsv() {
  return useMutation({
    mutationFn: async ({ year, month }: ExportReportCsvParams) => {
      const { data, filename } = await exportReportBytes(year, month, 'csv');
      const base64 = arrayBufferToBase64(data);

      const cacheDirectory = FileSystem.cacheDirectory ?? '';
      const fileUri = `${cacheDirectory}${filename}`;
      await FileSystem.writeAsStringAsync(fileUri, base64, {
        encoding: FileSystem.EncodingType.Base64,
      });

      const isAvailable = await Sharing.isAvailableAsync();
      if (!isAvailable) {
        throw new SharingUnavailableError();
      }

      await Sharing.shareAsync(fileUri);
      return { fileUri, filename };
    },
  });
}
