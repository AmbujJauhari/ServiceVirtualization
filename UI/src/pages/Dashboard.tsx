import React from 'react';
import { Link } from 'react-router-dom';
import { useGetProtocolStatusQuery } from '../api/healthApi';

// SVG icons for each protocol
const iconMap = {
  rest: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-indigo-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
    </svg>
  ),
  soap: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
    </svg>
  ),
  tibco: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
    </svg>
  ),
  ibmmq: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-blue-800" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
    </svg>
  ),
  kafka: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
    </svg>
  ),
  file: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  ),
  activemq: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
    </svg>
  ),
  about: (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-slate-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
  )
};

// Map protocol names to their routes and configurations
const protocolConfig = {
  'REST': { id: 'rest', route: '/rest', description: 'REST API virtualization', alwaysEnabled: true },
  'SOAP': { id: 'soap', route: '/soap', description: 'SOAP service virtualization', alwaysEnabled: true },
  'TIBCO EMS': { id: 'tibco', route: '/tibco', description: 'TIBCO EMS messaging virtualization' },
  'IBM MQ': { id: 'ibmmq', route: '/ibmmq', description: 'IBM MQ messaging virtualization' },
  'Kafka': { id: 'kafka', route: '/kafka', description: 'Kafka messaging virtualization' },
  'ActiveMQ': { id: 'activemq', route: '/activemq', description: 'ActiveMQ messaging virtualization' },
};

// Additional services that are always available
const additionalServices = [
  {
    id: 'file',
    name: 'File Service',
    description: 'File-based service virtualization',
    isEnabled: true,
    route: '/files'
  },
  {
    id: 'fol',
    name: 'File Management',
    description: 'File-based service virtualization with scheduling capabilities',
    isEnabled: true,
    route: '/fol'
  },
  {
    id: 'about',
    name: 'About Platform',
    description: 'Learn about Service Virtualization architecture, benefits, and ROI',
    isEnabled: true,
    route: '/about'
  }
];

const Dashboard: React.FC = () => {
  const { data: protocolStatus, isLoading, error } = useGetProtocolStatusQuery();

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading protocol status...</p>
        </div>
      </div>
    );
  }

  // Combine protocol status with configuration
  const protocols = protocolStatus?.protocols?.map(protocol => {
    const config = protocolConfig[protocol.name as keyof typeof protocolConfig];
    return {
      ...protocol,
      ...config,
      // Always show as enabled if it's marked as alwaysEnabled
      isEnabled: config?.alwaysEnabled || protocol.enabled
    };
  }) || [];

  // Add additional services
  const allServices = [...protocols, ...additionalServices];

  return (
    <div className="container mx-auto px-4 py-8">
      {error && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L5.082 18.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-yellow-800">
                Protocol Status Unavailable
              </h3>
              <div className="mt-2 text-sm text-yellow-700">
                <p>Unable to check protocol status. All protocols will be shown as available.</p>
              </div>
            </div>
          </div>
        </div>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {allServices.map((protocol) => {
          const isDisabled = !protocol.isEnabled;
          return (
            <div key={protocol.id || protocol.name} className={`bg-white rounded-lg shadow-md overflow-hidden ${isDisabled ? 'opacity-60' : ''}`}>
              <div className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center">
                    {iconMap[protocol.id as keyof typeof iconMap]}
                    <h2 className="text-xl font-semibold text-gray-800 ml-3">{protocol.name}</h2>
                  </div>
                  {protocol.isEnabled ? (
                    <span className="px-2 py-1 text-xs font-semibold text-green-800 bg-green-100 rounded-full">Enabled</span>
                  ) : (
                    <span className="px-2 py-1 text-xs font-semibold text-red-800 bg-red-100 rounded-full">Disabled</span>
                  )}
                </div>
                
                <p className="text-gray-600 mb-4">{protocol.description}</p>
                
                {!isDisabled && protocol.route && (
                  <Link 
                    to={protocol.route} 
                    className="block text-center w-full py-2 px-4 bg-primary-600 hover:bg-primary-700 text-white font-medium rounded-md transition duration-150 ease-in-out"
                  >
                    Manage {protocol.name}
                  </Link>
                )}
                
                {isDisabled && (
                  <div className="text-center">
                    <button 
                      disabled 
                      className="w-full py-2 px-4 bg-gray-300 text-gray-500 font-medium rounded-md cursor-not-allowed"
                      title={`${protocol.name} is disabled: ${protocol.reason || 'Unknown reason'}`}
                    >
                      Unavailable
                    </button>
                    <p className="text-xs text-gray-500 mt-2">{protocol.reason}</p>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Dashboard; 