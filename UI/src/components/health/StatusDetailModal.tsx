import React from 'react';
import { SystemHealth, HealthStatus } from '../../api/healthApi';

interface StatusDetailModalProps {
  healthData?: SystemHealth;
  isOpen: boolean;
  isLoading: boolean;
  error: any;
  onClose: () => void;
}

const StatusDetailModal: React.FC<StatusDetailModalProps> = ({
  healthData,
  isOpen,
  isLoading,
  error,
  onClose
}) => {
  if (!isOpen) return null;

  // Function to render status badge with appropriate color
  const renderStatusBadge = (status?: HealthStatus) => {
    let bgColor = 'bg-gray-400';
    let textColor = 'text-white';
    let statusText = 'Unknown';

    switch (status) {
      case 'UP':
        bgColor = 'bg-green-100';
        textColor = 'text-green-800';
        statusText = 'Operational';
        break;
      case 'DEGRADED':
        bgColor = 'bg-yellow-100';
        textColor = 'text-yellow-800';
        statusText = 'Degraded';
        break;
      case 'DOWN':
        bgColor = 'bg-red-100';
        textColor = 'text-red-800';
        statusText = 'Down';
        break;
    }

    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${bgColor} ${textColor}`}>
        {statusText}
      </span>
    );
  };

  // Format timestamp to a readable format
  const formatTimestamp = (timestamp?: string) => {
    if (!timestamp) return 'Unknown';
    
    try {
      const date = new Date(timestamp);
      return date.toLocaleString();
    } catch (e) {
      return timestamp;
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 transition-opacity" onClick={onClose}>
          <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
        </div>

        <span className="hidden sm:inline-block sm:align-middle sm:h-screen">&#8203;</span>

        <div 
          className="inline-block align-bottom bg-white dark:bg-gray-800 rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6"
          role="dialog" 
          aria-modal="true" 
          aria-labelledby="modal-headline"
        >
          <div className="sm:flex sm:items-start">
            <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
              <h3 
                className="text-lg leading-6 font-medium text-gray-900 dark:text-gray-100" 
                id="modal-headline"
              >
                System Health Status
              </h3>
              
              <div className="mt-4">
                {isLoading ? (
                  <div className="flex justify-center">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                  </div>
                ) : error ? (
                  <div className="bg-red-50 dark:bg-red-900/20 border-l-4 border-red-400 p-4">
                    <div className="flex">
                      <div className="flex-shrink-0">
                        <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                        </svg>
                      </div>
                      <div className="ml-3">
                        <p className="text-sm text-red-800 dark:text-red-200">
                          Error loading health data
                        </p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {/* Application Status */}
                    <div className="bg-gray-50 dark:bg-gray-700 p-4 rounded-md">
                      <div className="flex justify-between items-center">
                        <h4 className="text-base font-medium text-gray-700 dark:text-gray-300">
                          Application
                        </h4>
                        {renderStatusBadge(healthData?.application.status)}
                      </div>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                        Last checked: {formatTimestamp(healthData?.application.lastChecked)}
                      </p>
                      {healthData?.application.details && (
                        <p className="text-sm mt-2 text-gray-600 dark:text-gray-300">
                          {healthData.application.details}
                        </p>
                      )}
                    </div>
                    
                    {/* Services Status */}
                    <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
                      External Services
                    </h4>
                    
                    <div className="space-y-3">
                      {healthData?.services.map((service, index) => (
                        <div 
                          key={index} 
                          className="bg-gray-50 dark:bg-gray-700 p-3 rounded-md"
                        >
                          <div className="flex justify-between items-center">
                            <h5 className="text-sm font-medium text-gray-700 dark:text-gray-300">
                              {service.name}
                            </h5>
                            {renderStatusBadge(service.status)}
                          </div>
                          <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                            Last checked: {formatTimestamp(service.lastChecked)}
                          </p>
                          {service.details && (
                            <p className="text-xs mt-1 text-gray-600 dark:text-gray-300">
                              {service.details}
                            </p>
                          )}
                        </div>
                      ))}
                      
                      {(!healthData?.services || healthData.services.length === 0) && (
                        <p className="text-sm text-gray-500 dark:text-gray-400 italic">
                          No external services configured
                        </p>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
          
          <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
            <button
              type="button"
              className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:ml-3 sm:w-auto sm:text-sm"
              onClick={onClose}
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatusDetailModal; 