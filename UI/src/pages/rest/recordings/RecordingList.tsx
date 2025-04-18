import React from 'react';
import { Link } from 'react-router-dom';

interface RecordingListProps {
  isEmbedded?: boolean;
}

interface Recording {
  id: string;
  name: string;
  timestamp: string;
  method: string;
  url: string;
  statusCode: number;
}

const mockRecordings: Recording[] = [
  {
    id: '1',
    name: 'Get Users',
    timestamp: '2023-05-15T10:30:00Z',
    method: 'GET',
    url: '/api/users',
    statusCode: 200
  },
  {
    id: '2',
    name: 'Create User',
    timestamp: '2023-05-15T11:45:00Z',
    method: 'POST',
    url: '/api/users',
    statusCode: 201
  },
  {
    id: '3',
    name: 'Update User',
    timestamp: '2023-05-15T14:20:00Z',
    method: 'PUT',
    url: '/api/users/123',
    statusCode: 200
  },
  {
    id: '4',
    name: 'Delete User',
    timestamp: '2023-05-15T16:10:00Z',
    method: 'DELETE',
    url: '/api/users/456',
    statusCode: 204
  },
  {
    id: '5',
    name: 'User Authentication',
    timestamp: '2023-05-15T09:15:00Z',
    method: 'POST',
    url: '/api/auth/login',
    statusCode: 200
  }
];

const RecordingList: React.FC<RecordingListProps> = ({ isEmbedded = false }) => {
  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return 'bg-green-100 text-green-800';
    if (status >= 300 && status < 400) return 'bg-blue-100 text-blue-800';
    if (status >= 400 && status < 500) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString();
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

      <div className={isEmbedded ? "" : "bg-white rounded-lg shadow-md overflow-hidden"}>
        {isEmbedded && (
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium text-gray-800">Recordings</h3>
          </div>
        )}
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
                Time
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {mockRecordings.map((recording) => (
              <tr key={recording.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{recording.name}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                    ${recording.method === 'GET' ? 'bg-blue-100 text-blue-800' : 
                      recording.method === 'POST' ? 'bg-green-100 text-green-800' : 
                      recording.method === 'PUT' ? 'bg-yellow-100 text-yellow-800' : 
                      'bg-red-100 text-red-800'}`}>
                    {recording.method}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm text-gray-500">{recording.url}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(recording.statusCode)}`}>
                    {recording.statusCode}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {formatDateTime(recording.timestamp)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <Link to={`/rest/recordings/${recording.id}`} className="text-primary-600 hover:text-primary-900 mr-4">
                    View
                  </Link>
                  <button className="text-green-600 hover:text-green-900">
                    Convert to Stub
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

export default RecordingList; 