import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

interface SoapStub {
  id: string;
  name: string;
  description?: string;
  userId?: string;
  behindProxy: boolean;
  protocol: string;
  tags: string[];
  status: string;
  wsdlUrl?: string;
  serviceName: string;
  portName?: string;
  operationName: string;
  matchConditions: Record<string, any>;
  response: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
}

export const soapStubApi = createApi({
  reducerPath: 'soapStubApi',
  baseQuery: fetchBaseQuery({ 
    baseUrl: config.API_URL,
    credentials: 'include',
  }),
  tagTypes: ['SoapStub'],
  endpoints: (builder) => ({
    getSoapStubs: builder.query<SoapStub[], void>({
      query: () => '/soap/stubs',
      providesTags: ['SoapStub'],
    }),
    getSoapStubById: builder.query<SoapStub, string>({
      query: (id) => `/soap/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'SoapStub', id }],
    }),
    createSoapStub: builder.mutation<SoapStub, Partial<SoapStub>>({
      query: (stub) => ({
        url: '/soap/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['SoapStub'],
    }),
    updateSoapStub: builder.mutation<SoapStub, Partial<SoapStub>>({
      query: (stub) => ({
        url: `/soap/stubs/${stub.id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'SoapStub', id }],
    }),
    deleteSoapStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/soap/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['SoapStub'],
    }),
    updateSoapStubStatus: builder.mutation<SoapStub, { id: string; active: boolean }>({
      query: ({ id, active }) => ({
        url: `/soap/stubs/${id}/active?active=${active}`,
        method: 'PATCH',
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'SoapStub', id }],
    }),
    getSoapStubsByService: builder.query<SoapStub[], string>({
      query: (serviceName) => `/soap/stubs/service/${serviceName}`,
      providesTags: ['SoapStub'],
    }),
    getSoapStubsByOperation: builder.query<SoapStub[], string>({
      query: (operationName) => `/soap/stubs/operation/${operationName}`,
      providesTags: ['SoapStub'],
    }),
  }),
});

export const {
  useGetSoapStubsQuery,
  useGetSoapStubByIdQuery,
  useCreateSoapStubMutation,
  useUpdateSoapStubMutation,
  useDeleteSoapStubMutation,
  useUpdateSoapStubStatusMutation,
  useGetSoapStubsByServiceQuery,
  useGetSoapStubsByOperationQuery,
} = soapStubApi; 