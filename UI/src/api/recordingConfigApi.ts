import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import config from '../config/configLoader';

export interface RecordingConfig {
  id: string;
  name: string;
  description: string;
  urlPattern: string;
  active: boolean;
  useHttps: boolean;
  certificateData?: Uint8Array;
  certificatePassword?: string;
}

export const recordingConfigApi = createApi({
  reducerPath: 'recordingConfigApi',
  baseQuery: fetchBaseQuery({ baseUrl: config.API_URL }),
  tagTypes: ['RecordingConfig'],
  endpoints: (builder) => ({
    getRecordingConfigs: builder.query<RecordingConfig[], void>({
      query: () => '/recording-configs',
      providesTags: ['RecordingConfig'],
    }),
    getRecordingConfigById: builder.query<RecordingConfig, string>({
      query: (id) => `/recording-configs/${id}`,
      providesTags: (result, error, id) => [{ type: 'RecordingConfig', id }],
    }),
    createRecordingConfig: builder.mutation<RecordingConfig, FormData>({
      query: (formData) => ({
        url: '/recording-configs',
        method: 'POST',
        body: formData,
        formData: true,
      }),
      invalidatesTags: ['RecordingConfig'],
    }),
    updateRecordingConfig: builder.mutation<RecordingConfig, RecordingConfig>({
      query: (config) => ({
        url: `/recording-configs/${config.id}`,
        method: 'PUT',
        body: config,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'RecordingConfig', id }],
    }),
    deleteRecordingConfig: builder.mutation<void, string>({
      query: (id) => ({
        url: `/recording-configs/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['RecordingConfig'],
    }),
    updateActiveStatus: builder.mutation<RecordingConfig, { id: string; active: boolean }>({
      query: ({ id, active }) => ({
        url: `/recording-configs/${id}/active`,
        method: 'PATCH',
        params: { active },
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'RecordingConfig', id }],
    }),
    uploadCertificate: builder.mutation<RecordingConfig, { id: string; formData: FormData }>({
      query: ({ id, formData }) => ({
        url: `/recording-configs/${id}/certificate`,
        method: 'POST',
        body: formData,
        formData: true,
      }),
      invalidatesTags: (result, error, { id }) => [{ type: 'RecordingConfig', id }],
    }),
  }),
});

export const {
  useGetRecordingConfigsQuery,
  useGetRecordingConfigByIdQuery,
  useCreateRecordingConfigMutation,
  useUpdateRecordingConfigMutation,
  useDeleteRecordingConfigMutation,
  useUpdateActiveStatusMutation,
  useUploadCertificateMutation,
} = recordingConfigApi; 