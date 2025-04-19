import React from 'react';
import { Link } from 'react-router-dom';
import IBMMQStubForm from './IBMMQStubForm';

/**
 * Component for creating a new IBM MQ stub
 */
const CreateIBMMQStub: React.FC = () => {
  return (
    <div>
      <div className="mb-4">
        <Link to="/ibmmq" className="text-primary-600 hover:text-primary-700 flex items-center">
          <svg className="w-6 h-6 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to IBM MQ Dashboard
        </Link>
      </div>
      <IBMMQStubForm />
    </div>
  );
};

export default CreateIBMMQStub; 