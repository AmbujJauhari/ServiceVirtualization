import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import FileStubList from './stubs/FileStubList';

const FileDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState('stubs');
  const navigate = useNavigate();

  const handleCreateStub = () => {
    navigate('/file/stubs/create');
  };

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex items-center mb-6">
        <button
          onClick={() => navigate('/')}
          className="mr-4 p-2 rounded-full hover:bg-gray-100"
        >
          <svg className="h-5 w-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">File Virtual Service</h1>
          <p className="text-sm text-gray-500">
            Create and manage file stubs for simulating file-based services
          </p>
        </div>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="border-b border-gray-200">
          <div className="flex">
            <button
              className={`px-6 py-3 font-medium text-sm focus:outline-none ${
                activeTab === 'stubs'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab('stubs')}
            >
              Stubs
            </button>
          </div>
        </div>

        <div className="p-4">
          {activeTab === 'stubs' && (
            <div className="mb-4 flex justify-end">
              <button
                onClick={handleCreateStub}
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
              >
                <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                Create Stub
              </button>
            </div>
          )}

          {activeTab === 'stubs' && <FileStubList />}
        </div>
      </div>
    </div>
  );
};

export default FileDashboard; 