import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import './index.css';
import './styles/darkMode.css';
import App from './App';
import { store } from './config/store';
import config from './config/configLoader';

// Initialize dark mode from localStorage if present
const initDarkMode = () => {
  if (localStorage.getItem('darkMode') === 'true') {
    document.documentElement.classList.add('dark');
  }
};

// Call the init function before rendering
initDarkMode();

// Log configuration in development mode
if (process.env.NODE_ENV === 'development') {
  console.log('Application config:', config);
}

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
); 