import React from 'react';
import { Link } from 'react-router-dom';

interface RecordingConfigListProps {
  isEmbedded?: boolean;
}

interface RecordingConfig {
  id: string;
  name: string;
  targetUrl: string;
  proxyPort: number;
  isActive: boolean;
  createdAt: string;
  isCertificateUploaded: boolean;
}

// Mock data for recording configurations
const mockConfigs: RecordingConfig[] = [
  {
    id: '1',
    name: 'User API',
    targetUrl: 'https://api.example.com/users',
    proxyPort: 8001,
    isActive: true,
    createdAt: '2023-05-10T14:30:00Z',
    isCertificateUploaded: true
  },
  {
    id: '2',
    name: 'Product API',
    targetUrl: 'https://api.example.com/products',
    proxyPort: 8002,
    isActive: false,
    createdAt: '2023-05-11T09:15:00Z',
    isCertificateUploaded: false
  },
  {
    id: '3',
    name: 'Authentication Service',
    targetUrl: 'https://auth.example.com',
    proxyPort: 8003,
    isActive: true,
    createdAt: '2023-05-12T16:45:00Z',
    isCertificateUploaded: true
  },
  {
    id: '4',
    name: 'Payment Gateway',
    targetUrl: 'https://payments.example.com',
    proxyPort: 8004,
    isActive: false,
    createdAt: '2023-05-13T11:20:00Z',
    isCertificateUploaded: false
  },
  {
    id: '5',
    name: 'Order Service',
    targetUrl: 'https://api.example.com/orders',
    proxyPort: 8005,
    isActive: true,
    createdAt: '2023-05-14T13:10:00Z',
    isCertificateUploaded: true
  },
  {
    id: '6',
    name: 'Notification Service',
    targetUrl: 'https://notifications.example.com',
    proxyPort: 8006,
    isActive: false,
    createdAt: '2023-05-15T10:30:00Z',
    isCertificateUploaded: false
  },
  {
    id: '7',
    name: 'Analytics API',
    targetUrl: 'https://analytics.example.com',
    proxyPort: 8007,
    isActive: true,
    createdAt: '2023-05-16T09:45:00Z',
    isCertificateUploaded: true
  }
];

const RecordingConfigList: React.FC<RecordingConfigListProps> = ({ isEmbedded = false }) => {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString();
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
          <Link 
            to="/rest/configs/new" 
            className="bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors"
          >
            Add New Configuration
          </Link>
        </div>
      )}

      <div className={isEmbedded ? "" : "bg-white rounded-lg shadow-md overflow-hidden"}>
        {isEmbedded && (
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-medium text-gray-800">Recording Configurations</h3>
            <Link 
              to="/rest/configs/new" 
              className="bg-primary-600 text-white py-2 px-4 rounded hover:bg-primary-700 transition-colors"
            >
              Add New Configuration
            </Link>
          </div>
        )}
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Name
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Target URL
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Proxy Port
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Certificate
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
            {mockConfigs.map((config) => (
              <tr key={config.id} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-gray-900">{config.name}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm text-gray-500">{config.targetUrl}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm text-gray-500">{config.proxyPort}</div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    config.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  }`}>
                    {config.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {config.isCertificateUploaded ? (
                    <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                      Uploaded
                    </span>
                  ) : (
                    <Link 
                      to={`/rest/configs/${config.id}/certificate`}
                      className="text-primary-600 hover:text-primary-900 text-sm"
                    >
                      Upload
                    </Link>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {formatDate(config.createdAt)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <Link to={`/rest/configs/${config.id}`} className="text-primary-600 hover:text-primary-900 mr-3">
                    Edit
                  </Link>
                  <button className={`${config.isActive ? 'text-yellow-600 hover:text-yellow-900' : 'text-green-600 hover:text-green-900'} mr-3`}>
                    {config.isActive ? 'Stop' : 'Start'}
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

export default RecordingConfigList; 