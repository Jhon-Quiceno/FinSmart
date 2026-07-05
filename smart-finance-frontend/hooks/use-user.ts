import { useCallback, useState } from "react"
import { changePassword, updateProfile } from "@/lib/services/user.service"
import type { ChangePasswordRequest, UpdateProfileRequest } from "@/lib/services/user.service"

export function useUpdateProfile() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: UpdateProfileRequest) => {
    setIsLoading(true)
    try {
      return await updateProfile(payload)
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { updateProfile: mutate, isLoading }
}

export function useChangePassword() {
  const [isLoading, setIsLoading] = useState(false)

  const mutate = useCallback(async (payload: ChangePasswordRequest) => {
    setIsLoading(true)
    try {
      await changePassword(payload)
    } finally {
      setIsLoading(false)
    }
  }, [])

  return { changePassword: mutate, isLoading }
}
