import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { renderHook, waitFor } from "@testing-library/react-native"
import type { ReactNode } from "react"
import React from "react"

import type { Expense, ExpenseRequest } from "@/lib/types/expense"
import type { PaginatedResponse } from "@/lib/types/pagination"

jest.mock("@/lib/services/expense.service", () => ({
  getExpenses: jest.fn(),
  createExpense: jest.fn(),
  updateExpense: jest.fn(),
  deleteExpense: jest.fn(),
}))

import { createExpense, getExpenses } from "@/lib/services/expense.service"
import { useCreateExpense, useExpenses } from "./use-expenses"

const mockedGetExpenses = getExpenses as jest.MockedFunction<typeof getExpenses>
const mockedCreateExpense = createExpense as jest.MockedFunction<typeof createExpense>

function emptyPage(): PaginatedResponse<Expense> {
  return { content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 }
}

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  function Wrapper({ children }: { children: ReactNode }) {
    return React.createElement(QueryClientProvider, { client: queryClient }, children)
  }
  return Wrapper
}

describe("useExpenses + useCreateExpense", () => {
  beforeEach(() => {
    mockedGetExpenses.mockReset()
    mockedCreateExpense.mockReset()
  })

  it("invalidates and refetches the expenses query after a successful create", async () => {
    mockedGetExpenses.mockResolvedValue(emptyPage())

    const newExpense: Expense = {
      id: 1,
      amount: 100,
      description: "Cafe",
      date: "2026-08-20",
      paymentMethod: "CASH",
      categoryId: null,
      categoryName: null,
      recurringPaymentId: null,
    }
    mockedCreateExpense.mockResolvedValue(newExpense)

    const wrapper = createWrapper()
    const { result: queryResult } = await renderHook(() => useExpenses({}), { wrapper })

    await waitFor(() => expect(queryResult.current.isSuccess).toBe(true))
    expect(mockedGetExpenses).toHaveBeenCalledTimes(1)

    const { result: mutationResult } = await renderHook(() => useCreateExpense(), { wrapper })

    const payload: ExpenseRequest = {
      amount: 100,
      description: "Cafe",
      date: "2026-08-20",
      paymentMethod: "CASH",
    }

    await mutationResult.current.mutateAsync(payload)

    expect(mockedCreateExpense).toHaveBeenCalledWith(payload)
    await waitFor(() => expect(mockedGetExpenses).toHaveBeenCalledTimes(2))
  })
})
