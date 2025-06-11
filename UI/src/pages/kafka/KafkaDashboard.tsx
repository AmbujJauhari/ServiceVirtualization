import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import KafkaStubList from './stubs/KafkaStubList';
import KafkaPublisher from './publisher/KafkaPublisher';
import { useGetProtocolStatusQuery } from '../../api/healthApi';

const KafkaDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'stubs' | 'publish'>('stubs');
  
  // Get protocol status
  const { data: protocolStatus, isLoading: protocolLoading, error: protocolError } = useGetProtocolStatusQuery();
  const kafkaProtocol = protocolStatus?.protocols.find(p => p.name === 'Kafka');
  const isKafkaDisabled = kafkaProtocol && !kafkaProtocol.enabled;

  // Show loading state
  if (protocolLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Checking protocol status...</p>
        </div>
      </div>
    );
  }

  // Show disabled state
  if (isKafkaDisabled) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="mb-8">
          <div className="flex items-center mb-6">
            <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </Link>
            <h1 className="text-2xl font-bold text-gray-800">Kafka Dashboard</h1>
          </div>
        </div>

        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8 text-center">
          <div className="mb-4">
            <svg className="w-16 h-16 text-yellow-400 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L5.082 18.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-yellow-800 mb-2">Kafka Protocol Disabled</h2>
          <p className="text-yellow-700 mb-4">
            The Kafka protocol is currently disabled on this deployment.
          </p>
          <p className="text-yellow-600 text-sm">
            Reason: {kafkaProtocol?.reason || 'Unknown reason'}
          </p>
          <div className="mt-6">
            <Link
              to="/"
              className="inline-flex items-center px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              Go to Dashboard
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Show error state
  if (protocolError) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="bg-red-50 border border-red-200 rounded-lg p-8 text-center">
          <div className="mb-4">
            <svg className="w-16 h-16 text-red-400 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold text-red-800 mb-2">Unable to Check Protocol Status</h2>
          <p className="text-red-700 mb-4">
            There was an error checking if Kafka is enabled. The dashboard may not function correctly.
          </p>
          <div className="mt-6">
            <button
              onClick={() => window.location.reload()}
              className="inline-flex items-center px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <div className="flex items-center mb-6">
          <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <h1 className="text-2xl font-bold text-gray-800">Kafka Dashboard</h1>
        </div>
        <p className="text-gray-600">Manage Kafka stubs and publish messages</p>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Tabs Navigation */}
        <div className="flex border-b border-gray-200">
          <button
            className={`px-6 py-3 text-sm font-medium ${
              activeTab === 'stubs'
                ? 'text-primary-600 border-b-2 border-primary-600'
                : 'text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('stubs')}
          >
            Stubs
          </button>
          <button
            className={`px-6 py-3 text-sm font-medium ${
              activeTab === 'publish'
                ? 'text-primary-600 border-b-2 border-primary-600'
                : 'text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('publish')}
          >
            Publish
          </button>
        </div>

        {/* Tab Content */}
        {activeTab === 'stubs' && (
          <div>
            <div className="flex justify-end p-4">
              <button
                className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
                onClick={() => navigate('/kafka/stubs/create')}
              >
                Create Stub
              </button>
            </div>
            <KafkaStubList />
          </div>
        )}
        {activeTab === 'publish' && <KafkaPublisher />}
      </div>
    </div>
  );
};

export default KafkaDashboard; 