import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import SoapStubList from './stubs/SoapStubList';

const SoapDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'stubs'>('stubs');

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <div className="flex items-center mb-6">
          <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
        </div>
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
        </div>

        {/* Tab Content */}
        {activeTab === 'stubs' && <SoapStubList />}
      </div>
    </div>
  );
};

export default SoapDashboard; 