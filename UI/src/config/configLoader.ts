// Loads configuration from external config.js or uses defaults
const getConfig = () => {
  // If window.APP_CONFIG exists (set by external config.js), use it
  if ((window as any).APP_CONFIG) {
    return (window as any).APP_CONFIG;
  }
  
  // Otherwise return defaults for local development
  return {
    API_URL: 'http://localhost:8080/api',
    ENV: 'local',
    DEBUG: true
  };
};

// Export the config
const config = getConfig();
export default config; 