import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useGetTibcoStubsQuery, useUpdateTibcoStubStatusMutation, useDeleteTibcoStubMutation } from '../../../api/tibcoApi';

const TibcoStubList: React.FC = () => {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('');
  
  const { data: stubs, isLoading, refetch } = useGetTibcoStubsQuery();
  const [updateStatus] = useUpdateTibcoStubStatusMutation();
  const [deleteStub] = useDeleteTibcoStubMutation();

  const handleStatusToggle = async (id: string, currentStatus: string) => {
    try {
      const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      await updateStatus({ id, status: newStatus }).unwrap();
      refetch();
    } catch (error) {
      console.error('Failed to update stub status:', error);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this stub?')) {
      try {
        await deleteStub(id).unwrap();
        refetch();
      } catch (error) {
        console.error('Failed to delete stub:', error);
      }
    }
  };

  const handleCreateStub = () => {
    navigate('/tibco/stubs/create');
  };

  const filteredStubs = stubs?.filter(
    (stub) => {
      // Get destination info from either the old or new format
      const destinationName = stub.destinationName || 
        stub.requestDestination?.name || 
        '';
      const destinationType = stub.destinationType || 
        stub.requestDestination?.type || 
        '';
        
      return stub.name.toLowerCase().includes(filter.toLowerCase()) ||
        (stub.description && stub.description.toLowerCase().includes(filter.toLowerCase())) ||
        destinationName.toLowerCase().includes(filter.toLowerCase()) ||
        destinationType.toLowerCase().includes(filter.toLowerCase());
    }
  );

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-lg font-medium text-gray-700">Message Stubs</h2>
        <button
          onClick={handleCreateStub}
          className="bg-primary-600 text-white px-4 py-2 rounded-md hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
        >
          Create Stub
        </button>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="Filter stubs..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="w-full md:w-1/3 rounded-md border border-gray-300 shadow-sm px-3 py-2 focus:outline-none focus:ring-primary-500 focus:border-primary-500"
        />
      </div>

      {isLoading ? (
        <div className="text-center p-4">Loading stubs...</div>
      ) : filteredStubs?.length ? (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Destination
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Description
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Tags
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
                    <div className="flex items-center">
                      <label className="inline-flex relative items-center cursor-pointer">
                        <input
                          type="checkbox"
                          className="sr-only peer"
                          checked={stub.status === 'ACTIVE'}
                          onChange={() => handleStatusToggle(stub.id, stub.status)}
                        />
                        <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                      </label>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {stub.name}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        (stub.destinationType || stub.requestDestination?.type) === 'TOPIC' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'
                      }`}>
                        {stub.destinationType || stub.requestDestination?.type}
                      </span>
                      <span className="ml-2 text-sm text-gray-500">
                        {stub.destinationName || stub.requestDestination?.name || ''}
                        {stub.responseDestination && stub.responseDestination.name !== (stub.destinationName || stub.requestDestination?.name || '') && 
                          ` → ${stub.responseDestination.name}`}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.description || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex flex-wrap gap-1">
                      {stub.tags && stub.tags.length > 0 ? (
                        stub.tags.map((tag, index) => (
                          <span key={index} className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                            {tag}
                          </span>
                        ))
                      ) : (
                        <span className="text-sm text-gray-400">-</span>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <Link 
                      to={`/tibco/stubs/${stub.id}/edit`}
                      className="text-primary-600 hover:text-primary-900 mr-3"
                    >
                      Edit
                    </Link>
                    <button
                      onClick={() => handleDelete(stub.id)}
                      className="text-red-600 hover:text-red-900"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-center p-6 bg-gray-50 rounded-lg">
        </div>
      )}
    </div>
  );
};

export default TibcoStubList; 