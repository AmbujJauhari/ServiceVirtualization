import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useGetStubsQuery, useUpdateStubMutation, useUpdateStubStatusMutation, useDeleteStubMutation } from '../../../api/stubApi';

interface StubListProps {
  isEmbedded?: boolean;
}

const StubList: React.FC<StubListProps> = ({ isEmbedded = false }) => {
  // Fetch stubs from API
  const { data: stubs, isLoading, error, refetch } = useGetStubsQuery();
  const [updateStub] = useUpdateStubMutation();
  const [updateStubStatus] = useUpdateStubStatusMutation();
  const [deleteStub] = useDeleteStubMutation();
  
  // State for search and filters
  const [searchTerm, setSearchTerm] = useState('');
  const [methodFilter, setMethodFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [uniqueMethods, setUniqueMethods] = useState<string[]>([]);
  const [uniqueStatuses, setUniqueStatuses] = useState<string[]>([]);
  const [statusError, setStatusError] = useState('');
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  
  // Extract unique methods from stubs data
  useEffect(() => {
    if (stubs && stubs.length > 0) {
      const methods = stubs
        .map(stub => stub.matchConditions?.method || 'ANY')
        .filter((method, index, self) => self.indexOf(method) === index)
        .sort();
      
      setUniqueMethods(methods);
      
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
  
  // Handle toggle active status
  const handleToggleActive = async (stub: any) => {
    try {
      setTogglingIds(prev => new Set(prev).add(stub.id));
      const updatedStatus = stub.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      await updateStub({ 
        ...stub, 
        status: updatedStatus 
      });
      // Refetch to update the list
      refetch();
    } catch (error) {
      console.error("Error toggling stub status:", error);
      setStatusError('Failed to update status. Please try again.');
    } finally {
      setTogglingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(stub.id);
        return newSet;
      });
    }
  };
  
  // Handle status change to any status
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
  
  // Handle delete stub
  const handleDeleteStub = async (id: string) => {
    if (!window.confirm("Are you sure you want to delete this stub?")) {
      return;
    }
    
    try {
      await deleteStub(id);
      // Refetch to update the list
      refetch();
    } catch (error) {
      console.error("Error deleting stub:", error);
      setStatusError('Failed to delete stub. Please try again.');
    }
  };
  
  // Filter stubs based on search term and filters
  const filteredStubs = stubs ? stubs.filter(stub => {
    const matchesSearch = searchTerm === '' || 
      stub.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (stub.matchConditions?.url && stub.matchConditions.url.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (stub.description && stub.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (stub.tags && stub.tags.some(tag => tag.toLowerCase().includes(searchTerm.toLowerCase())));
    
    const matchesMethod = methodFilter === '' || 
      (stub.matchConditions?.method && stub.matchConditions.method === methodFilter);
    
    const matchesStatus = statusFilter === '' || stub.status === statusFilter;
    
    return matchesSearch && matchesMethod && matchesStatus;
  }) : [];
  
  const getStatusBadgeClasses = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'bg-green-100 text-green-800 border border-green-200';
      case 'INACTIVE':
        return 'bg-gray-100 text-gray-800 border border-gray-200';
      case 'STUB_NOT_REGISTERED':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'DRAFT':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      case 'ARCHIVED':
        return 'bg-yellow-100 text-yellow-800 border border-yellow-200';
      case 'ERROR':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'PENDING':
        return 'bg-orange-100 text-orange-800 border border-orange-200';
      default:
        return 'bg-gray-100 text-gray-800 border border-gray-200';
    }
  };
  
  // Loading state
  if (isLoading) {
    return (
      <div className="py-8 px-4">
        <div className="animate-pulse flex space-x-4">
          <div className="flex-1 space-y-4 py-1">
            <div className="h-4 bg-gray-200 rounded w-3/4"></div>
            <div className="space-y-2">
              <div className="h-4 bg-gray-200 rounded"></div>
              <div className="h-4 bg-gray-200 rounded w-5/6"></div>
            </div>
          </div>
        </div>
        <p className="text-center text-gray-500 mt-4">Loading stubs...</p>
      </div>
    );
  }
  
  // Error state
  if (error) {
    return (
      <div className="py-8 px-4 text-center">
        <div className="bg-red-50 p-4 rounded-md">
          <p className="text-red-700">Error loading stubs. Please try again later.</p>
        </div>
        <button 
          onClick={() => refetch()} 
          className="mt-4 px-4 py-2 bg-primary-600 text-white rounded hover:bg-primary-700"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className={`bg-white shadow-md rounded-lg overflow-hidden ${isEmbedded ? '' : 'mt-8'}`}>
      <div className="border-b border-gray-200 px-6 py-4">
        <div className="flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
          <div className="flex space-x-2">
            <input 
              type="text" 
              placeholder="Search stubs..." 
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <select 
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={methodFilter}
              onChange={(e) => setMethodFilter(e.target.value)}
            >
              <option value="">All Methods</option>
              {uniqueMethods.map(method => (
                <option key={method} value={method}>{method}</option>
              ))}
            </select>
            <select 
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Status</option>
              {uniqueStatuses.map(status => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
            <Link 
              to="/rest/stubs/new" 
              className="bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors"
            >
              Create Stub
            </Link>
          </div>
        </div>
      </div>
      
      {statusError && (
        <div className="p-3 m-4 bg-red-100 text-red-700 rounded">
          {statusError}
          <button 
            onClick={() => setStatusError('')} 
            className="ml-2 text-red-700 hover:text-red-900"
          >
            ✕
          </button>
        </div>
      )}
      
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Method
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                URL
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Tags
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Response Status
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredStubs.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-6 py-8 text-center text-gray-500">
                  No stubs found. {searchTerm || methodFilter || statusFilter ? 'Try adjusting your filters.' : 'Create your first stub!'}
                </td>
              </tr>
            ) : (
              filteredStubs.map((stub) => (
                <tr key={stub.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    <div className="flex flex-col">
                      <span>{stub.name}</span>
                      {stub.description && (
                        <span className="text-xs text-gray-500">{stub.description}</span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span className={`px-2 py-1 rounded text-xs font-medium
                      ${stub.matchConditions?.method === 'GET' ? 'bg-green-100 text-green-800' : ''}
                      ${stub.matchConditions?.method === 'POST' ? 'bg-blue-100 text-blue-800' : ''}
                      ${stub.matchConditions?.method === 'PUT' ? 'bg-yellow-100 text-yellow-800' : ''}
                      ${stub.matchConditions?.method === 'DELETE' ? 'bg-red-100 text-red-800' : ''}
                      ${stub.matchConditions?.method === 'PATCH' ? 'bg-purple-100 text-purple-800' : ''}
                    `}>
                      {stub.matchConditions?.method || 'ANY'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.matchConditions?.url || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <div className="flex flex-wrap gap-1">
                      {stub.tags && stub.tags.length > 0 ? (
                        stub.tags.map(tag => (
                          <span 
                            key={tag} 
                            className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-gray-100 text-gray-800"
                          >
                            {tag}
                          </span>
                        ))
                      ) : (
                        <span className="text-gray-400 text-xs">No tags</span>
                      )}
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
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusBadgeClasses(stub.status)}`}>
                        {stub.status}
                      </span>
                      <div className="flex items-center space-x-1">
                        <button
                          onClick={() => handleQuickToggle(stub.id, stub.status)}
                          disabled={togglingIds.has(stub.id)}
                          className={`px-2 py-1 text-xs font-medium rounded border transition-colors ${
                            stub.status === 'ACTIVE'
                              ? 'border-red-300 text-red-700 hover:bg-red-50'
                              : 'border-green-300 text-green-700 hover:bg-green-50'
                          } ${togglingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''}`}
                          title={stub.status === 'ACTIVE' ? 'Mark as Inactive' : 'Mark as Active'}
                        >
                          {togglingIds.has(stub.id) 
                            ? '...' 
                            : (stub.status === 'ACTIVE' ? 'Deactivate' : 'Activate')
                          }
                        </button>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.createdAt ? new Date(stub.createdAt).toLocaleDateString() : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex space-x-2">
                      <Link
                        to={`/rest/stubs/${stub.id}/edit`}
                        className="text-primary-600 hover:text-primary-900"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => handleDeleteStub(stub.id)}
                        className="text-red-600 hover:text-red-900"
                      >
                        Delete
                      </button>
                    </div>
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

export default StubList; 