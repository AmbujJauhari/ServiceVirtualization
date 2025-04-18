import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  useGetSoapStubsQuery,
  useUpdateSoapStubMutation,
  useDeleteSoapStubMutation,
} from '../../../api/soapStubApi';

interface SoapStubListProps {
  isEmbedded?: boolean;
}

const SoapStubList: React.FC<SoapStubListProps> = ({ isEmbedded = false }) => {
  // Fetch stubs from API
  const { data: stubs, isLoading, error, refetch } = useGetSoapStubsQuery();
  const [updateStub] = useUpdateSoapStubMutation();
  const [deleteStub] = useDeleteSoapStubMutation();

  // State for search and filters
  const [searchTerm, setSearchTerm] = useState('');
  const [serviceFilter, setServiceFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [uniqueServices, setUniqueServices] = useState<string[]>([]);

  // Extract unique services from stubs data
  useEffect(() => {
    if (stubs && stubs.length > 0) {
      const services = stubs
        .map(stub => stub.serviceName || 'Unknown')
        .filter((service, index, self) => self.indexOf(service) === index)
        .sort();

      setUniqueServices(services);
    }
  }, [stubs]);

  // Handle toggle active status
  const handleToggleActive = async (stub: any) => {
    try {
      const updatedStatus = stub.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
      await updateStub({
        ...stub,
        status: updatedStatus,
      });
      // Refetch to update the list
      refetch();
    } catch (error) {
      console.error('Error toggling stub status:', error);
    }
  };

  // Handle delete stub
  const handleDeleteStub = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this stub?')) {
      return;
    }

    try {
      await deleteStub(id);
      // Refetch to update the list
      refetch();
    } catch (error) {
      console.error('Error deleting stub:', error);
    }
  };

  // Filter stubs based on search term and filters
  const filteredStubs = stubs
    ? stubs.filter(stub => {
        const matchesSearch =
          searchTerm === '' ||
          stub.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          (stub.operationName &&
            stub.operationName.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesService =
          serviceFilter === '' ||
          (stub.serviceName && stub.serviceName === serviceFilter);

        const matchesStatus =
          statusFilter === '' ||
          (statusFilter === 'active' && stub.status === 'ACTIVE') ||
          (statusFilter === 'inactive' && stub.status === 'INACTIVE');

        return matchesSearch && matchesService && matchesStatus;
      })
    : [];

  return (
    <div className={`bg-white shadow-md rounded-lg overflow-hidden ${isEmbedded ? '' : 'mt-8'}`}>
      <div className="border-b border-gray-200 px-6 py-4">
        <div className="flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
          <h2 className="text-xl font-semibold text-gray-800">SOAP API Stubs</h2>
          <div className="flex space-x-2">
            <input
              type="text"
              placeholder="Search stubs..."
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={searchTerm}
              onChange={e => setSearchTerm(e.target.value)}
            />
            <select
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={serviceFilter}
              onChange={e => setServiceFilter(e.target.value)}
            >
              <option value="">All Services</option>
              {uniqueServices.map(service => (
                <option key={service} value={service}>
                  {service}
                </option>
              ))}
            </select>
            <select
              className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
            >
              <option value="">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
            <Link
              to="/soap/stubs/new"
              className="bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors"
            >
              Create Stub
            </Link>
          </div>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Name
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Service
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Operation
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Response Status
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Status
              </th>
              <th
                scope="col"
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredStubs.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                  No stubs found.{' '}
                  {searchTerm || serviceFilter || statusFilter
                    ? 'Try adjusting your filters.'
                    : 'Create your first stub!'}
                </td>
              </tr>
            ) : (
              filteredStubs.map(stub => (
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
                    {stub.serviceName || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {stub.operationName || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <span
                      className={`px-2 py-1 rounded text-xs font-medium
                      ${
                        stub.response?.status >= 200 && stub.response?.status < 300
                          ? 'bg-green-100 text-green-800'
                          : ''
                      }
                      ${
                        stub.response?.status >= 300 && stub.response?.status < 400
                          ? 'bg-blue-100 text-blue-800'
                          : ''
                      }
                      ${
                        stub.response?.status >= 400 && stub.response?.status < 500
                          ? 'bg-yellow-100 text-yellow-800'
                          : ''
                      }
                      ${stub.response?.status >= 500 ? 'bg-red-100 text-red-800' : ''}
                    `}
                    >
                      {stub.response?.status || 'N/A'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <button
                      onClick={() => handleToggleActive(stub)}
                      className={`relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 
                        ${stub.status === 'ACTIVE' ? 'bg-primary-600' : 'bg-gray-200'}`}
                    >
                      <span
                        className={`${
                          stub.status === 'ACTIVE' ? 'translate-x-5' : 'translate-x-0'
                        } inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200`}
                      />
                    </button>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex space-x-2">
                      <Link
                        to={`/soap/stubs/${stub.id}/edit`}
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

export default SoapStubList; 