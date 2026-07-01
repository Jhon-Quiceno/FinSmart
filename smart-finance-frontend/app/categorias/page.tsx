"use client"

import { useState } from "react"
import { Plus } from "lucide-react"
import { toast } from "sonner"
import { CategoryModal } from "@/components/categories/category-modal"
import { CategoriesTable } from "@/components/categories/categories-table"
import { AppLayout } from "@/components/layout/app-layout"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from "@/hooks/use-categories"
import { getApiErrorMessage } from "@/lib/api-client"
import type { CategoryFormValues } from "@/lib/schemas/category.schema"
import type { Category, CategoryRequest, CategoryType } from "@/lib/types/category"

export default function CategoriasPage() {
  const [activeType, setActiveType] = useState<CategoryType>("EXPENSE")
  const [modalOpen, setModalOpen] = useState(false)
  const [editingCategory, setEditingCategory] = useState<Category | null>(null)
  const [deletingCategory, setDeletingCategory] = useState<Category | null>(null)

  const { categories, isLoading } = useCategories(activeType)
  const { createCategory, isLoading: isCreating } = useCreateCategory()
  const { updateCategory, isLoading: isUpdating } = useUpdateCategory()
  const { deleteCategory, isLoading: isDeleting } = useDeleteCategory()

  const handleSubmit = async (values: CategoryFormValues) => {
    const payload: CategoryRequest = {
      name: values.name,
      type: values.type,
    }

    try {
      if (editingCategory) {
        await updateCategory(editingCategory.id, payload)
        toast.success("Categoria actualizada correctamente")
      } else {
        await createCategory(payload)
        toast.success("Categoria creada correctamente")
      }

      setModalOpen(false)
      setEditingCategory(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible guardar la categoria"))
    }
  }

  const handleDelete = async () => {
    if (!deletingCategory) return

    try {
      await deleteCategory(deletingCategory.id)
      toast.success("Categoria eliminada correctamente")
      setDeletingCategory(null)
    } catch (error) {
      toast.error(getApiErrorMessage(error, "No fue posible eliminar la categoria"))
    }
  }

  return (
    <AppLayout>
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Categorias</h1>
            <p className="text-sm text-muted-foreground">
              Administra las categorias que usas para clasificar tus ingresos y gastos
            </p>
          </div>
          <Button
            onClick={() => {
              setEditingCategory(null)
              setModalOpen(true)
            }}
          >
            <Plus data-icon="inline-start" />
            Agregar categoria
          </Button>
        </div>

        <Tabs value={activeType} onValueChange={(value) => setActiveType(value as CategoryType)}>
          <TabsList>
            <TabsTrigger value="INCOME">Ingresos</TabsTrigger>
            <TabsTrigger value="EXPENSE">Gastos</TabsTrigger>
          </TabsList>

          <TabsContent value="INCOME">
            <CategoriesTable
              categories={activeType === "INCOME" ? categories : []}
              isLoading={activeType === "INCOME" && isLoading}
              onEdit={(category) => {
                setEditingCategory(category)
                setModalOpen(true)
              }}
              onDelete={(category) => setDeletingCategory(category)}
            />
          </TabsContent>

          <TabsContent value="EXPENSE">
            <CategoriesTable
              categories={activeType === "EXPENSE" ? categories : []}
              isLoading={activeType === "EXPENSE" && isLoading}
              onEdit={(category) => {
                setEditingCategory(category)
                setModalOpen(true)
              }}
              onDelete={(category) => setDeletingCategory(category)}
            />
          </TabsContent>
        </Tabs>
      </div>

      <CategoryModal
        open={modalOpen}
        onOpenChange={(nextOpen) => {
          setModalOpen(nextOpen)
          if (!nextOpen) setEditingCategory(null)
        }}
        initialValue={editingCategory}
        defaultType={activeType}
        onSubmit={handleSubmit}
        isSubmitting={isCreating || isUpdating}
      />

      <AlertDialog open={!!deletingCategory} onOpenChange={(open) => !open && setDeletingCategory(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Eliminar categoria</AlertDialogTitle>
            <AlertDialogDescription>
              Esta accion no se puede deshacer. Si eliminas &quot;{deletingCategory?.name}&quot;, los ingresos o
              gastos que la tengan asignada quedaran marcados como &quot;Sin categoria&quot;, pero esos movimientos
              no se eliminaran.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} disabled={isDeleting}>
              {isDeleting ? "Eliminando..." : "Eliminar"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </AppLayout>
  )
}
