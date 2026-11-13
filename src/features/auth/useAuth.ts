import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/auth';
import { User, Role, LoginRequest } from '@/api/types';

export const AUTH_QUERY_KEY = ['auth', 'me'];

export function useAuth() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const {
    data: user,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<User | null>({
    queryKey: AUTH_QUERY_KEY,
    queryFn: async () => {
      try {
        return await authApi.getMe();
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: false,
  });

  const loginMutation = useMutation({
    mutationFn: async (credentials: LoginRequest) => {
      const res = await authApi.login(credentials);
      return res.user;
    },
    onSuccess: (loggedUser) => {
      queryClient.setQueryData(AUTH_QUERY_KEY, loggedUser);
      navigate('/');
    },
  });

  const logoutMutation = useMutation({
    mutationFn: async () => {
      await authApi.logout();
    },
    onSuccess: () => {
      queryClient.setQueryData(AUTH_QUERY_KEY, null);
      navigate('/login');
    },
  });

  const hasRole = (requiredRoles: Role | Role[]): boolean => {
    if (!user) return false;
    const roles = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
    return roles.includes(user.role);
  };

  return {
    user,
    isAuthenticated: Boolean(user),
    isLoading,
    isError,
    error,
    login: loginMutation.mutateAsync,
    isLoggingIn: loginMutation.isPending,
    loginError: loginMutation.error,
    logout: logoutMutation.mutateAsync,
    isLoggingOut: logoutMutation.isPending,
    hasRole,
    refetch,
  };
}

