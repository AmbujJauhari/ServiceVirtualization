import { configureStore } from '@reduxjs/toolkit';
import { recordingConfigApi } from '../api/recordingConfigApi';
import { recordingApi } from '../api/recordingApi';
import { stubApi } from '../api/stubApi';
import { soapStubApi } from '../api/soapStubApi';

export const store = configureStore({
  reducer: {
    [recordingConfigApi.reducerPath]: recordingConfigApi.reducer,
    [recordingApi.reducerPath]: recordingApi.reducer,
    [stubApi.reducerPath]: stubApi.reducer,
    [soapStubApi.reducerPath]: soapStubApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(recordingConfigApi.middleware)
      .concat(recordingApi.middleware)
      .concat(stubApi.middleware)
      .concat(soapStubApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch; 