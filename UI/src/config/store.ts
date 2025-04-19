import { configureStore } from '@reduxjs/toolkit';
import { recordingConfigApi } from '../api/recordingConfigApi';
import { recordingApi } from '../api/recordingApi';
import { stubApi } from '../api/stubApi';
import { soapStubApi } from '../api/soapStubApi';
import { tibcoApi } from '../api/tibcoApi';
import { kafkaApi } from '../api/kafkaApi';
import { setupListeners } from '@reduxjs/toolkit/query';

export const store = configureStore({
  reducer: {
    [recordingConfigApi.reducerPath]: recordingConfigApi.reducer,
    [recordingApi.reducerPath]: recordingApi.reducer,
    [stubApi.reducerPath]: stubApi.reducer,
    [soapStubApi.reducerPath]: soapStubApi.reducer,
    [tibcoApi.reducerPath]: tibcoApi.reducer,
    [kafkaApi.reducerPath]: kafkaApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(recordingConfigApi.middleware)
      .concat(recordingApi.middleware)
      .concat(stubApi.middleware)
      .concat(soapStubApi.middleware)
      .concat(tibcoApi.middleware)
      .concat(kafkaApi.middleware),
});

// Optional, but recommended for refetchOnFocus/refetchOnReconnect behaviors
setupListeners(store.dispatch);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch; 