import React, { useState } from 'react';
import { useGetFileGroupsQuery, usePublishFileMutation } from '../../api/fileApi';
import ErrorAlert from '../../components/ErrorAlert';
import LoadingSpinner from '../../components/LoadingSpinner';

const FilePublisher: React.FC = () => {
  const [selectedGroupId, setSelectedGroupId] = useState<string>('');
  const [filePublished, setFilePublished] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: fileGroups, isLoading: isLoadingGroups, error: groupsError } = useGetFileGroupsQuery();
  const [publishFile, { isLoading: isPublishing }] = usePublishFileMutation();

  const handlePublish = async () => {
    if (!selectedGroupId) {
      setErrorMessage('Please select a file group');
      return;
    }

    try {
      await publishFile({ 
        fileGroupId: selectedGroupId
      }).unwrap();
      
      setFilePublished(true);
      setTimeout(() => setFilePublished(false), 3000);
      
      // Reset form
      setSelectedGroupId('');
      setErrorMessage(null);
    } catch (err) {
      setErrorMessage('Failed to publish file. Please try again.');
    }
  };

  if (isLoadingGroups) {
    return <LoadingSpinner />;
  }

  if (groupsError) {
    return <ErrorAlert message="Failed to load file groups" />;
  }

  return (
    <div className="bg-white shadow rounded-lg p-6">
      <h2 className="text-lg font-medium text-gray-900 mb-4">Publish File</h2>
      
      {errorMessage && <ErrorAlert message={errorMessage} />}
      
      {filePublished && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
          File published successfully!
        </div>
      )}
      
      <div className="space-y-4">
        <div>
          <label htmlFor="fileGroup" className="block text-sm font-medium text-gray-700">
            File Group
          </label>
          <select
            id="fileGroup"
            value={selectedGroupId}
            onChange={(e) => setSelectedGroupId(e.target.value)}
            className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm rounded-md"
          >
            <option value="">Select a file group</option>
            {fileGroups?.map((group) => (
              <option key={group.id} value={group.id}>
                {group.name}
              </option>
            ))}
          </select>
        </div>
        
        <div>
          <button
            type="button"
            onClick={handlePublish}
            disabled={isPublishing}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:bg-gray-400"
          >
            {isPublishing ? 'Publishing...' : 'Publish Now'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FilePublisher; 