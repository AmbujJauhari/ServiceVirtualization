import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { 
  useGetActiveMQStubsQuery, 
  useUpdateActiveMQStubStatusMutation, 
  useDeleteActiveMQStubMutation,
  ActiveMQStub,
  StubStatus,
  ContentMatchType
} from '../../../api/activemqApi';

/**
 * Component for displaying and managing ActiveMQ stubs
 */
const ActiveMQStubList: React.FC = () => {
  const [filter, setFilter] = useState('');
  const { data: stubs, isLoading, isError, error, refetch } = useGetActiveMQStubsQuery();
  const [updateStatus] = useUpdateActiveMQStubStatusMutation();
  const [deleteStub] = useDeleteActiveMQStubMutation();

  const handleStatusToggle = async (stub: ActiveMQStub) => {
    if (!stub.id) return;
    
    try {
      await updateStatus({
        id: stub.id,
        status: stub.status === StubStatus.ACTIVE ? StubStatus.INACTIVE : StubStatus.ACTIVE
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
    const destinationType = stub.destinationType?.toLowerCase() || '';
    const destinationName = stub.destinationName?.toLowerCase() || '';
    const contentPattern = stub.contentPattern?.toLowerCase() || '';
    
    return name.includes(lowercaseFilter) ||
           description.includes(lowercaseFilter) ||
           destinationType.includes(lowercaseFilter) ||
           destinationName.includes(lowercaseFilter) ||
           contentPattern.includes(lowercaseFilter);
  });

  if (isLoading) {
    return <div className="text-center py-4">Loading...</div>;
  }

  if (isError) {
    return (
      <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative" role="alert">
        <strong className="font-bold">Error!</strong>
        <span className="block sm:inline"> {error ? String(error) : 'Failed to load ActiveMQ stubs'}</span>
        <button className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-1 px-2 rounded ml-2" onClick={() => refetch()}>
          Retry
        </button>
      </div>
    );
  }

  const getStatusBadgeClasses = (status?: StubStatus) => {
    switch (status) {
      case StubStatus.ACTIVE:
        return 'bg-green-100 text-green-800 hover:bg-green-200';
      case StubStatus.INACTIVE:
        return 'bg-gray-100 text-gray-800 hover:bg-gray-200';
      case StubStatus.DRAFT:
        return 'bg-blue-100 text-blue-800 hover:bg-blue-200';
      case StubStatus.ARCHIVED:
        return 'bg-yellow-100 text-yellow-800 hover:bg-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 hover:bg-gray-200';
    }
  };

  const getStatusLabel = (status?: StubStatus) => {
    switch (status) {
      case StubStatus.ACTIVE:
        return 'Active';
      case StubStatus.INACTIVE:
        return 'Inactive';
      case StubStatus.DRAFT:
        return 'Draft';
      case StubStatus.ARCHIVED:
        return 'Archived';
      default:
        return 'Unknown';
    }
  };
  
  const getContentMatchTypeLabel = (matchType?: ContentMatchType) => {
    switch (matchType) {
      case ContentMatchType.CONTAINS:
        return 'Contains';
      case ContentMatchType.EXACT:
        return 'Exact Match';
      case ContentMatchType.REGEX:
        return 'Regex';
      case ContentMatchType.NONE:
      default:
        return 'None';
    }
  };

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
          {filter ? 'No matching stubs found.' : 'No ActiveMQ stubs available. Create one to get started.'}
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white">
            <thead>
              <tr>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Request Destination</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Response Destination</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Content Matching</th>
                <th className="py-2 px-4 border-b border-gray-200 bg-gray-50 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Priority</th>
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
                    <div className="flex items-center">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mr-2 
                        ${stub.destinationType === 'queue' ? 'bg-purple-100 text-purple-800' : 'bg-yellow-100 text-yellow-800'}`}>
                        {stub.destinationType}
                      </span>
                      {stub.destinationName}
                    </div>
                    {stub.messageSelector && (
                      <div className="text-gray-500 text-sm mt-1">
                        <span className="font-semibold">Selector:</span> {stub.messageSelector}
                      </div>
                    )}
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    {stub.responseDestination ? (
                      <div className="flex items-center">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium mr-2 
                          ${stub.responseType === 'queue' ? 'bg-purple-100 text-purple-800' : 'bg-yellow-100 text-yellow-800'}`}>
                          {stub.responseType}
                        </span>
                        {stub.responseDestination}
                      </div>
                    ) : (
                      <span className="text-gray-500 text-sm">Uses JMSReplyTo</span>
                    )}
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    {stub.contentMatchType && stub.contentMatchType !== ContentMatchType.NONE ? (
                      <div>
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium 
                          ${stub.contentMatchType === ContentMatchType.CONTAINS ? 'bg-blue-100 text-blue-800' : 
                            stub.contentMatchType === ContentMatchType.EXACT ? 'bg-indigo-100 text-indigo-800' : 
                            'bg-green-100 text-green-800'}`}>
                          {getContentMatchTypeLabel(stub.contentMatchType)}
                        </span>
                        {stub.contentPattern && (
                          <div className="text-gray-500 text-sm mt-1 truncate max-w-xs">
                            <span className="font-semibold">Pattern:</span> 
                            <span className="font-mono">{stub.contentPattern.length > 30 ? 
                              `${stub.contentPattern.substring(0, 30)}...` : 
                              stub.contentPattern}
                            </span>
                          </div>
                        )}
                        <div className="text-gray-500 text-sm">
                          <span className="font-semibold">Case:</span> {stub.caseSensitive ? 'Sensitive' : 'Insensitive'}
                        </div>
                      </div>
                    ) : (
                      <span className="text-gray-500">None</span>
                    )}
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-orange-100 text-orange-800">
                      {stub.priority ?? 0}
                    </span>
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <button
                      onClick={() => stub.id && handleStatusToggle(stub)}
                      className={`px-3 py-1 rounded-full text-sm font-semibold ${getStatusBadgeClasses(stub.status)}`}
                    >
                      {getStatusLabel(stub.status)}
                    </button>
                  </td>
                  <td className="py-2 px-4 border-b border-gray-200">
                    <div className="flex items-center">
                      <Link
                        to={`/activemq/stubs/${stub.id}/edit`}
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

export default ActiveMQStubList; 