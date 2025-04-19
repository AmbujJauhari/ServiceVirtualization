import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import IBMMQStubList from './stubs/IBMMQStubList';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel: React.FC<TabPanelProps> = ({ children, value, index }) => {
  return (
    <div role="tabpanel" hidden={value !== index} id={`tabpanel-${index}`}>
      {value === index && <div className="py-4">{children}</div>}
    </div>
  );
};

/**
 * Dashboard for IBM MQ management
 */
const IBMMQDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const navigate = useNavigate();

  const handleTabChange = (index: number) => {
    setActiveTab(index);
  };

  const handleCreateStub = () => {
    navigate('/ibmmq/stubs/create');
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-6">
        <div className="flex items-center">
          <Link to="/" className="text-primary-600 hover:text-primary-700 mr-4">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
        </div>
        <button
          onClick={handleCreateStub}
          className="bg-primary-600 hover:bg-primary-700 text-white py-2 px-4 rounded flex items-center"
        >
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Create Stub
        </button>
      </div>

      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="border-b">
          <nav className="flex">
            <button
              className={`px-4 py-3 text-sm font-medium ${activeTab === 0 ? 'text-primary-700 border-b-2 border-primary-500' : 'text-gray-500 hover:text-gray-700'}`}
              onClick={() => handleTabChange(0)}
            >
              Stubs
            </button>
          </nav>
        </div>

        <div className="p-4">
          <TabPanel value={activeTab} index={0}>
            <IBMMQStubList />
          </TabPanel>
        </div>
      </div>
    </div>
  );
};

export default IBMMQDashboard; 