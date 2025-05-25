import React from 'react';
import FolStubForm from './FolStubForm';
import { useParams } from 'react-router-dom';

const EditFolStub: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  
  if (!id) {
    return <div>Error: No stub ID provided</div>;
  }
  
  return <FolStubForm isEdit={true} stubId={id} />;
};

export default EditFolStub; 