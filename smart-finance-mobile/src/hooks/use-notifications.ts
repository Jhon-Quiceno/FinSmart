import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  getNotifications,
  getPreferences,
  getUnreadCount,
  markAllAsRead,
  markAsRead,
  updatePreferences,
  type NotificationFilters,
} from '@/lib/services/notification.service';
import type { NotificationPreferenceRequest } from '@/lib/types/notification';

export const notificationsQueryKey = (filters: NotificationFilters = {}) => ['notifications', filters] as const;
export const unreadCountQueryKey = () => ['notifications-unread-count'] as const;
export const notificationPreferencesQueryKey = () => ['notification-preferences'] as const;

export function useNotifications(filters: NotificationFilters = {}) {
  return useQuery({
    queryKey: notificationsQueryKey(filters),
    queryFn: () => getNotifications(filters),
  });
}

export function useUnreadCount() {
  return useQuery({
    queryKey: unreadCountQueryKey(),
    queryFn: () => getUnreadCount(),
  });
}

/**
 * El feed (`useNotifications`) y el contador (`useUnreadCount`) son datos relacionados pero
 * viven en queries separadas — marcar como leída una notificación debe invalidar las dos, si no
 * la campana del header queda mostrando un conteo desactualizado.
 */
export function useMarkAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => markAsRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
      void queryClient.invalidateQueries({ queryKey: unreadCountQueryKey() });
    },
  });
}

export function useMarkAllAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => markAllAsRead(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
      void queryClient.invalidateQueries({ queryKey: unreadCountQueryKey() });
    },
  });
}

export function useNotificationPreferences() {
  return useQuery({
    queryKey: notificationPreferencesQueryKey(),
    queryFn: () => getPreferences(),
  });
}

export function useUpdateNotificationPreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: NotificationPreferenceRequest) => updatePreferences(payload),
    onSuccess: (updated) => {
      queryClient.setQueryData(notificationPreferencesQueryKey(), updated);
    },
  });
}
