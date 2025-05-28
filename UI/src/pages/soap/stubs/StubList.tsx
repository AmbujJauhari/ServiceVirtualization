import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  useGetSoapStubsQuery,
  useDeleteSoapStubMutation,
  useUpdateSoapStubStatusMutation
} from '../../../api/soapStubApi';

const StubList: React.FC = () => {
  const { data: stubs = [], isLoading, error, refetch } = useGetSoapStubsQuery();
  const [deleteStub] = useDeleteSoapStubMutation();
  const [updateStubStatus] = useUpdateSoapStubStatusMutation();

  const [uniqueStatuses, setUniqueStatuses] = useState<string[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [statusError, setStatusError] = useState('');

  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());

  // Extract unique methods from stubs data
  useEffect(() => {
    if (stubs && stubs.length > 0) {
      // Extract unique statuses
      const statuses = stubs
        .map(stub => stub.status)
        .filter((status, index, self) => self.indexOf(status) === index)
        .sort();

      // Ensure we always have basic statuses available
      setUniqueStatuses(statuses);
    } else {
      // Default statuses when no stubs exist
      setUniqueStatuses(['ACTIVE', 'INACTIVE', 'DRAFT', 'ARCHIVED', 'STUB_NOT_REGISTERED']);
    }
  }, [stubs]);

  // Filter stubs based on search term and status
  const filteredStubs = stubs.filter(stub => {
    const matchesSearch = !searchTerm ||
      stub.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      stub.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      stub.url?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      stub.tags?.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesStatus = statusFilter === 'ALL' || stub.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  const handleDelete = async (id: string) => {
    try {
      await deleteStub(id).unwrap();
      setDeleteConfirm(null);
    } catch (error) {
      console.error('Failed to delete stub:', error);
    }
  };

  const handleStatusChange = async (id: string, newStatus: string) => {
    try {
      setTogglingIds(prev => new Set(prev).add(id));
      await updateStubStatus({ id, status: newStatus });
      // Refetch to update the list
      refetch();
    } catch (error) {
      console.error("Error updating stub status:", error);
      setStatusError('Failed to update status. Please try again.');
    } finally {
      setTogglingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleQuickToggle = async (id: string, currentStatus: string) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    await handleStatusChange(id, newStatus);
  };


  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'INACTIVE':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error loading SOAP stubs</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>Failed to load SOAP stubs. Please try again.</p>
            </div>
            <div className="mt-4">
              <button
                onClick={() => refetch()}
                className="bg-red-100 px-3 py-2 rounded-md text-sm font-medium text-red-800 hover:bg-red-200"
              >
                Retry
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search stubs by name, description, URL, or tags..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="sm:w-48">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">All Status</option>
              {uniqueStatuses.map(status => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          </div>
          <Link
            to="/soap/stubs/new"
            className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            Create SOAP Stub
          </Link>
        </div>
      </div>

      {/* Stubs Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                URL
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Tags
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Response Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredStubs.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                  {searchTerm || statusFilter !== 'ALL' ? 'No stubs found matching your criteria.' : 'No SOAP stubs found. Create your first stub to get started.'}
                </td>
              </tr>
            ) : (
              filteredStubs.map((stub) => (
                <tr key={stub.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div>
                      <div className="text-sm font-medium text-gray-900">{stub.name}</div>
                      {stub.description && (
                        <div className="text-sm text-gray-500 truncate max-w-xs">
                          {stub.description}
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900 font-mono">{stub.url}</div>
                    {stub.soapAction && (
                      <div className="text-xs text-gray-500">SOAPAction: {stub.soapAction}</div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex flex-wrap gap-1">
                      {stub.tags?.map((tag) => (
                        <span
                          key={tag}
                          className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800"
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span className={`px-2 py-1 rounded text-xs font-medium
                      ${(stub.response?.status >= 200 && stub.response?.status < 300) ? 'bg-green-100 text-green-800' : ''}
                      ${(stub.response?.status >= 300 && stub.response?.status < 400) ? 'bg-blue-100 text-blue-800' : ''}
                      ${(stub.response?.status >= 400 && stub.response?.status < 500) ? 'bg-yellow-100 text-yellow-800' : ''}
                      ${(stub.response?.status >= 500) ? 'bg-red-100 text-red-800' : ''}
                    `}>
                      {stub.response?.status || 'N/A'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex items-center space-x-2">
                      <div className="flex items-center space-x-1">
                        <button
                          onClick={() => handleQuickToggle(stub.id, stub.status)}
                          disabled={togglingIds.has(stub.id)}
                          className={`px-2 py-1 inline-flex items-center text-xs leading-5 font-semibold rounded-full ${stub.status === 'active'
                            ? 'bg-green-100 text-green-800 hover:bg-green-200'
                            : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                          } ${togglingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''}`}
                        >
                          <span className={`mr-1.5 inline-block w-2 h-2 rounded-full ${stub.status.toLowerCase() === 'active' ? 'bg-green-500' : 'bg-gray-400'
                          }`}></span>
                        {togglingIds.has(stub.id) ? 'Updating...' : stub.status}
                          
                        </button>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.createdAt ? new Date(stub.createdAt).toLocaleDateString() : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex justify-end space-x-2">
                      {/* Status Toggle */}


                      {/* Edit */}
                      <Link
                        to={`/soap/stubs/${stub.id}/edit`}
                        className="bg-blue-100 text-blue-700 px-3 py-1 rounded text-xs font-medium hover:bg-blue-200 transition-colors"
                      >
                        Edit
                      </Link>

                      {/* Delete */}
                      {deleteConfirm === stub.id ? (
                        <div className="flex space-x-1">
                          <button
                            onClick={() => handleDelete(stub.id)}
                            className="bg-red-600 text-white px-2 py-1 rounded text-xs font-medium hover:bg-red-700"
                          >
                            Confirm
                          </button>
                          <button
                            onClick={() => setDeleteConfirm(null)}
                            className="bg-gray-300 text-gray-700 px-2 py-1 rounded text-xs font-medium hover:bg-gray-400"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => setDeleteConfirm(stub.id)}
                          className="bg-red-100 text-red-700 px-3 py-1 rounded text-xs font-medium hover:bg-red-200 transition-colors"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Summary */}
      <div className="text-sm text-gray-600">
        Showing {filteredStubs.length} of {stubs.length} SOAP stubs
      </div>
    </div>
  );
};

export default StubList; 