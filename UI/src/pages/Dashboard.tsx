import React from 'react';
import { Link } from 'react-router-dom';

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
  )
};

// Sample data for the protocols
const protocols = [
  {
    id: 'rest',
    name: 'REST',
    description: 'REST API virtualization',
    isEnabled: true,
    recordingCount: 15,
    stubCount: 28,
  },
  {
    id: 'soap',
    name: 'SOAP',
    description: 'SOAP service virtualization',
    isEnabled: true,
    recordingCount: 7,
    stubCount: 12,
  },
  {
    id: 'tibco',
    name: 'TIBCO',
    description: 'TIBCO EMS messaging virtualization',
    isEnabled: true,
    recordingCount: 5,
    stubCount: 10,
  },
  {
    id: 'ibmmq',
    name: 'IBM MQ',
    description: 'IBM MQ messaging virtualization',
    isEnabled: true,
    recordingCount: 3,
    stubCount: 8,
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Kafka messaging virtualization',
    isEnabled: true,
    recordingCount: 4,
    stubCount: 9,
  },
  {
    id: 'file',
    name: 'File Service',
    description: 'File-based service virtualization',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 5,
  },
  {
    id: 'activemq',
    name: 'ActiveMQ',
    description: 'ActiveMQ messaging virtualization',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0,
  }
];

const Dashboard: React.FC = () => {
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {protocols.map((protocol) => (
          <div key={protocol.id} className="bg-white rounded-lg shadow-md overflow-hidden">
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
              
              <div className="flex justify-between text-sm text-gray-500 mb-4">
                <span>{protocol.recordingCount} Recordings</span>
                <span>{protocol.stubCount} Stubs</span>
              </div>
              
              {protocol.id === 'rest' && (
                <Link to="/rest" className="block text-center w-full py-2 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage REST
                </Link>
              )}
              
              {protocol.id === 'soap' && (
                <Link to="/soap" className="block text-center w-full py-2 px-4 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage SOAP
                </Link>
              )}
              
              {protocol.id === 'tibco' && (
                <Link to="/tibco" className="block text-center w-full py-2 px-4 bg-purple-600 hover:bg-purple-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage TIBCO
                </Link>
              )}
              
              {protocol.id === 'ibmmq' && (
                <Link to="/ibmmq" className="block text-center w-full py-2 px-4 bg-blue-800 hover:bg-blue-900 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage IBM MQ
                </Link>
              )}
              
              {protocol.id === 'kafka' && (
                <Link to="/kafka" className="block text-center w-full py-2 px-4 bg-red-600 hover:bg-red-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage Kafka
                </Link>
              )}
              
              {protocol.id === 'file' && (
                <Link to="/file" className="block text-center w-full py-2 px-4 bg-green-600 hover:bg-green-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage Files
                </Link>
              )}
              
              {protocol.id === 'activemq' && (
                <Link to="/activemq" className="block text-center w-full py-2 px-4 bg-amber-600 hover:bg-amber-700 text-white font-medium rounded-md transition duration-150 ease-in-out">
                  Manage ActiveMQ
                </Link>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard; 