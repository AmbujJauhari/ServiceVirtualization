import React, { useState } from 'react';
import { useGetFolGroupsQuery, usePublishFolMutation } from '../../../api/folApi';

const FolPublisher: React.FC = () => {
  const [selectedGroupId, setSelectedGroupId] = useState<string>('');
  const [publishSuccess, setPublishSuccess] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  
  const { data: groups, isLoading } = useGetFolGroupsQuery();
  const [publishFol, { isLoading: isPublishing }] = usePublishFolMutation();
  
  const handlePublish = async () => {
    if (!selectedGroupId) {
      setErrorMessage('Please select a file group to publish');
      return;
    }
    
    try {
      setErrorMessage(null);
      await publishFol({ fileGroupId: selectedGroupId }).unwrap();
      setPublishSuccess(true);
      setTimeout(() => setPublishSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to publish:', err);
      setErrorMessage('Failed to publish. Please try again.');
    }
  };
  
  return (
    <div className="bg-white shadow rounded-lg p-6">
      <h2 className="text-lg font-medium text-gray-900 mb-4">Publish Files</h2>
      
      {errorMessage && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          {errorMessage}
        </div>
      )}
      
      {publishSuccess && (
        <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
          Files published successfully!
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
            disabled={isLoading}
          >
            <option value="">Select a file group</option>
            {groups?.map((group) => (
              <option key={group.id} value={group.id}>{group.name}</option>
            ))}
          </select>
          {!groups || groups.length === 0 ? (
            <p className="mt-2 text-sm text-gray-500">
              No file groups available. Create a file stub first.
            </p>
          ) : null}
        </div>
        
        <div>
          <button
            type="button"
            onClick={handlePublish}
            disabled={isPublishing || !selectedGroupId}
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 disabled:bg-gray-400"
          >
            {isPublishing ? (
              <>
                <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Publishing...
              </>
            ) : (
              'Publish Now'
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default FolPublisher; 