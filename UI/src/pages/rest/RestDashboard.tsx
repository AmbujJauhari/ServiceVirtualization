import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import StubList from './stubs/StubList';

const RestDashboard: React.FC = () => {
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

        <StubList />
    </div>
  );
};

export default RestDashboard; 