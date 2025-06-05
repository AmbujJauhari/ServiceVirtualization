import React from 'react';
import { Link } from 'react-router-dom';
import TibcoStubForm from './TibcoStubForm';

/**
 * Component for creating a new TIBCO stub
 */
const CreateTibcoStub: React.FC = () => {
  return (
    <div>
      <div className="mb-4">
        <Link to="/tibco" className="text-primary-600 hover:text-primary-700 flex items-center">
          <svg className="w-6 h-6 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to TIBCO Dashboard
        </Link>
      </div>
      <TibcoStubForm isEdit={false} />
    </div>
  );
};

export default CreateTibcoStub; 