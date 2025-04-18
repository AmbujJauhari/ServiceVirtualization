import React from 'react';
import { Link } from 'react-router-dom';

interface StubListProps {
  isEmbedded?: boolean;
}

interface Stub {
  id: string;
  name: string;
  method: string;
  url: string;
  createdAt: string;
  isActive: boolean;
  responseStatus: number;
  responseTime: number;
}

// Mock data for stubs
const mockStubs: Stub[] = [
  {
    id: '1',
    name: 'Get Users',
    method: 'GET',
    url: '/api/users',
    createdAt: '2023-05-15T10:30:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 50
  },
  {
    id: '2',
    name: 'Get User By ID',
    method: 'GET',
    url: '/api/users/:id',
    createdAt: '2023-05-15T11:45:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 30
  },
  {
    id: '3',
    name: 'Create User',
    method: 'POST',
    url: '/api/users',
    createdAt: '2023-05-16T09:20:00Z',
    isActive: false,
    responseStatus: 201,
    responseTime: 75
  },
  {
    id: '4',
    name: 'Update User',
    method: 'PUT',
    url: '/api/users/:id',
    createdAt: '2023-05-16T14:10:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 40
  },
  {
    id: '5',
    name: 'Delete User',
    method: 'DELETE',
    url: '/api/users/:id',
    createdAt: '2023-05-17T10:15:00Z',
    isActive: false,
    responseStatus: 204,
    responseTime: 25
  },
  {
    id: '6',
    name: 'Authentication',
    method: 'POST',
    url: '/api/auth/login',
    createdAt: '2023-05-17T16:30:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 60
  },
  {
    id: '7',
    name: 'Logout',
    method: 'POST',
    url: '/api/auth/logout',
    createdAt: '2023-05-18T09:45:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 20
  },
  {
    id: '8',
    name: 'Get User Profile',
    method: 'GET',
    url: '/api/profile',
    createdAt: '2023-05-18T13:20:00Z',
    isActive: false,
    responseStatus: 200,
    responseTime: 35
  },
  {
    id: '9',
    name: 'Update User Profile',
    method: 'PUT',
    url: '/api/profile',
    createdAt: '2023-05-19T11:10:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 45
  },
  {
    id: '10',
    name: 'Upload Profile Picture',
    method: 'POST',
    url: '/api/profile/picture',
    createdAt: '2023-05-19T15:40:00Z',
    isActive: true,
    responseStatus: 201,
    responseTime: 80
  },
  {
    id: '11',
    name: 'Get Products',
    method: 'GET',
    url: '/api/products',
    createdAt: '2023-05-20T10:25:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 55
  },
  {
    id: '12',
    name: 'Get Product By ID',
    method: 'GET',
    url: '/api/products/:id',
    createdAt: '2023-05-20T14:15:00Z',
    isActive: true,
    responseStatus: 200,
    responseTime: 30
  }
];

const StubList: React.FC<StubListProps> = ({ isEmbedded = false }) => {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString();
  };

  const getMethodColor = (method: string) => {
    switch (method) {
      case 'GET':
        return 'bg-blue-100 text-blue-800';
      case 'POST':
        return 'bg-green-100 text-green-800';
      case 'PUT':
        return 'bg-yellow-100 text-yellow-800';
      case 'DELETE':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return 'bg-green-100 text-green-800';
    if (status >= 300 && status < 400) return 'bg-blue-100 text-blue-800';
    if (status >= 400 && status < 500) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <div>
      {!isEmbedded && (
        <div className="flex justify-between items-center mb-6">
          <Link to="/rest" className="text-primary-600 hover:text-primary-700 flex items-center">
            <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Back to REST Dashboard
          </Link>
        </div>
      )}

      <div className={isEmbedded ? "" : "bg-white rounded-lg shadow-md overflow-hidden mb-6"}>
        <div className="p-4 border-b border-gray-200">
          <div className="flex justify-between items-center">
            <h3 className="text-lg font-medium text-gray-800">Stubs ({mockStubs.length})</h3>
            <div className="flex space-x-2">
              <input 
                type="text" 
                placeholder="Search stubs..." 
                className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              />
              <select className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500">
                <option value="">All Methods</option>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
              <select className="px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500">
                <option value="">All Status</option>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
              {isEmbedded && (
                <Link 
                  to="/rest/stubs/new" 
                  className="bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors"
                >
                  Create New Stubs
                </Link>
              )}
            </div>
          </div>
        </div>
        
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
                Status
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Response
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Response Time
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Created
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {mockStubs.map((stub) => (
              <tr key={stub.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{stub.name}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getMethodColor(stub.method)}`}>
                    {stub.method}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm text-gray-500 font-mono">{stub.url}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    stub.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  }`}>
                    {stub.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(stub.responseStatus)}`}>
                    {stub.responseStatus}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {stub.responseTime} ms
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {formatDate(stub.createdAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <Link to={`/rest/stubs/${stub.id}/edit`} className="text-primary-600 hover:text-primary-900 mr-3">
                    Edit
                  </Link>
                  <button className={`${stub.isActive ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'} mr-3`}>
                    {stub.isActive ? 'Disable' : 'Enable'}
                  </button>
                  <button className="text-red-600 hover:text-red-900">
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default StubList; 