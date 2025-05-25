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
    isEnabled: true,
    recordingCount: 1,
    stubCount: 6
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Event streaming platform',
    isEnabled: true,
    recordingCount: 4,
    stubCount: 10
  },
  {
    id: 'file',
    name: 'File Service',
    description: 'File-based virtual services for file upload/download simulation',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  },
  {
    id: 'activemq',
    name: 'ActiveMQ',
    description: 'Apache ActiveMQ messaging middleware',
    isEnabled: true,
    recordingCount: 0,
    stubCount: 0
  }
];

import React from 'react';
import { Link } from 'react-router-dom';

const Dashboard: React.FC = () => {
  return (
    <div className="container mx-auto px-4 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {protocols.map((protocol) => (
          <div key={protocol.id} className="bg-white rounded-lg shadow-md overflow-hidden p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-800">{protocol.name}</h2>
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
            ) : protocol.id === 'kafka' ? (
              <Link 
                to="/kafka" 
                className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
              >
                Manage Kafka
              </Link>
            ) : protocol.id === 'file' ? (
              <Link 
                to="/file" 
                className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
              >
                Manage File Service
              </Link>
            ) : protocol.id === 'activemq' ? (
              <Link 
                to="/activemq" 
                className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
              >
                Manage ActiveMQ
              </Link>
            ) : protocol.id === 'ibm-mq' ? (
              <Link 
                to="/ibmmq" 
                className="inline-block bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors w-full text-center"
              >
                Manage IBM MQ
              </Link>
            ) : (
              <div className="inline-block bg-gray-200 text-gray-700 py-2 px-4 rounded w-full text-center">
                Coming Soon
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Dashboard; 