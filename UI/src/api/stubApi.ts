import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export interface Stub {
  id: string;
  name: string;
  description: string;
  protocol: string;
  status: string;
  behindProxy: boolean;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  matchConditions: Record<string, any>;
  response: Record<string, any>;
}

export const stubApi = createApi({
  reducerPath: 'stubApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['Stub'],
  endpoints: (builder) => ({
    getStubs: builder.query<Stub[], void>({
      query: () => '/rest/stubs',
      providesTags: ['Stub'],
    }),
    getStubById: builder.query<Stub, string>({
      query: (id) => `/rest/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'Stub', id }],
    }),
    createStub: builder.mutation<Stub, Partial<Stub>>({
      query: (stub) => ({
        url: '/rest/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['Stub'],
    }),
    updateStub: builder.mutation<Stub, Stub>({
      query: (stub) => ({
        url: `/rest/stubs/${stub.id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'Stub', id }],
    }),
    deleteStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/rest/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Stub'],
    }),
    updateActiveStatus: builder.mutation<Stub, { id: string; active: boolean }>({
      query: ({ id, active }) => ({
        url: `/rest/stubs/${id}/active`,
        method: 'PATCH',
        params: { active },
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'Stub', id }],
    }),
    getStubsByTag: builder.query<Stub[], string>({
      query: (tag) => `/rest/stubs/tag/${tag}`,
      providesTags: ['Stub'],
    }),
  }),
});

export const {
  useGetStubsQuery,
  useGetStubByIdQuery,
  useCreateStubMutation,
  useUpdateStubMutation,
  useDeleteStubMutation,
  useUpdateActiveStatusMutation,
  useGetStubsByTagQuery,
} = stubApi; 