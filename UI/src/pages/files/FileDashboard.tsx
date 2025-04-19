import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeftIcon, PlusIcon } from '@heroicons/react/outline';
import FileStubList from './FileStubList';
import FilePublisher from './FilePublisher';
import FileScheduler from './FileScheduler';
import TabPanel from '../../components/TabPanel';

const FileDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('stubs');
  const navigate = useNavigate();

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex items-center mb-6">
        <button
          onClick={() => navigate('/')}
          className="mr-4 p-2 rounded-full hover:bg-gray-100"
        >
          <ArrowLeftIcon className="h-5 w-5 text-gray-600" />
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
                to="/files/stubs/create"
                className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
              >
                <PlusIcon className="h-4 w-4 mr-2" />
                Create Stub
              </Link>
            </div>
          )}

          <TabPanel id="stubs" activeTab={activeTab}>
            <FileStubList />
          </TabPanel>

          <TabPanel id="publish" activeTab={activeTab}>
            <FilePublisher />
          </TabPanel>

          <TabPanel id="schedule" activeTab={activeTab}>
            <FileScheduler />
          </TabPanel>
        </div>
      </div>
    </div>
  );
};

export default FileDashboard; 