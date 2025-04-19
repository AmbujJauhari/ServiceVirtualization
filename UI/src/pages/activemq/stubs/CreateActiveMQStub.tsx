import React from 'react';
import ActiveMQStubForm from './ActiveMQStubForm';

/**
 * Component for creating a new ActiveMQ stub
 */
const CreateActiveMQStub: React.FC = () => {
  return <ActiveMQStubForm isEdit={false} />;
};

export default CreateActiveMQStub; 