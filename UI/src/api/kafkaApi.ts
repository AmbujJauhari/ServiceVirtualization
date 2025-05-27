import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

// Types
export type ContentFormat = 'JSON' | 'XML' | 'AVRO';

export enum ContentMatchType {
  NONE = 'NONE',
  CONTAINS = 'CONTAINS',
  EXACT = 'EXACT',
  REGEX = 'REGEX'
}

export interface KafkaStub {
  id: string;
  name: string;
  description?: string;
  userId?: string;
  
  // Request matching
  requestTopic: string;
  requestContentFormat: ContentFormat;
  requestContentMatcher?: string; // JSON path, XPath, or Avro schema for matching content
  
  // Key matching (for Kafka message key)
  keyMatchType?: ContentMatchType;
  keyPattern?: string;
  
  // Value/Content matching (for Kafka message value/payload)
  // Note: In Kafka, "value" and "content" refer to the same thing - the message payload
  contentMatchType?: ContentMatchType;
  contentPattern?: string;
  caseSensitive?: boolean;
  
  // Response configuration
  responseTopic?: string;
  responseContentFormat: ContentFormat;
  responseKey?: string; // Key for response messages (auto-generated if not provided)
  
  // Response handling
  responseType: 'direct' | 'callback';
  responseContent?: string;
  latency?: number;
  callbackUrl?: string;
  callbackHeaders?: Record<string, string>;
  
  // Status - simplified approach: only active/inactive
  status: string; // 'active' or 'inactive'
  
  // Metadata
  createdAt: string;
  updatedAt: string;
  tags?: string[];
}

// Request types for publishing
export interface PublishKafkaMessageRequest {
  topic: string;
  key?: string;
  contentType: string;
  message: string;
  headers?: Record<string, string>;
}

export const kafkaApi = createApi({
  reducerPath: 'kafkaApi',
  baseQuery: fetchBaseQuery({ 
    baseUrl: config.API_URL,
    credentials: 'include'
  }),
  tagTypes: ['KafkaStub'],
  endpoints: (builder) => ({
    // Stubs
    getKafkaStubs: builder.query<KafkaStub[], void>({
      query: () => '/kafka/stubs',
      providesTags: ['KafkaStub']
    }),
    
    getKafkaStubById: builder.query<KafkaStub, string>({
      query: (id) => `/kafka/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'KafkaStub', id }]
    }),
    
    createKafkaStub: builder.mutation<KafkaStub, Partial<KafkaStub>>({
      query: (stub) => ({
        url: '/kafka/stubs',
        method: 'POST',
        body: stub
      }),
      invalidatesTags: ['KafkaStub']
    }),
    
    updateKafkaStub: builder.mutation<KafkaStub, Partial<KafkaStub>>({
      query: (stub) => ({
        url: `/kafka/stubs/${stub.id}`,
        method: 'PUT',
        body: stub
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'KafkaStub', id }]
    }),
    
    deleteKafkaStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/kafka/stubs/${id}`,
        method: 'DELETE'
      }),
      invalidatesTags: ['KafkaStub']
    }),
    
    updateKafkaStubStatus: builder.mutation<KafkaStub, { id: string; status: string }>({
      query: ({ id, status }) => ({
        url: `/kafka/stubs/${id}/status`,
        method: 'PATCH',
        params: { status }
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'KafkaStub', id }]
    }),
    
    // Topic-specific stub queries
    getStubsByRequestTopic: builder.query<KafkaStub[], string>({
      query: (topic) => `/kafka/stubs/topic/${topic}/request`,
      providesTags: ['KafkaStub']
    }),
    
    getStubsByResponseTopic: builder.query<KafkaStub[], string>({
      query: (topic) => `/kafka/stubs/topic/${topic}/response`,
      providesTags: ['KafkaStub']
    }),
    
    // Format-specific queries
    getStubsByContentFormat: builder.query<KafkaStub[], ContentFormat>({
      query: (format) => `/kafka/stubs/format/${format}`,
      providesTags: ['KafkaStub']
    }),
    
    // Active stubs by topic (simplified approach)
    getActiveStubsByTopic: builder.query<KafkaStub[], string>({
      query: (topic) => `/kafka/stubs/topic/${topic}/active`,
      providesTags: ['KafkaStub']
    }),
    
    // Webhook callback logs
    getCallbackLogs: builder.query<any[], string>({
      query: (stubId) => `/kafka/stubs/${stubId}/callbacks`,
      providesTags: (result, error, stubId) => [{ type: 'KafkaStub', id: `${stubId}-callbacks` }]
    }),
    
    // Retry a failed callback
    retryCallback: builder.mutation<void, { stubId: string, callbackId: string }>({
      query: ({ stubId, callbackId }) => ({
        url: `/kafka/stubs/${stubId}/callbacks/${callbackId}/retry`,
        method: 'POST'
      }),
      invalidatesTags: (result, error, { stubId }) => [{ type: 'KafkaStub', id: `${stubId}-callbacks` }]
    }),
    
    // Publishing
    publishKafkaMessage: builder.mutation<void, PublishKafkaMessageRequest>({
      query: (message) => ({
        url: '/kafka/publish',
        method: 'POST',
        body: message
      })
    }),
    
    // Get Kafka topics
    getKafkaTopics: builder.query<string[], void>({
      query: () => '/kafka/topics'
    })
  })
});

export const {
  useGetKafkaStubsQuery,
  useGetKafkaStubByIdQuery,
  useCreateKafkaStubMutation,
  useUpdateKafkaStubMutation,
  useDeleteKafkaStubMutation,
  useUpdateKafkaStubStatusMutation,
  useGetStubsByRequestTopicQuery,
  useGetStubsByResponseTopicQuery,
  useGetStubsByContentFormatQuery,
  useGetActiveStubsByTopicQuery,
  useGetCallbackLogsQuery,
  useRetryCallbackMutation,
  usePublishKafkaMessageMutation,
  useGetKafkaTopicsQuery
} = kafkaApi; 