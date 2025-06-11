import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

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

export interface ProtocolStatus {
  name: string;
  enabled: boolean;
  reason: string;
}

export interface ProtocolsResponse {
  protocols: ProtocolStatus[];
  timestamp: string;
}

export const healthApi = createApi({
  reducerPath: 'healthApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['Health', 'Protocols'],
  endpoints: (builder) => ({
    getSystemHealth: builder.query<SystemHealth, void>({
      query: () => '/health/status',
      providesTags: ['Health'],
      // Cache the result for a short time (30 seconds)
      keepUnusedDataFor: 30,
    }),
    getProtocolStatus: builder.query<ProtocolsResponse, void>({
      query: () => '/health/protocols',
      providesTags: ['Protocols'],
      // Cache for longer since protocols don't change often
      keepUnusedDataFor: 300, // 5 minutes
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

export const { useGetSystemHealthQuery, useGetProtocolStatusQuery } = healthApi; 