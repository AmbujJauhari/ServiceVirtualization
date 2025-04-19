import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useGetFileGroupsQuery, useToggleFileGroupStatusMutation, useDeleteFileGroupMutation } from '../../../api/fileApi';
import LoadingSpinner from '../../../components/common/LoadingSpinner';
import ErrorAlert from '../../../components/common/ErrorAlert';
import { formatDate } from '../../../utils/dateUtils';

const FileGroupList: React.FC = () => {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('');
  const { data: fileGroups, isLoading, error, refetch } = useGetFileGroupsQuery();
  const [toggleStatus] = useToggleFileGroupStatusMutation();
  const [deleteFileGroup] = useDeleteFileGroupMutation();
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleStatusToggle = async (id: string) => {
    try {
      await toggleStatus(id).unwrap();
    } catch (error) {
      console.error('Failed to toggle status:', error);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (window.confirm(`Are you sure you want to delete the file group "${name}"?`)) {
      try {
        await deleteFileGroup(id).unwrap();
        refetch();
      } catch (error) {
        console.error('Failed to delete file group:', error);
        setDeleteError('Failed to delete file group. Please try again.');
      }
    }
  };

  const handleEditFileGroup = (id: string) => {
    navigate(`/files/groups/${id}/edit`);
  };

  const handleCreateFileGroup = () => {
    navigate('/files/groups/create');
  };

  const filteredGroups = fileGroups?.filter(group => {
    const groupName = group.name || '';
    const groupDescription = group.description || '';
    const searchTerm = filter.toLowerCase();
    
    return (
      groupName.toLowerCase().includes(searchTerm) ||
      groupDescription.toLowerCase().includes(searchTerm)
    );
  });

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorAlert message="Failed to load file groups" onRetry={refetch} />;
  }

  return (
    <div className="p-4">
      {deleteError && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 flex justify-between">
          <span>{deleteError}</span>
          <button onClick={() => setDeleteError(null)} className="text-red-700 hover:text-red-900">
            ✕
          </button>
        </div>
      )}

      <div className="mb-4">
        <input
          type="text"
          placeholder="Search file groups..."
          className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      </div>

      {filteredGroups && filteredGroups.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Schedule
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Destination
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Files
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Created
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredGroups.map((group) => {
                const createdDate = formatDate(group.createdAt);
                return (
                  <tr key={group.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{group.name}</div>
                      {group.description && (
                        <div className="text-sm text-gray-500">{group.description}</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{group.scheduleExpression || 'Manual'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{group.destination}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{group.fileDefinitions?.length || 0}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span 
                        className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                          ${group.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}
                      >
                        {group.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{createdDate}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleStatusToggle(group.id)}
                          className={`px-2 py-1 inline-flex leading-5 font-semibold rounded-md ${
                            group.status === 'ACTIVE'
                              ? 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200'
                              : 'bg-green-100 text-green-800 hover:bg-green-200'
                          }`}
                        >
                          {group.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          onClick={() => handleEditFileGroup(group.id)}
                          className="px-2 py-1 bg-blue-100 text-blue-800 hover:bg-blue-200 rounded-md"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(group.id, group.name)}
                          className="px-2 py-1 bg-red-100 text-red-800 hover:bg-red-200 rounded-md"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-center py-6">
          <p className="text-gray-500 mb-4">No file groups found.</p>
          <button
            onClick={handleCreateFileGroup}
            className="px-4 py-2 bg-primary-600 text-white rounded-md hover:bg-primary-700"
          >
            Create Your First File Group
          </button>
        </div>
      )}
    </div>
  );
};

export default FileGroupList; 