import React from 'react';
import { Link } from 'react-router-dom';

// Simple SVG icons as placeholders
const iconMap: Record<string, JSX.Element> = {
  'rest': (
    <svg className="w-8 h-8 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
    </svg>
  ),
  'soap': (
    <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
    </svg>
  ),
  'tibco': (
    <svg className="w-8 h-8 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
    </svg>
  ),
  'ibm-mq': (
    <svg className="w-8 h-8 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
    </svg>
  ),
  'kafka': (
    <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
    </svg>
  )
};

// Hardcoded protocol data
const protocols = [
  {
    id: 'rest',
    name: 'REST',
    description: 'HTTP/HTTPS protocol for RESTful services',
    isEnabled: true,
    recordingCount: 5,
    stubCount: 12
  },
  {
    id: 'soap',
    name: 'SOAP',
    description: 'XML-based messaging protocol',
    isEnabled: true,
    recordingCount: 3,
    stubCount: 8
  },
  {
    id: 'tibco',
    name: 'TIBCO',
    description: 'TIBCO Enterprise Message Service',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  },
  {
    id: 'ibm-mq',
    name: 'IBM MQ',
    description: 'IBM Message Queue middleware',
    isEnabled: false,
    recordingCount: 1,
    stubCount: 6
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Event streaming platform',
    isEnabled: false,
    recordingCount: 4,
    stubCount: 10
  }
];

const Dashboard: React.FC = () => {
  return (
    <div className="container mx-auto px-4 py-8">

      <section>
        <h2 className="text-xl font-semibold text-gray-700 mb-4">Available Protocols</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {protocols.map((protocol) => (
            <div key={protocol.id} className="bg-white p-6 rounded-lg shadow-md hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                {iconMap[protocol.id] || (
                  <div className="w-8 h-8 bg-gray-200 rounded-full"></div>
                )}
                <h3 className="ml-3 text-lg font-medium text-gray-800">{protocol.name}</h3>
                {protocol.isEnabled && (
                  <span className="ml-auto bg-green-100 text-green-800 text-xs font-medium px-2.5 py-0.5 rounded">
                    Active
                  </span>
                )}
              </div>
              
              <p className="text-gray-600 mb-4 h-20 overflow-hidden">{protocol.description}</p>
              
              <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
                <div>
                  <span className="font-medium">{protocol.recordingCount}</span> Recordings
                </div>
                <div>
                  <span className="font-medium">{protocol.stubCount}</span> Stubs
                </div>
              </div>
              
              {protocol.id === 'rest' ? (
                <Link 
                  to="/rest" 
                  className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
                >
                  Manage REST API
                </Link>
              ) : protocol.id === 'soap' ? (
                <Link 
                  to="/soap" 
                  className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
                >
                  Manage SOAP API
                </Link>
              ) : protocol.id === 'tibco' ? (
                <Link 
                  to="/tibco" 
                  className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
                >
                  Manage TIBCO EMS
                </Link>
              ) : (
                <div className="inline-block bg-gray-200 text-gray-700 py-2 px-4 rounded w-full text-center">
                  Coming Soon
                </div>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default Dashboard; 