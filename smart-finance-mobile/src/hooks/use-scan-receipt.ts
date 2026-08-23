import { useMutation } from "@tanstack/react-query"

import { scanReceipt } from "@/lib/services/receipt.service"

export function useScanReceipt() {
  return useMutation({
    mutationFn: (imageDataUri: string) => scanReceipt(imageDataUri),
  })
}
