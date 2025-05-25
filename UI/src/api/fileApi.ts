import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export interface FileEntry {
  filename: string;
  contentType?: string;
  content?: string;
  webhookUrl?: string;
}

export interface FileStub {
  id: string;
  name: string;
  description?: string;
  userId: string;
  filePath: string;
  content?: string;
  contentType?: string;
  webhookUrl?: string;
  cronExpression?: string;
  status: boolean;
  createdAt?: string;
  updatedAt?: string;
  files?: FileEntry[];
}

export interface CreateFileStubRequest {
  name: string;
  description?: string;
  filePath: string;
  content?: string;
  contentType?: string;
  webhookUrl?: string;
  cronExpression?: string;
  status?: boolean;
  files?: FileEntry[];
}

export interface UpdateFileStubStatusRequest {
  id: string;
  status: boolean;
}

export interface FileGroup {
  id: string;
  name: string;
  description?: string;
  scheduleExpression?: string;
  destination: string;
  fileDefinitions?: any[];
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export const fileApi = createApi({
  reducerPath: 'fileApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['FileStub', 'FileGroup'],
  endpoints: (builder) => ({
    getFileStubs: builder.query<FileStub[], void>({
      query: () => '/file/stubs',
      providesTags: ['FileStub'],
    }),
    getFileStub: builder.query<FileStub, string>({
      query: (id) => `/file/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'FileStub', id }],
    }),
    createFileStub: builder.mutation<FileStub, CreateFileStubRequest>({
      query: (stub) => ({
        url: '/file/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['FileStub'],
    }),
    updateFileStub: builder.mutation<FileStub, Partial<FileStub> & { id: string }>({
      query: ({ id, ...stub }) => ({
        url: `/file/stubs/${id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'FileStub', id }],
    }),
    updateFileStubStatus: builder.mutation<void, UpdateFileStubStatusRequest>({
      query: ({ id, status }) => ({
        url: `/file/stubs/${id}/status`,
        method: 'PUT',
        body: { status },
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'FileStub', id }],
    }),
    deleteFileStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/file/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['FileStub'],
    }),
    // File Group endpoints
    getFileGroups: builder.query<FileGroup[], void>({
      query: () => '/file/groups',
      providesTags: ['FileGroup'],
    }),
    toggleFileGroupStatus: builder.mutation<void, string>({
      query: (id) => ({
        url: `/file/groups/${id}/toggle-status`,
        method: 'PUT',
      }),
      invalidatesTags: (result, error, id) => [{ type: 'FileGroup', id }],
    }),
    deleteFileGroup: builder.mutation<void, string>({
      query: (id) => ({
        url: `/file/groups/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['FileGroup'],
    }),
    publishFile: builder.mutation<void, { fileGroupId: string }>({
      query: (data) => ({
        url: '/file/publish',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['FileGroup'],
    }),
    createFileSchedule: builder.mutation<void, { fileGroupId: string, cronExpression: string }>({
      query: (data) => ({
        url: '/file/schedule',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['FileGroup'],
    }),
  }),
});

export const {
  useGetFileStubsQuery,
  useGetFileStubQuery,
  useCreateFileStubMutation,
  useUpdateFileStubMutation,
  useUpdateFileStubStatusMutation,
  useDeleteFileStubMutation,
  useGetFileGroupsQuery,
  useToggleFileGroupStatusMutation,
  useDeleteFileGroupMutation,
  usePublishFileMutation,
  useCreateFileScheduleMutation,
} = fileApi; 