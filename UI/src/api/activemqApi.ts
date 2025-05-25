import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export enum StubStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ARCHIVED = 'ARCHIVED',
  DRAFT = 'DRAFT'
}

export enum ContentMatchType {
  NONE = 'NONE',
  CONTAINS = 'CONTAINS',
  EXACT = 'EXACT',
  REGEX = 'REGEX'
}

export interface MessageHeader {
  name: string;
  value: string;
  type: string;
}

export interface ActiveMQStub {
  id?: string;
  name: string;
  description?: string;
  userId?: string;
  destinationType: string;
  destinationName: string;
  messageSelector?: string;
  contentMatchType?: ContentMatchType;
  contentPattern?: string;
  caseSensitive?: boolean;
  priority?: number;
  responseContent?: string;
  responseType?: string;
  responseDestination?: string;
  webhookUrl?: string;
  latency?: number;
  headers?: Record<string, string>;
  status?: StubStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateActiveMQStubStatusRequest {
  id: string;
  status: StubStatus;
}

export interface CreateStubErrorResponse {
  error: string;
  message: string;
}

export const activemqApi = createApi({
  reducerPath: 'activemqApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['ActiveMQStub'],
  endpoints: (builder) => ({
    getActiveMQStubs: builder.query<ActiveMQStub[], void>({
      query: () => '/activemq/stubs',
      providesTags: ['ActiveMQStub'],
    }),
    
    getActiveMQStubsByUserId: builder.query<ActiveMQStub[], string>({
      query: (userId) => `/activemq/stubs/user/${userId}`,
      providesTags: ['ActiveMQStub'],
    }),
    
    getActiveActiveMQStubsByUserId: builder.query<ActiveMQStub[], string>({
      query: (userId) => `/activemq/stubs/user/${userId}/active`,
      providesTags: ['ActiveMQStub'],
    }),
    
    getActiveMQStub: builder.query<ActiveMQStub, string>({
      query: (id) => `/activemq/stubs/${id}`,
      providesTags: ['ActiveMQStub'],
    }),
    
    createActiveMQStub: builder.mutation<ActiveMQStub, Partial<ActiveMQStub>>({
      query: (stub) => ({
        url: '/activemq/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    updateActiveMQStub: builder.mutation<ActiveMQStub, Partial<ActiveMQStub>>({
      query: (stub) => ({
        url: `/activemq/stubs/${stub.id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    updateActiveMQStubStatus: builder.mutation<ActiveMQStub, UpdateActiveMQStubStatusRequest>({
      query: ({ id, status }) => ({
        url: `/activemq/stubs/${id}/status`,
        method: 'PATCH',
        body: { status },
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    deleteActiveMQStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/activemq/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    addMessageHeader: builder.mutation<ActiveMQStub, { id: string, header: MessageHeader }>({
      query: ({ id, header }) => ({
        url: `/activemq/stubs/${id}/headers`,
        method: 'POST',
        body: header,
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    removeMessageHeader: builder.mutation<ActiveMQStub, { id: string, headerName: string }>({
      query: ({ id, headerName }) => ({
        url: `/activemq/stubs/${id}/headers/${headerName}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['ActiveMQStub'],
    }),
    
    getActiveMQStubsByDestination: builder.query<ActiveMQStub[], string>({
      query: (destinationName) => `/activemq/stubs/destination?destinationName=${destinationName}`,
      providesTags: ['ActiveMQStub'],
    }),
    
    getActiveMQStubsByDestinationTypeAndName: builder.query<ActiveMQStub[], { destinationType: string, destinationName: string }>({
      query: ({ destinationType, destinationName }) => 
        `/activemq/stubs/destination/type?destinationType=${destinationType}&destinationName=${destinationName}`,
      providesTags: ['ActiveMQStub'],
    }),
    
    getActiveActiveMQStubsByDestinationTypeAndName: builder.query<ActiveMQStub[], { destinationType: string, destinationName: string }>({
      query: ({ destinationType, destinationName }) => 
        `/activemq/stubs/destination/active?destinationType=${destinationType}&destinationName=${destinationName}`,
      providesTags: ['ActiveMQStub'],
    }),
  }),
});

export const {
  useGetActiveMQStubsQuery,
  useGetActiveMQStubsByUserIdQuery,
  useGetActiveActiveMQStubsByUserIdQuery,
  useGetActiveMQStubQuery,
  useCreateActiveMQStubMutation,
  useUpdateActiveMQStubMutation,
  useUpdateActiveMQStubStatusMutation,
  useDeleteActiveMQStubMutation,
  useAddMessageHeaderMutation,
  useRemoveMessageHeaderMutation,
  useGetActiveMQStubsByDestinationQuery,
  useGetActiveMQStubsByDestinationTypeAndNameQuery,
  useGetActiveActiveMQStubsByDestinationTypeAndNameQuery,
} = activemqApi; 