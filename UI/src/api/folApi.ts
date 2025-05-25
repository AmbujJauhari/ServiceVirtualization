import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export interface FolEntry {
  filename: string;
  contentType?: string;
  content?: string;
  webhookUrl?: string;
}

export interface FolStub {
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
  files?: FolEntry[];
}

export interface CreateFolStubRequest {
  name: string;
  description?: string;
  filePath: string;
  content?: string;
  contentType?: string;
  webhookUrl?: string;
  cronExpression?: string;
  status?: boolean;
  files?: FolEntry[];
}

export interface UpdateFolStubStatusRequest {
  id: string;
  status: boolean;
}

export interface FolGroup {
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

// Scheduler interfaces
export interface SchedulerInfo {
  isRunning: boolean;
  activeTaskCount: number;
  totalExecutions: number;
  serverTime: string;
}

export interface FolScheduledTask {
  id: string;
  fileStubId: string;
  fileStubName: string;
  cronExpression: string;
  lastExecutionTime?: string;
  nextExecutionTime?: string;
  status: 'SCHEDULED' | 'PAUSED' | 'ERROR';
  errorMessage?: string;
}

export const folApi = createApi({
  reducerPath: 'folApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['FolStub', 'FolGroup', 'FolScheduledTasks', 'SchedulerInfo'],
  endpoints: (builder) => ({
    getFolStubs: builder.query<FolStub[], void>({
      query: () => '/fol/stubs',
      providesTags: ['FolStub'],
    }),
    getFolStub: builder.query<FolStub, string>({
      query: (id) => `/fol/stubs/${id}`,
      providesTags: (result, error, id) => [{ type: 'FolStub', id }],
    }),
    createFolStub: builder.mutation<FolStub, CreateFolStubRequest>({
      query: (stub) => ({
        url: '/fol/stubs',
        method: 'POST',
        body: stub,
      }),
      invalidatesTags: ['FolStub'],
    }),
    updateFolStub: builder.mutation<FolStub, Partial<FolStub> & { id: string }>({
      query: ({ id, ...stub }) => ({
        url: `/fol/stubs/${id}`,
        method: 'PUT',
        body: stub,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'FolStub', id }],
    }),
    updateFolStubStatus: builder.mutation<void, UpdateFolStubStatusRequest>({
      query: ({ id, status }) => ({
        url: `/fol/stubs/${id}/status`,
        method: 'PUT',
        body: { status },
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'FolStub', id }],
    }),
    deleteFolStub: builder.mutation<void, string>({
      query: (id) => ({
        url: `/fol/stubs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['FolStub'],
    }),
    // File Group endpoints
    getFolGroups: builder.query<FolGroup[], void>({
      query: () => '/fol/groups',
      providesTags: ['FolGroup'],
    }),
    toggleFolGroupStatus: builder.mutation<void, string>({
      query: (id) => ({
        url: `/fol/groups/${id}/toggle-status`,
        method: 'PUT',
      }),
      invalidatesTags: (result, error, id) => [{ type: 'FolGroup', id }],
    }),
    deleteFolGroup: builder.mutation<void, string>({
      query: (id) => ({
        url: `/fol/groups/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['FolGroup'],
    }),
    publishFol: builder.mutation<void, { fileGroupId: string }>({
      query: (data) => ({
        url: '/fol/publish',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['FolGroup'],
    }),
    createFolSchedule: builder.mutation<void, { fileGroupId: string; cronExpression: string }>({
      query: (data) => ({
        url: '/fol/schedule',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['FolGroup'],
    }),
    
    // Scheduler endpoints
    getSchedulerInfo: builder.query<SchedulerInfo, void>({
      query: () => '/fol/scheduler/info',
      providesTags: ['SchedulerInfo'],
    }),
    getFolScheduledTasks: builder.query<FolScheduledTask[], void>({
      query: () => '/fol/scheduler/tasks',
      providesTags: ['FolScheduledTasks'],
    }),
    executeScheduledTask: builder.mutation<void, string>({
      query: (taskId) => ({
        url: `/fol/scheduler/tasks/${taskId}/execute`,
        method: 'POST',
      }),
      invalidatesTags: ['FolScheduledTasks', 'SchedulerInfo'],
    }),
    cancelScheduledTask: builder.mutation<void, string>({
      query: (taskId) => ({
        url: `/fol/scheduler/tasks/${taskId}/cancel`,
        method: 'POST',
      }),
      invalidatesTags: ['FolScheduledTasks', 'SchedulerInfo'],
    }),
  }),
});

export const {
  useGetFolStubsQuery,
  useGetFolStubQuery,
  useCreateFolStubMutation,
  useUpdateFolStubMutation,
  useUpdateFolStubStatusMutation,
  useDeleteFolStubMutation,
  useGetFolGroupsQuery,
  useToggleFolGroupStatusMutation,
  useDeleteFolGroupMutation,
  usePublishFolMutation,
  useCreateFolScheduleMutation,
  // Scheduler hooks
  useGetSchedulerInfoQuery,
  useGetFolScheduledTasksQuery,
  useExecuteScheduledTaskMutation,
  useCancelScheduledTaskMutation,
} = folApi; 