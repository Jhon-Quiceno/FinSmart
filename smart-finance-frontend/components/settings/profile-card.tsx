"use client"

import { useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import axios from "axios"
import { Save, User } from "lucide-react"
import { useForm } from "react-hook-form"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/contexts/auth-context"
import { getApiErrorMessage } from "@/lib/api-client"
import { useUpdateProfile } from "@/hooks/use-user"
import { profileSchema } from "@/lib/schemas/user.schema"
import type { ProfileFormValues } from "@/lib/schemas/user.schema"

export function ProfileCard() {
  const { user, updateUser } = useAuth()
  const { updateProfile, isLoading } = useUpdateProfile()

  const form = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    mode: "onSubmit",
    reValidateMode: "onBlur",
    defaultValues: { name: user?.name ?? "", email: user?.email ?? "" },
  })

  useEffect(() => {
    if (user) {
      form.reset({ name: user.name, email: user.email })
    }
  }, [form, user])

  const handleSubmit = form.handleSubmit(async (values) => {
    try {
      const response = await updateProfile(values)
      updateUser({ name: response.name, email: response.email })
      toast.success("Perfil actualizado correctamente")
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        form.setError("email", { message: "Ya existe una cuenta con ese correo" })
        return
      }
      toast.error(getApiErrorMessage(err, "No fue posible actualizar tu perfil"))
    }
  })

  return (
    <div className="rounded-xl bg-card border border-border p-6">
      <div className="flex items-center gap-3 mb-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
          <User className="h-5 w-5 text-primary" />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-foreground">Perfil</h2>
          <p className="text-sm text-muted-foreground">Informacion personal</p>
        </div>
      </div>

      <Form {...form}>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre completo</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Correo electronico</FormLabel>
                  <FormControl>
                    <Input type="email" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <Button type="submit" disabled={isLoading}>
            <Save className="h-4 w-4 mr-2" />
            {isLoading ? "Guardando..." : "Guardar cambios"}
          </Button>
        </form>
      </Form>
    </div>
  )
}
