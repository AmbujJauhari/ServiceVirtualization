import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

export interface MessageHeader {
  name: string;
  value: string;
  type: string;
}

export interface IBMMQStub {
  id?: string;
  name: string;
  description?: string;
  userId?: string;
  queueManager: string;
  queueName: string;
  selector?: string;
  responseContent?: string;
  responseType?: string;
  latency?: number;
  headers?: MessageHeader[];
  status?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UpdateIBMMQStubStatusRequest {
  id: string;
  status: boolean;
}

export const ibmMqApi = createApi({
  reducerPath: 'ibmMqApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['IBMMQStub'],
  endpoints: (builder) => ({
    getIBMMQStubs: builder.query<IBMMQStub[], void>({
      query: () => '/ibmmq/stubs',
      providesTags: ['IBMMQStub'],
    }),
    
    getIBMMQStubsByUserId: builder.query<IBMMQStub[], string>({
      query: (userId) => `/ibmmq/stubs/user/${userId}`,
      providesTags: ['IBMMQStub'],
    }),
    
    getActiveIBMMQStubsByUserId: builder.query<IBMMQStub[], string>({
      query: (userId) => `/ibmmq/stubs/user/${userId}/active`,
      providesTags: ['IBMMQStub'],
    }),
    
    getIBMMQStub: builder.query<IBMMQStub, string>({
      query: (id) => `/ibmmq/stubs/${id}`,
      providesTags: ['IBMMQStub'],
    }),
    
    createIBMMQStub: builder.mutation<IBMMQStub, Partial<IBMMQStub>>({
      query: (stub) => ({
        url: '/ibmmq/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    updateIBMMQStub: builder.mutation<IBMMQStub, Partial<IBMMQStub>>({
      query: (stub) => ({
        url: `/ibmmq/stubs/${stub.id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    updateIBMMQStubStatus: builder.mutation<IBMMQStub, UpdateIBMMQStubStatusRequest>({
      query: ({ id, status }) => ({
        url: `/ibmmq/stubs/${id}/status`,
        method: 'PATCH',
        body: { status },
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    deleteIBMMQStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/ibmmq/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    addMessageHeader: builder.mutation<IBMMQStub, { id: string, header: MessageHeader }>({
      query: ({ id, header }) => ({
        url: `/ibmmq/stubs/${id}/headers`,
        method: 'POST',
        body: header,
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    removeMessageHeader: builder.mutation<IBMMQStub, { id: string, headerName: string }>({
      query: ({ id, headerName }) => ({
        url: `/ibmmq/stubs/${id}/headers/${headerName}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['IBMMQStub'],
    }),
    
    getIBMMQStubsByQueue: builder.query<IBMMQStub[], { queueManager: string, queueName: string }>({
      query: ({ queueManager, queueName }) => 
        `/ibmmq/stubs/queue?queueManager=${queueManager}&queueName=${queueName}`,
      providesTags: ['IBMMQStub'],
    }),
    
    getActiveIBMMQStubsByQueue: builder.query<IBMMQStub[], { queueManager: string, queueName: string }>({
      query: ({ queueManager, queueName }) => 
        `/ibmmq/stubs/queue/active?queueManager=${queueManager}&queueName=${queueName}`,
      providesTags: ['IBMMQStub'],
    }),
  }),
});

export const {
  useGetIBMMQStubsQuery,
  useGetIBMMQStubsByUserIdQuery,
  useGetActiveIBMMQStubsByUserIdQuery,
  useGetIBMMQStubQuery,
  useCreateIBMMQStubMutation,
  useUpdateIBMMQStubMutation,
  useUpdateIBMMQStubStatusMutation,
  useDeleteIBMMQStubMutation,
  useAddMessageHeaderMutation,
  useRemoveMessageHeaderMutation,
  useGetIBMMQStubsByQueueQuery,
  useGetActiveIBMMQStubsByQueueQuery,
} = ibmMqApi; 