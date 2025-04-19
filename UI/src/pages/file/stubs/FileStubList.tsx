import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { formatDate } from '../../../utils/formatDate';
import { 
  useGetFileStubsQuery, 
  useDeleteFileStubMutation,
  useUpdateFileStubStatusMutation,
  FileStub
} from '../../../api/fileApi';

const FileStubList: React.FC = () => {
  const [filter, setFilter] = useState('');
  const { data: stubs = [], isLoading, error } = useGetFileStubsQuery();
  const [deleteFileStub] = useDeleteFileStubMutation();
  const [updateFileStubStatus] = useUpdateFileStubStatusMutation();
  const navigate = useNavigate();

  const filteredStubs = stubs.filter((stub: FileStub) => {
    const searchTerm = filter.toLowerCase();
    return (
      stub.name.toLowerCase().includes(searchTerm) ||
      (stub.description || '').toLowerCase().includes(searchTerm) ||
      (stub.filePath || '').toLowerCase().includes(searchTerm) ||
      (stub.contentType || '').toLowerCase().includes(searchTerm) ||
      (stub.cronExpression || '').toLowerCase().includes(searchTerm)
    );
  });

  const handleStatusToggle = async (stub: FileStub) => {
    try {
      await updateFileStubStatus({
        id: stub.id,
        status: !stub.status
      });
    } catch (error) {
      console.error('Failed to update status:', error);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this stub?')) {
      try {
        await deleteFileStub(id);
      } catch (error) {
        console.error('Failed to delete:', error);
      }
    }
  };

  const handleEditStub = (id: string) => {
    navigate(`/file/stubs/${id}/edit`);
  };

  // Get the total number of files for a stub (primary + additional files)
  const getFileCount = (stub: FileStub): number => {
    let count = 0;
    
    // Count primary file if content exists
    if (stub.content && stub.content.trim() !== '') {
      count++;
    }
    
    // Add additional files
    if (stub.files && Array.isArray(stub.files)) {
      count += stub.files.length;
    }
    
    return count;
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4">
        Error loading file stubs. Please try again later.
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4">
        <input
          type="text"
          className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Filter by name, description, file path, content type or cron expression"
        />
      </div>
      
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">File Path</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Files</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Cron Schedule</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created</th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredStubs.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-4 text-center text-sm text-gray-500">
                  No file stubs found
                </td>
              </tr>
            ) : (
              filteredStubs.map((stub: FileStub) => (
                <tr key={stub.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{stub.name}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{stub.filePath || 'N/A'}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                      {getFileCount(stub)} files
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.cronExpression ? (
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        {stub.cronExpression}
                      </span>
                    ) : (
                      'Manual'
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <label className="inline-flex items-center cursor-pointer">
                      <input 
                        type="checkbox" 
                        className="sr-only peer"
                        checked={stub.status}
                        onChange={() => handleStatusToggle(stub)}
                      />
                      <div className="relative w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                    </label>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{formatDate(stub.createdAt)}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <button
                      onClick={() => handleEditStub(stub.id)}
                      className="text-primary-600 hover:text-primary-900 mr-3"
                    >
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      onClick={() => handleDelete(stub.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FileStubList; 