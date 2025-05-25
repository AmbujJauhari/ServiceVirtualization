import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useGetFolStubsQuery, useUpdateFolStubStatusMutation, useDeleteFolStubMutation } from '../../../api/folApi';

const FolStubList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: stubs, isLoading, error, refetch } = useGetFolStubsQuery();
  const [updateStatus] = useUpdateFolStubStatusMutation();
  const [deleteStub] = useDeleteFolStubMutation();
  const [deleteInProgress, setDeleteInProgress] = useState<string | null>(null);
  const [statusUpdateInProgress, setStatusUpdateInProgress] = useState<string | null>(null);

  const handleToggleStatus = async (id: string, currentStatus: boolean) => {
    try {
      setStatusUpdateInProgress(id);
      await updateStatus({ id, status: !currentStatus }).unwrap();
    } catch (err) {
      console.error('Failed to update status:', err);
    } finally {
      setStatusUpdateInProgress(null);
    }
  };

  const handleDeleteStub = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this stub?')) {
      try {
        setDeleteInProgress(id);
        await deleteStub(id).unwrap();
      } catch (err) {
        console.error('Failed to delete stub:', err);
      } finally {
        setDeleteInProgress(null);
      }
    }
  };

  const filteredStubs = stubs?.filter(stub => 
    stub.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    stub.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    stub.filePath.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        <p>Failed to load stubs. Please try refreshing the page.</p>
        <button 
          onClick={() => refetch()} 
          className="mt-2 px-3 py-1 text-xs font-medium text-red-700 bg-red-100 rounded hover:bg-red-200 focus:outline-none"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4">
        <input
          type="text"
          placeholder="Search stubs..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full p-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary-500 focus:border-primary-500"
        />
      </div>

      {filteredStubs && filteredStubs.length > 0 ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  File Path
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Webhook URL
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Created At
                </th>
                <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredStubs.map((stub) => (
                <tr key={stub.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-gray-900">{stub.name}</div>
                    {stub.description && (
                      <div className="text-xs text-gray-500 truncate max-w-xs">{stub.description}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">{stub.filePath}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-500">
                      {stub.webhookUrl ? (
                        <span className="text-xs px-2 py-1 bg-blue-50 text-blue-600 rounded-full">
                          Dynamic
                        </span>
                      ) : (
                        <span className="text-xs px-2 py-1 bg-gray-50 text-gray-600 rounded-full">
                          Static
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      stub.status ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    }`}>
                      {statusUpdateInProgress === stub.id ? (
                        <svg className="animate-spin mr-1 h-2 w-2 text-current" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                      ) : null}
                      {stub.status ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.createdAt ? new Date(stub.createdAt).toLocaleString() : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex justify-end space-x-2">
                      <button
                        onClick={() => handleToggleStatus(stub.id, stub.status)}
                        disabled={statusUpdateInProgress === stub.id}
                        className="text-indigo-600 hover:text-indigo-900 disabled:opacity-50"
                      >
                        {stub.status ? 'Deactivate' : 'Activate'}
                      </button>
                      <Link
                        to={`/fol/stubs/${stub.id}/edit`}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => handleDeleteStub(stub.id)}
                        disabled={deleteInProgress === stub.id}
                        className="text-red-600 hover:text-red-900 disabled:opacity-50"
                      >
                        {deleteInProgress === stub.id ? 'Deleting...' : 'Delete'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-center py-8">
          <p className="text-gray-500 mb-2">No stubs found{searchTerm ? ' matching your search' : ''}.</p>
          <Link
            to="/fol/stubs/create"
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-primary-600 hover:bg-primary-700"
          >
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Create a Stub
          </Link>
        </div>
      )}
    </div>
  );
};

export default FolStubList; 