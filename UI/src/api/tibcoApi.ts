import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

// Types
export enum ContentMatchType {
  NONE = 'NONE',
  CONTAINS = 'CONTAINS',
  EXACT = 'EXACT',
  REGEX = 'REGEX'
}

export enum StubStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE'
}

export interface TibcoDestination {
  type: 'TOPIC' | 'QUEUE';
  name: string;
}

export interface TibcoStub {
  id?: string;
  name: string;
  description?: string;
  userId?: string;
  
  // Flat destination structure to match backend
  destinationType?: string; // "QUEUE" or "TOPIC"
  destinationName: string;
  
  // Response destination as flat strings
  responseType?: string; // "QUEUE" or "TOPIC"
  responseDestination?: string;
  
  messageSelector?: string;
  
  // Legacy body match criteria (kept for backward compatibility)
  bodyMatchCriteria?: BodyMatchCriteria[];
  
  // Standardized content matching configuration
  contentMatchType?: ContentMatchType;
  contentPattern?: string;
  caseSensitive?: boolean;
  
  // Priority for stub matching
  priority?: number;
  
  responseContent?: string;
  responseHeaders?: Record<string, string>;
  
  // Response latency in milliseconds
  latency?: number;
  
  // Webhook configuration
  webhookUrl?: string;
  
  status?: StubStatus;
  createdAt?: string;
  updatedAt?: string;
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

export interface BodyMatchCriteria {
  type: 'xpath' | 'jsonpath';
  expression: string;
  value: string;
  operator: 'equals' | 'contains' | 'startsWith' | 'endsWith' | 'regex';
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
  destinationType: string;
  destinationName: string;
  message: string;
  headers?: MessageHeader[];
}

export interface PublishMessageResponse {
  success: boolean;
  message: string;
}

export interface MessageHeader {
  name: string;
  value: string;
  type: string;
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
    publishMessage: builder.mutation<PublishMessageResponse, PublishMessageRequest>({
      query: (messageRequest) => ({
        url: '/tibco/stubs/publish',
        method: 'POST',
        body: messageRequest,
      }),
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
  }),
});

export const {
  useGetTibcoDestinationsQuery,
  useCreateTibcoDestinationMutation,
  useDeleteTibcoDestinationMutation,
  useGetTibcoStubsQuery,
  useGetTibcoStubByIdQuery,
  useCreateTibcoStubMutation,
  useUpdateTibcoStubMutation,
  useDeleteTibcoStubMutation,
  useUpdateTibcoStubStatusMutation,
  usePublishMessageMutation,
  useGetTibcoSchedulesQuery,
  useCreateTibcoScheduleMutation,
  useDeleteTibcoScheduleMutation,
} = tibcoApi; 