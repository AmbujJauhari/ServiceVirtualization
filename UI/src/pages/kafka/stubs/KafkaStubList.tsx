import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  useGetKafkaStubsQuery,
  useDeleteKafkaStubMutation,
  useUpdateKafkaStubStatusMutation,
  KafkaStub
} from '../../../api/kafkaApi';

const KafkaStubList: React.FC = () => {
  const { data: stubs, isLoading, isError, refetch } = useGetKafkaStubsQuery();
  const [deleteStub] = useDeleteKafkaStubMutation();
  const [updateStatus] = useUpdateKafkaStubStatusMutation();
  const [filter, setFilter] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
  const navigate = useNavigate();

  const filteredStubs = stubs?.filter(stub => {
    if (!filter) return true;
    
    const lowerFilter = filter.toLowerCase();
    const name = stub.name?.toLowerCase() || '';
    const description = stub.description?.toLowerCase() || '';
    const requestTopic = stub.requestTopic?.toLowerCase() || '';
    const responseTopic = stub.responseTopic?.toLowerCase() || '';
    const keyPattern = stub.keyPattern?.toLowerCase() || '';
    const requestMatcher = stub.requestContentMatcher?.toLowerCase() || '';
    
    return name.includes(lowerFilter) ||
      description.includes(lowerFilter) ||
      requestTopic.includes(lowerFilter) ||
      responseTopic.includes(lowerFilter) ||
      keyPattern.includes(lowerFilter) ||
      requestMatcher.includes(lowerFilter);
  });

  const handleDelete = async (id: string) => {
    if (window.confirm('Are you sure you want to delete this Kafka stub?')) {
      try {
        setDeletingIds(prev => new Set(prev).add(id));
        await deleteStub(id);
      } catch (err) {
        console.error('Error deleting Kafka stub:', err);
        setError('Failed to delete Kafka stub. Please try again.');
      } finally {
        setDeletingIds(prev => {
          const newSet = new Set(prev);
          newSet.delete(id);
          return newSet;
        });
      }
    }
  };

  const handleStatusToggle = async (id: string, currentStatus: string) => {
    const newStatus = currentStatus === 'active' ? 'inactive' : 'active';
    try {
      setTogglingIds(prev => new Set(prev).add(id));
      await updateStatus({ id, status: newStatus });
    } catch (err) {
      console.error('Error updating Kafka stub status:', err);
      setError('Failed to update Kafka stub status. Please try again.');
    } finally {
      setTogglingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleEditStub = (id: string) => {
    navigate(`/kafka/stubs/${id}/edit`);
  };

  if (isLoading) {
    return <div className="flex justify-center p-4">Loading...</div>;
  }

  if (isError) {
    return (
      <div className="p-3 m-4 bg-red-100 text-red-700 rounded">
        Error loading Kafka stubs. Please try again.
      </div>
    );
  }

  return (
    <div className="bg-white shadow-md rounded-lg overflow-hidden">
      <div className="p-4">
        <input
          type="text"
          placeholder="Search stubs by name, description, topic, or content matcher..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="w-full p-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {error && (
        <div className="p-3 m-4 bg-red-100 text-red-700 rounded">
          {error}
          <button 
            onClick={() => setError(null)} 
            className="ml-2 text-red-700 hover:text-red-900"
          >
            ✕
          </button>
        </div>
      )}

      {(!stubs || stubs.length === 0) ? (
        <div className="p-4 text-center text-gray-500">
          No Kafka stubs found. Create one to get started.
        </div>
      ) : filteredStubs?.length === 0 ? (
        <div className="p-4 text-center text-gray-500">
          No matching Kafka stubs found.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Topics</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Formats</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Response</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredStubs?.map((stub) => {
                const created = stub.createdAt ? stub.createdAt : 'N/A';
                
                return (
                  <tr key={stub.id}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{stub.name}</div>
                      <div className="text-sm text-gray-500">{stub.description}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      <div>
                        <span className="font-semibold">Request:</span> {stub.requestTopic}
                      </div>
                      {stub.responseTopic && stub.responseTopic !== stub.requestTopic && (
                        <div className="text-xs text-gray-400 mt-1">
                          <span className="font-semibold">Response:</span> {stub.responseTopic}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex flex-col space-y-1">
                        <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-indigo-100 text-indigo-800">
                          Req: {stub.requestContentFormat || 'JSON'}
                        </span>
                        {stub.requestContentFormat !== stub.responseContentFormat && (
                          <span className="px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full bg-teal-100 text-teal-800">
                            Res: {stub.responseContentFormat || 'JSON'}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                        stub.responseType === 'callback' 
                          ? 'bg-purple-100 text-purple-800' 
                          : 'bg-blue-100 text-blue-800'
                      }`}>
                        {stub.responseType === 'callback' ? 'Webhook' : 'Direct'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <button
                        onClick={() => handleStatusToggle(stub.id, stub.status)}
                        disabled={togglingIds.has(stub.id)}
                        className={`px-2 py-1 inline-flex items-center text-xs leading-5 font-semibold rounded-full ${
                          stub.status === 'active' 
                            ? 'bg-green-100 text-green-800 hover:bg-green-200' 
                            : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                        } ${togglingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''}`}
                      >
                        <span className={`mr-1.5 inline-block w-2 h-2 rounded-full ${
                          stub.status === 'active' ? 'bg-green-500' : 'bg-gray-400'
                        }`}></span>
                        {togglingIds.has(stub.id) ? 'Updating...' : stub.status}
                      </button>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{created}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-right">
                      <div className="flex justify-end space-x-2">
                        <button 
                          onClick={() => handleEditStub(stub.id)}
                          className="text-blue-600 hover:text-blue-900"
                        >
                          Edit
                        </button>
                        <button 
                          onClick={() => handleDelete(stub.id)}
                          disabled={deletingIds.has(stub.id)}
                          className={`text-red-600 hover:text-red-900 ${
                            deletingIds.has(stub.id) ? 'opacity-50 cursor-not-allowed' : ''
                          }`}
                        >
                          {deletingIds.has(stub.id) ? 'Deleting...' : 'Delete'}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default KafkaStubList; 