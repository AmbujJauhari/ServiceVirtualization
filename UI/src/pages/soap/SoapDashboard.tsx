import React from 'react';
import { Link } from 'react-router-dom';
import StubList from './stubs/StubList';

const SoapDashboard: React.FC = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Breadcrumb */}
      <div className="mb-8">
        <div className="flex items-center mb-6">
          <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
        </div>
      </div>

      {/* Main Content */}
      <StubList />
    </div>
  );
};

export default SoapDashboard; 