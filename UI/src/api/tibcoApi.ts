import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

// Types
export interface TibcoDestination {
  name: string;
  type: 'TOPIC' | 'QUEUE';
}

export interface TibcoStub {
  id: string;
  name: string;
  description?: string;
  userId?: string;
  destinationType?: 'TOPIC' | 'QUEUE';
  destinationName?: string;
  requestDestination?: {
    type: 'TOPIC' | 'QUEUE';
    name: string;
  };
  responseDestination?: {
    type: 'TOPIC' | 'QUEUE';
    name: string;
  };
  tags: string[];
  status: string;
  createdAt: string;
  updatedAt: string;
  matchConditions: Record<string, any>;
  bodyMatchCriteria?: Array<{
    type: 'xpath' | 'jsonpath';
    expression: string;
    value: string;
    operator: 'equals' | 'contains' | 'startsWith' | 'endsWith' | 'regex';
  }>;
  response: Record<string, any>;
  responseHeaders?: Record<string, string>;
  callbackConfig?: Record<string, any>;
}

export interface TibcoSchedule {
  id: string;
  destinationType: 'TOPIC' | 'QUEUE';
  destinationName: string;
  contentType: string;
  scheduledTime: string;
  message: string;
  properties: Record<string, string>;
  createdAt: string;
}

// Request types
export interface CreateDestinationRequest {
  name: string;
  type: 'TOPIC' | 'QUEUE';
}

export interface DeleteDestinationRequest {
  name: string;
  type: string;
}

export interface PublishMessageRequest {
  destinationType: 'TOPIC' | 'QUEUE';
  destinationName: string;
  contentType: string;
  message: string;
  properties?: Record<string, string>;
}

export interface CreateScheduleRequest extends PublishMessageRequest {
  scheduledTime: string;
}

export const tibcoApi = createApi({
  reducerPath: 'tibcoApi',
  baseQuery: fetchBaseQuery({ 
    baseUrl: config.API_URL,
    credentials: 'include'
  }),
  tagTypes: ['TibcoDestination', 'TibcoStub', 'TibcoSchedule'],
  endpoints: (builder) => ({
    // Destinations
    getTibcoDestinations: builder.query<TibcoDestination[], void>({
      query: () => '/tibco/destinations',
      providesTags: ['TibcoDestination']
    }),
    
    createTibcoDestination: builder.mutation<TibcoDestination, CreateDestinationRequest>({
      query: (destination) => ({
        url: '/tibco/destinations',
        method: 'POST',
        body: destination
      }),
      invalidatesTags: ['TibcoDestination']
    }),
    
    deleteTibcoDestination: builder.mutation<void, DeleteDestinationRequest>({
      query: ({ name, type }) => ({
        url: `/tibco/destinations/${type}/${name}`,
        method: 'DELETE'
      }),
      invalidatesTags: ['TibcoDestination']
    }),
    
    // Stubs
    getTibcoStubs: builder.query<TibcoStub[], void>({
      query: () => '/tibco/stubs',
      providesTags: ['TibcoStub']
    }),
    
    getTibcoStubById: builder.query<TibcoStub, string>({
      query: (id) => `/tibco/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'TibcoStub', id }]
    }),
    
    createTibcoStub: builder.mutation<TibcoStub, Partial<TibcoStub>>({
      query: (stub) => ({
        url: '/tibco/stubs',
        method: 'POST',
        body: stub
      }),
      invalidatesTags: ['TibcoStub']
    }),
    
    updateTibcoStub: builder.mutation<TibcoStub, Partial<TibcoStub>>({
      query: (stub) => ({
        url: `/tibco/stubs/${stub.id}`,
        method: 'PUT',
        body: stub
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'TibcoStub', id }]
    }),
    
    deleteTibcoStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/tibco/stubs/${id}`,
        method: 'DELETE'
      }),
      invalidatesTags: ['TibcoStub']
    }),
    
    updateTibcoStubStatus: builder.mutation<TibcoStub, { id: string; status: string }>({
      query: ({ id, status }) => ({
        url: `/tibco/stubs/${id}/status`,
        method: 'PATCH',
        params: { status }
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'TibcoStub', id }]
    }),
    
    // Publishing
    publishTibcoMessage: builder.mutation<void, PublishMessageRequest>({
      query: (message) => ({
        url: '/tibco/publish',
        method: 'POST',
        body: message
      })
    }),
    
    // Scheduling
    getTibcoSchedules: builder.query<TibcoSchedule[], void>({
      query: () => '/tibco/schedules',
      providesTags: ['TibcoSchedule']
    }),
    
    createTibcoSchedule: builder.mutation<TibcoSchedule, CreateScheduleRequest>({
      query: (schedule) => ({
        url: '/tibco/schedules',
        method: 'POST',
        body: schedule
      }),
      invalidatesTags: ['TibcoSchedule']
    }),
    
    deleteTibcoSchedule: builder.mutation<void, string>({
      query: (id) => ({
        url: `/tibco/schedules/${id}`,
        method: 'DELETE'
      }),
      invalidatesTags: ['TibcoSchedule']
    })
  })
});

export const {
  // Destinations
  useGetTibcoDestinationsQuery,
  useCreateTibcoDestinationMutation,
  useDeleteTibcoDestinationMutation,
  
  // Stubs
  useGetTibcoStubsQuery,
  useGetTibcoStubByIdQuery,
  useCreateTibcoStubMutation,
  useUpdateTibcoStubMutation,
  useDeleteTibcoStubMutation,
  useUpdateTibcoStubStatusMutation,
  
  // Publishing
  usePublishTibcoMessageMutation,
  
  // Scheduling
  useGetTibcoSchedulesQuery,
  useCreateTibcoScheduleMutation,
  useDeleteTibcoScheduleMutation
} = tibcoApi; 