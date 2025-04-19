import { configureStore, ThunkAction, Action } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import { stubApi } from '../api/stubApi';
import { soapStubApi } from '../api/soapStubApi';
import { recordingApi } from '../api/recordingApi';
import { recordingConfigApi } from '../api/recordingConfigApi';
import { tibcoApi } from '../api/tibcoApi';

export const store = configureStore({
  reducer: {
    [stubApi.reducerPath]: stubApi.reducer,
    [soapStubApi.reducerPath]: soapStubApi.reducer,
    [recordingApi.reducerPath]: recordingApi.reducer,
    [recordingConfigApi.reducerPath]: recordingConfigApi.reducer,
    [tibcoApi.reducerPath]: tibcoApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      stubApi.middleware,
      soapStubApi.middleware,
      recordingApi.middleware,
      recordingConfigApi.middleware,
      tibcoApi.middleware
    ),
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