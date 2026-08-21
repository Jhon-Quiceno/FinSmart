import { formatCurrency, formatDate, formatPercentage } from './format';

describe('formatCurrency', () => {
  it('formatea con separador de miles, 2 decimales y prefijo $ (es-MX)', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50');
  });

  it('rellena con ceros cuando el valor es un entero', () => {
    expect(formatCurrency(100)).toBe('$100.00');
  });

  it('formatea cero correctamente', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });
});

describe('formatDate', () => {
  it('formatea una fecha ISO como dia/mes-corto/anio en es-MX', () => {
    // Mediodia UTC para evitar que el corrimiento de zona horaria local cambie el dia.
    expect(formatDate('2026-08-20T12:00:00.000Z')).toBe('20 ago 2026');
  });
});

describe('formatPercentage', () => {
  it('redondea al entero mas cercano', () => {
    expect(formatPercentage(0.1234)).toBe('12%');
  });

  it('redondea 0.005 hacia arriba (caso limite)', () => {
    expect(formatPercentage(0.005)).toBe('1%');
  });
});
