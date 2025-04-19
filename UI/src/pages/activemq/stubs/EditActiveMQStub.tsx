import React from 'react';
import ActiveMQStubForm from './ActiveMQStubForm';

/**
 * Component for editing an existing ActiveMQ stub
 */
const EditActiveMQStub: React.FC = () => {
  return <ActiveMQStubForm isEdit={true} />;
};

export default EditActiveMQStub; 