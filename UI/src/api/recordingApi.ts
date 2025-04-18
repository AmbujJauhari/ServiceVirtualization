import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export interface Recording {
  id: string;
  name: string;
  description: string;
  userId: string;
  behindProxy: boolean;
  protocol: string;
  protocolData: Record<string, any>;
  createdAt: string;
  updatedAt: string;
  sessionId: string;
  recordedAt: string;
  convertedToStub: boolean;
  convertedStubId: string | null;
  sourceIp: string;
  requestData: Record<string, any>;
  responseData: Record<string, any>;
}

export const recordingApi = createApi({
  reducerPath: 'recordingApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['Recording'],
  endpoints: (builder) => ({
    getRecordings: builder.query<Recording[], void>({
      query: () => '/recordings',
      providesTags: ['Recording'],
    }),
    getRecordingById: builder.query<Recording, string>({
      query: (id) => `/recordings/${id}`,
      providesTags: (result, error, id) => [{ type: 'Recording', id }],
    }),
    createRecording: builder.mutation<Recording, Partial<Recording>>({
      query: (recording) => ({
        url: '/recordings',
        method: 'POST',
        body: recording,
      }),
      invalidatesTags: ['Recording'],
    }),
    updateRecording: builder.mutation<Recording, Recording>({
      query: (recording) => ({
        url: `/recordings/${recording.id}`,
        method: 'PUT',
        body: recording,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'Recording', id }],
    }),
    deleteRecording: builder.mutation<void, string>({
      query: (id) => ({
        url: `/recordings/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Recording'],
    }),
    convertRecordingToStub: builder.mutation<{ stubId: string }, string>({
      query: (id) => ({
        url: `/recordings/${id}/convert-to-stub`,
        method: 'POST',
      }),
      invalidatesTags: ['Recording'],
    }),
  }),
});

export const {
  useGetRecordingsQuery,
  useGetRecordingByIdQuery,
  useCreateRecordingMutation,
  useUpdateRecordingMutation,
  useDeleteRecordingMutation,
  useConvertRecordingToStubMutation,
} = recordingApi; 