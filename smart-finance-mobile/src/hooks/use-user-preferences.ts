import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"

import { getUserPreferences, updateUserPreferences } from "@/lib/services/user.service"
import type { UserPreferences } from "@/lib/types/preferences"

export const USER_PREFERENCES_QUERY_KEY = "user-preferences" as const

export function useUserPreferences() {
  return useQuery({
    queryKey: [USER_PREFERENCES_QUERY_KEY],
    queryFn: getUserPreferences,
  })
}

export function useUpdateUserPreferences() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: UserPreferences) => updateUserPreferences(payload),
    onSuccess: (data) => {
      queryClient.setQueryData([USER_PREFERENCES_QUERY_KEY], data)
    },
  })
}
