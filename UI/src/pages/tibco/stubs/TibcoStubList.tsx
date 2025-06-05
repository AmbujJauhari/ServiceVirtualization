import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  useGetTibcoStubsQuery, 
  useUpdateTibcoStubStatusMutation, 
  useDeleteTibcoStubMutation,
  ContentMatchType 
} from '../../../api/tibcoApi';

const TibcoStubList: React.FC = () => {
  const navigate = useNavigate();
  const [filter, setFilter] = useState('');
  
  const { data: stubs, isLoading, refetch } = useGetTibcoStubsQuery();
  const [updateStatus] = useUpdateTibcoStubStatusMutation();
  const [deleteStub] = useDeleteTibcoStubMutation();

  const getContentMatchTypeLabel = (matchType?: ContentMatchType) => {
    switch (matchType) {
      case ContentMatchType.CONTAINS:
        return 'Contains';
      case ContentMatchType.EXACT:
        return 'Exact';
      case ContentMatchType.REGEX:
        return 'Regex';
      case ContentMatchType.NONE:
      default:
        return 'None';
    }
  };

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



  const filteredStubs = stubs?.filter(
    (stub) => {
      // Get destination info from flat structure
      const destinationName = stub.destinationName || '';
      const destinationType = stub.destinationType || '';
      const contentPattern = stub.contentPattern?.toLowerCase() || '';
      const lowercaseFilter = filter.toLowerCase();
        
      return stub.name.toLowerCase().includes(lowercaseFilter) ||
        (stub.description && stub.description.toLowerCase().includes(lowercaseFilter)) ||
        destinationName.toLowerCase().includes(lowercaseFilter) ||
        destinationType.toLowerCase().includes(lowercaseFilter) ||
        contentPattern.includes(lowercaseFilter);
    }
  );

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-lg font-medium text-gray-700">Message Stubs</h2>
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
                  Content Matching
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
                          onChange={() => handleStatusToggle(stub.id!, stub.status!)}
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
                        stub.destinationType === 'TOPIC' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'
                      }`}>
                        {stub.destinationType}
                      </span>
                      <span className="ml-2 text-sm text-gray-500">
                        {stub.destinationName || ''}
                        {stub.responseDestination && stub.responseDestination !== stub.destinationName && 
                          ` → ${stub.responseDestination}`}
                      </span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.description || '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex flex-wrap gap-1">
                      {stub.contentMatchType && stub.contentMatchType !== ContentMatchType.NONE ? (
                        <div className="flex flex-col">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            stub.contentMatchType === ContentMatchType.CONTAINS ? 'bg-blue-100 text-blue-800' :
                            stub.contentMatchType === ContentMatchType.EXACT ? 'bg-indigo-100 text-indigo-800' :
                            'bg-purple-100 text-purple-800'
                          }`}>
                            {getContentMatchTypeLabel(stub.contentMatchType)}
                          </span>
                          {stub.contentPattern && (
                            <div className="mt-1">
                              <span className="text-xs text-gray-500 font-mono">{stub.contentPattern.length > 30 ?
                                `${stub.contentPattern.substring(0, 30)}...` :
                                stub.contentPattern}
                              </span>
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-sm text-gray-400">No content matching</span>
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
                      onClick={() => handleDelete(stub.id!)}
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
        <div className="text-center p-4 text-gray-500">
          {stubs?.length === 0 ? 'No stubs created yet.' : 'No stubs match your filter.'}
        </div>
      )}
    </div>
  );
};

export default TibcoStubList; 