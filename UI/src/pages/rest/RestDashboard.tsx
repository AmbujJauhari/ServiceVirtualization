import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import RecordingConfigList from './configs/RecordingConfigList';
import RecordingList from './recordings/RecordingList';
import StubList from './stubs/StubList';

const RestDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'configs' | 'recordings' | 'stubs'>('configs');

  // Handle direct navigation to create new stub
  const handleCreateStub = () => {
    navigate('/rest/stubs/new');
  };

  // Handle direct navigation to create new recording configuration
  const handleCreateConfig = () => {
    navigate('/rest/configs/new');
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <div className="flex items-center mb-6">
          <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <h1 className="text-2xl font-bold text-gray-800">REST API Management</h1>
        </div>
        <p className="text-gray-600 mb-6">Configure and manage REST API virtual services</p>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Tabs Navigation */}
        <div className="flex border-b border-gray-200">
          <button
            className={`px-6 py-3 text-sm font-medium ${
              activeTab === 'configs'
                ? 'text-primary-600 border-b-2 border-primary-600'
                : 'text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('configs')}
          >
            Recording Configurations
          </button>
          <button
            className={`px-6 py-3 text-sm font-medium ${
              activeTab === 'recordings'
                ? 'text-primary-600 border-b-2 border-primary-600'
                : 'text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
            onClick={() => setActiveTab('recordings')}
          >
            Recordings
          </button>
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
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {activeTab === 'configs' && (
            <>
              <div className="flex justify-end mb-4">
                <button
                  onClick={handleCreateConfig}
                  className="bg-primary-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                >
                  Create New Configuration
                </button>
              </div>
              <RecordingConfigList isEmbedded={true} />
            </>
          )}
          {activeTab === 'recordings' && <RecordingList isEmbedded={true} />}
          {activeTab === 'stubs' && (
            <>
              <StubList isEmbedded={true} />
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default RestDashboard; 