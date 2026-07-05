"use client"

import { zodResolver } from "@hookform/resolvers/zod"
import axios from "axios"
import { Shield } from "lucide-react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { getApiErrorMessage } from "@/lib/api-client"
import { useChangePassword } from "@/hooks/use-user"
import { changePasswordSchema } from "@/lib/schemas/user.schema"
import type { ChangePasswordFormValues } from "@/lib/schemas/user.schema"

const defaultValues: ChangePasswordFormValues = {
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
}

export function PasswordCard() {
  const { changePassword, isLoading } = useChangePassword()

  const form = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    mode: "onSubmit",
    reValidateMode: "onBlur",
    defaultValues,
  })

  const handleSubmit = form.handleSubmit(async (values) => {
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      })
      form.reset(defaultValues)
      toast.success("Contrasena actualizada correctamente")
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 401) {
        form.setError("currentPassword", { message: "La contrasena actual no es correcta" })
        return
      }
      toast.error(getApiErrorMessage(err, "No fue posible cambiar tu contrasena"))
    }
  })

  return (
    <div className="rounded-xl bg-card border border-border p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-destructive/10">
          <Shield className="h-5 w-5 text-destructive" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-foreground">Seguridad</h2>
          <p className="text-sm text-muted-foreground">Protege tu cuenta</p>
        </div>
      </div>

      <Form {...form}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <FormField
            control={form.control}
            name="currentPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Contrasena actual</FormLabel>
                <FormControl>
                  <Input type="password" placeholder="********" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="newPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Nueva contrasena</FormLabel>
                <FormControl>
                  <Input type="password" placeholder="********" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="confirmPassword"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Confirmar nueva contrasena</FormLabel>
                <FormControl>
                  <Input type="password" placeholder="********" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <Button type="submit" variant="outline" disabled={isLoading}>
            {isLoading ? "Cambiando..." : "Cambiar contrasena"}
          </Button>
        </form>
      </Form>
    </div>
  )
}
