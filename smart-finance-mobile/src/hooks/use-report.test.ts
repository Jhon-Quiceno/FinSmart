import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react-native';
import type { ReactNode } from 'react';
import React from 'react';

import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';

import { exportReportBytes, getMonthlyReport, getReportMovements } from '@/lib/services/report.service';
import type { MonthlyReport, ReportMovementRow } from '@/lib/types/report';

import { arrayBufferToBase64, useExportReportCsv, useMonthlyReport, useReportMovements } from './use-report';

jest.mock('@/lib/services/report.service', () => ({
  getMonthlyReport: jest.fn(),
  getReportMovements: jest.fn(),
  exportReportBytes: jest.fn(),
}));

jest.mock('expo-file-system/legacy', () => ({
  cacheDirectory: 'file:///cache/',
  writeAsStringAsync: jest.fn(),
  EncodingType: { Base64: 'base64' },
}));

jest.mock('expo-sharing', () => ({
  isAvailableAsync: jest.fn(),
  shareAsync: jest.fn(),
}));

const mockedGetMonthlyReport = getMonthlyReport as jest.MockedFunction<typeof getMonthlyReport>;
const mockedGetReportMovements = getReportMovements as jest.MockedFunction<typeof getReportMovements>;
const mockedExportReportBytes = exportReportBytes as jest.MockedFunction<typeof exportReportBytes>;
const mockedWriteAsStringAsync = FileSystem.writeAsStringAsync as jest.Mock;
const mockedIsAvailableAsync = Sharing.isAvailableAsync as jest.Mock;
const mockedShareAsync = Sharing.shareAsync as jest.Mock;

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children);
  }
  return Wrapper;
}

describe('arrayBufferToBase64', () => {
  it('codifica bytes conocidos igual que btoa', () => {
    const bytes = new TextEncoder().encode('Man');
    expect(arrayBufferToBase64(bytes.buffer)).toBe('TWFu');
  });

  it('agrega el padding correcto para tamaños no múltiplo de 3', () => {
    const bytes = new TextEncoder().encode('Ma');
    expect(arrayBufferToBase64(bytes.buffer)).toBe('TWE=');
  });
});

describe('useMonthlyReport + useReportMovements', () => {
  beforeEach(() => {
    mockedGetMonthlyReport.mockReset();
    mockedGetReportMovements.mockReset();
  });

  it('mapea year/month a los filtros del servicio', async () => {
    const report: MonthlyReport = {
      periodYear: 2026,
      periodMonth: 8,
      totalIncome: 100,
      totalExpense: 50,
      savings: 50,
      expenseRatio: 0.5,
      debtRatio: 0.1,
      topCategories: [],
      monthlySeries: [],
    };
    mockedGetMonthlyReport.mockResolvedValue(report);
    mockedGetReportMovements.mockResolvedValue([] as ReportMovementRow[]);

    const wrapper = createWrapper();
    const { result } = await renderHook(() => useMonthlyReport(2026, 8), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockedGetMonthlyReport).toHaveBeenCalledWith({ year: 2026, month: 8 });
    expect(result.current.data?.totalIncome).toBe(100);

    const { result: movementsResult } = await renderHook(() => useReportMovements(2026, 8), { wrapper });
    await waitFor(() => expect(movementsResult.current.isSuccess).toBe(true));
    expect(mockedGetReportMovements).toHaveBeenCalledWith({ year: 2026, month: 8 });
  });
});

describe('useExportReportCsv', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedIsAvailableAsync.mockResolvedValue(true);
  });

  it('escribe el CSV en base64 en cache y comparte el archivo', async () => {
    const bytes = new TextEncoder().encode('Fecha,Tipo\n');
    mockedExportReportBytes.mockResolvedValue({ data: bytes.buffer, filename: 'korofin-2026-08.csv' });

    const wrapper = createWrapper();
    const { result } = await renderHook(() => useExportReportCsv(), { wrapper });

    await result.current.mutateAsync({ year: 2026, month: 8 });

    expect(mockedExportReportBytes).toHaveBeenCalledWith(2026, 8, 'csv');
    expect(mockedWriteAsStringAsync).toHaveBeenCalledWith(
      'file:///cache/korofin-2026-08.csv',
      expect.any(String),
      { encoding: 'base64' },
    );
    expect(mockedShareAsync).toHaveBeenCalledWith('file:///cache/korofin-2026-08.csv');
  });

  it('lanza un error especifico cuando compartir no esta disponible en la plataforma', async () => {
    mockedIsAvailableAsync.mockResolvedValue(false);
    const bytes = new TextEncoder().encode('Fecha,Tipo\n');
    mockedExportReportBytes.mockResolvedValue({ data: bytes.buffer, filename: 'korofin-2026-08.csv' });

    const wrapper = createWrapper();
    const { result } = await renderHook(() => useExportReportCsv(), { wrapper });

    await expect(result.current.mutateAsync({ year: 2026, month: 8 })).rejects.toThrow('SHARING_UNAVAILABLE');
    expect(mockedShareAsync).not.toHaveBeenCalled();
  });
});
