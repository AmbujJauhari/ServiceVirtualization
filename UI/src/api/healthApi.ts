import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export type HealthStatus = 'UP' | 'DOWN' | 'DEGRADED';

export interface ServiceHealth {
  name: string;
  status: HealthStatus;
  details?: string;
  lastChecked: string;
}

export interface SystemHealth {
  overall: HealthStatus;
  application: ServiceHealth;
  services: ServiceHealth[];
}

export const healthApi = createApi({
  reducerPath: 'healthApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['Health'],
  endpoints: (builder) => ({
    getSystemHealth: builder.query<SystemHealth, void>({
      query: () => '/health/status',
      providesTags: ['Health'],
      // Cache the result for a short time (30 seconds)
      keepUnusedDataFor: 30,
    }),
  }),
});

// Hook with built-in polling logic
export const useGetSystemHealthWithPolling = (pollingInterval = 30000) => {
  const result = healthApi.useGetSystemHealthQuery(undefined, {
    pollingInterval,
  });
  
  return result;
};

export const { useGetSystemHealthQuery } = healthApi; 