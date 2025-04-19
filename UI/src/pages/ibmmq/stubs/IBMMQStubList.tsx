import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  useGetIBMMQStubsQuery, 
  useUpdateIBMMQStubStatusMutation, 
  useDeleteIBMMQStubMutation,
  IBMMQStub
} from '../../../api/ibmMqApi';

/**
 * Component for displaying and managing IBM MQ stubs
 */
const IBMMQStubList: React.FC = () => {
  const [filter, setFilter] = useState('');
  const { data: stubs, isLoading, isError, error, refetch } = useGetIBMMQStubsQuery();
  const [updateStatus] = useUpdateIBMMQStubStatusMutation();
  const [deleteStub] = useDeleteIBMMQStubMutation();

  const handleStatusToggle = async (stub: IBMMQStub) => {
    if (!stub.id) return;
    
    try {
      await updateStatus({
        id: stub.id,
        status: !stub.status
      });
    } catch (err) {
      console.error('Failed to update status:', err);
    }
  };

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this stub?')) {
      try {
        await deleteStub(id);
      } catch (err) {
        console.error('Failed to delete stub:', err);
      }
    }
  };

  const filteredStubs = stubs?.filter(stub => {
    if (!filter) return true;
    
    const lowercaseFilter = filter.toLowerCase();
    const name = stub.name?.toLowerCase() || '';
    const description = stub.description?.toLowerCase() || '';
    const queueManager = stub.queueManager?.toLowerCase() || '';
    const queueName = stub.queueName?.toLowerCase() || '';
    
    return name.includes(lowercaseFilter) ||
           description.includes(lowercaseFilter) ||
           queueManager.includes(lowercaseFilter) ||
           queueName.includes(lowercaseFilter);
  });

  if (isLoading) {
    return <div className="text-center py-4">Loading...</div>;
  }

  if (isError) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
        <strong className="font-bold">Error!</strong>
        <span className="block sm:inline"> {error ? String(error) : 'Failed to load IBM MQ stubs'}</span>
        <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-1 px-2 rounded ml-2" onClick={() => refetch()}>
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
          placeholder="Filter stubs..."
          className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
      </div>

      {!filteredStubs?.length ? (
        <div className="text-gray-500 text-center py-4">
          {filter ? 'No matching stubs found.' : 'No IBM MQ stubs available. Create one to get started.'}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white">
            <thead>
              <tr>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Queue Information</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredStubs.map((stub) => (
                <tr key={stub.id} className="hover:bg-gray-50">
                  <td className="py-2 px-4 border-b border-gray-200">
                    <div className="font-medium text-gray-900">{stub.name}</div>
                    {stub.description && <div className="text-gray-500 text-sm">{stub.description}</div>}
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <div>
                      <span className="font-semibold">Queue Manager:</span> {stub.queueManager}
                    </div>
                    <div>
                      <span className="font-semibold">Queue:</span> {stub.queueName}
                    </div>
                    {stub.selector && (
                      <div className="text-gray-500 text-sm mt-1">
                        <span className="font-semibold">Selector:</span> {stub.selector}
                      </div>
                    )}
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <button
                      onClick={() => stub.id && handleStatusToggle(stub)}
                      className={`px-3 py-1 rounded-full text-sm font-semibold ${
                        stub.status
                          ? 'bg-green-100 text-green-800 hover:bg-green-200'
                          : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                      }`}
                    >
                      {stub.status ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <div className="flex items-center">
                      <Link
                        to={`/ibmmq/stubs/${stub.id}/edit`}
                        className="text-indigo-600 hover:text-indigo-900 mr-4"
                      >
                        Edit
                      </Link>
                      <button
                        onClick={() => stub.id && handleDelete(stub.id)}
                        className="text-red-600 hover:text-red-900"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default IBMMQStubList; 