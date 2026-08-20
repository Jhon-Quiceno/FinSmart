// Datos mock para Reportes — reemplazar por GET /api/reports/monthly (TODO Fase 0 backend).
// Forma replica MonthlyReport de smart-finance-frontend/lib/types/report.ts.

export const mockMonthlyReport = {
  periodYear: 2026,
  periodMonth: 8,
  totalIncome: 4_800_000,
  totalExpense: 3_120_450,
  savings: 1_679_550,
  expenseRatio: 0.65,
  debtRatio: 0.51,
  topCategories: [
    { categoryName: 'Mercado', totalAmount: 820_000, percentage: 0.26 },
    { categoryName: 'Transporte', totalAmount: 410_000, percentage: 0.13 },
    { categoryName: 'Servicios', totalAmount: 355_000, percentage: 0.11 },
    { categoryName: 'Suscripciones', totalAmount: 149_900, percentage: 0.05 },
  ],
};

export interface MockMovementRow {
  date: string;
  type: 'INCOME' | 'EXPENSE';
  categoryName: string | null;
  description: string | null;
  amount: number;
}

export const mockReportMovements: MockMovementRow[] = [
  { date: '2026-08-19', type: 'EXPENSE', categoryName: 'Mercado', description: 'Supermercado La Rebaja', amount: 185_000 },
  { date: '2026-08-15', type: 'INCOME', categoryName: 'Salario', description: 'Nómina agosto', amount: 4_800_000 },
  { date: '2026-08-14', type: 'EXPENSE', categoryName: 'Transporte', description: 'Uber al trabajo', amount: 62_500 },
];
