import { configureStore, ThunkAction, Action } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import { stubApi } from '../api/stubApi';
import { soapStubApi } from '../api/soapStubApi';
import { recordingApi } from '../api/recordingApi';
import { recordingConfigApi } from '../api/recordingConfigApi';
import { tibcoApi } from '../api/tibcoApi';
import { kafkaApi } from '../api/kafkaApi';
import { fileApi } from '../api/fileApi';
import { activemqApi } from '../api/activemqApi';
import { ibmMqApi } from '../api/ibmMqApi';
import { healthApi } from '../api/healthApi';

// Create middleware array explicitly
const middleware = [
  stubApi.middleware,
  soapStubApi.middleware,
  recordingApi.middleware,
  recordingConfigApi.middleware,
  tibcoApi.middleware,
  kafkaApi.middleware,
  fileApi.middleware,
  activemqApi.middleware,
  ibmMqApi.middleware,
  healthApi.middleware
];

export const store = configureStore({
  reducer: {
    [stubApi.reducerPath]: stubApi.reducer,
    [soapStubApi.reducerPath]: soapStubApi.reducer,
    [recordingApi.reducerPath]: recordingApi.reducer,
    [recordingConfigApi.reducerPath]: recordingConfigApi.reducer,
    [tibcoApi.reducerPath]: tibcoApi.reducer,
    [kafkaApi.reducerPath]: kafkaApi.reducer,
    [fileApi.reducerPath]: fileApi.reducer,
    [activemqApi.reducerPath]: activemqApi.reducer,
    [ibmMqApi.reducerPath]: ibmMqApi.reducer,
    [healthApi.reducerPath]: healthApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(middleware),
});

setupListeners(store.dispatch);

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;
export type AppThunk<ReturnType = void> = ThunkAction<
  ReturnType,
  RootState,
  unknown,
  Action<string>
>; 