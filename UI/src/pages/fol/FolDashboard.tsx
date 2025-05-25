import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import FolStubList from './stubs/FolStubList';
import FolPublisher from './publish/FolPublisher';
import FolScheduler from './schedules/FolScheduler';

const FolDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('stubs');
  const navigate = useNavigate();

  // Custom TabPanel component
  const TabPanel: React.FC<{ id: string; activeTab: string; children: React.ReactNode }> = ({ 
    id, 
    activeTab, 
    children 
  }) => {
    return activeTab === id ? <div>{children}</div> : null;
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
          <h1 className="text-2xl font-bold text-gray-900">File Management</h1>
          <p className="text-sm text-gray-500">
            Manage file stubs, publish files, and schedule file publications
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
            <button
              className={`px-6 py-3 font-medium text-sm focus:outline-none ${
                activeTab === 'publish'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab('publish')}
            >
              Publish
            </button>
            <button
              className={`px-6 py-3 font-medium text-sm focus:outline-none ${
                activeTab === 'schedule'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab('schedule')}
            >
              Schedule
            </button>
          </div>
        </div>

        <div className="p-4">
          {activeTab === 'stubs' && (
            <div className="mb-4 flex justify-end">
              <Link
                to="/fol/stubs/create"
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
              >
                <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                Create Stub
              </Link>
            </div>
          )}

          <TabPanel id="stubs" activeTab={activeTab}>
            <FolStubList />
          </TabPanel>

          <TabPanel id="publish" activeTab={activeTab}>
            <FolPublisher />
          </TabPanel>

          <TabPanel id="schedule" activeTab={activeTab}>
            <FolScheduler />
          </TabPanel>
        </div>
      </div>
    </div>
  );
};

export default FolDashboard; 