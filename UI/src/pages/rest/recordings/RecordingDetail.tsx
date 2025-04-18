import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';

interface Recording {
  id: string;
  name: string;
  timestamp: string;
  method: string;
  url: string;
  statusCode: number;
  requestHeaders: Record<string, string>;
  requestBody: string;
  responseHeaders: Record<string, string>;
  responseBody: string;
}

// Mock data for a specific recording
const mockRecording: Recording = {
  id: '1',
  name: 'Get Users',
  timestamp: '2023-05-15T10:30:00Z',
  method: 'GET',
  url: '/api/users',
  statusCode: 200,
  requestHeaders: {
    'Accept': 'application/json',
    'Authorization': 'Bearer token123',
    'User-Agent': 'Mozilla/5.0'
  },
  requestBody: '',
  responseHeaders: {
    'Content-Type': 'application/json',
    'Cache-Control': 'no-cache'
  },
  responseBody: JSON.stringify({
    users: [
      { id: 1, name: 'John Doe', email: 'john@example.com' },
      { id: 2, name: 'Jane Smith', email: 'jane@example.com' }
    ]
  }, null, 2)
};

const RecordingDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [activeTab, setActiveTab] = useState<'request' | 'response'>('request');

  const formatJson = (jsonString: string) => {
    if (!jsonString) return '';
    try {
      return JSON.stringify(JSON.parse(jsonString), null, 2);
    } catch (e) {
      return jsonString;
    }
  };

  // In a real implementation, you would fetch the recording by ID
  const recording = mockRecording;

  if (!recording) {
    return <div>Recording not found</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <Link to="/rest/recordings" className="text-primary-600 hover:text-primary-700 flex items-center">
          <svg className="w-5 h-5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Recordings
        </Link>
        <button className="bg-green-600 text-white py-2 px-4 rounded hover:bg-green-700 transition-colors">
          Convert to Stub
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden p-6 mb-6">
        <div className="flex flex-wrap items-center mb-4">
          <h2 className="text-xl font-semibold text-gray-800 mr-4">{recording.name}</h2>
          <span className={`px-2 py-1 text-xs font-medium rounded-full 
            ${recording.method === 'GET' ? 'bg-blue-100 text-blue-800' : 
              recording.method === 'POST' ? 'bg-green-100 text-green-800' : 
              recording.method === 'PUT' ? 'bg-yellow-100 text-yellow-800' : 
              'bg-red-100 text-red-800'}`}>
            {recording.method}
          </span>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div>
            <p className="text-sm text-gray-500">URL</p>
            <p className="text-gray-800 font-mono">{recording.url}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Status Code</p>
            <span className={`px-2 py-1 text-xs font-medium rounded-full 
              ${recording.statusCode >= 200 && recording.statusCode < 300 ? 'bg-green-100 text-green-800' : 
                recording.statusCode >= 300 && recording.statusCode < 400 ? 'bg-blue-100 text-blue-800' : 
                recording.statusCode >= 400 && recording.statusCode < 500 ? 'bg-yellow-100 text-yellow-800' : 
                'bg-red-100 text-red-800'}`}>
              {recording.statusCode}
            </span>
          </div>
          <div>
            <p className="text-sm text-gray-500">Timestamp</p>
            <p className="text-gray-800">{new Date(recording.timestamp).toLocaleString()}</p>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden mb-6">
        <div className="border-b border-gray-200">
          <div className="flex">
            <button
              className={`px-4 py-2 font-medium text-sm focus:outline-none ${
                activeTab === 'request'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab('request')}
            >
              Request
            </button>
            <button
              className={`px-4 py-2 font-medium text-sm focus:outline-none ${
                activeTab === 'response'
                  ? 'text-primary-600 border-b-2 border-primary-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setActiveTab('response')}
            >
              Response
            </button>
          </div>
        </div>

        <div className="p-4">
          {activeTab === 'request' ? (
            <div>
              <h3 className="text-lg font-medium text-gray-800 mb-4">Request Details</h3>
              
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-700 mb-2">Headers</h4>
                <div className="bg-gray-50 p-3 rounded font-mono text-sm overflow-x-auto">
                  {Object.entries(recording.requestHeaders).map(([key, value]) => (
                    <div key={key} className="mb-1">
                      <span className="text-gray-600">{key}: </span>
                      <span className="text-gray-900">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
              
              {recording.requestBody && (
                <div>
                  <h4 className="text-sm font-medium text-gray-700 mb-2">Body</h4>
                  <div className="bg-gray-50 p-3 rounded font-mono text-sm overflow-x-auto whitespace-pre">
                    {formatJson(recording.requestBody)}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div>
              <h3 className="text-lg font-medium text-gray-800 mb-4">Response Details</h3>
              
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-700 mb-2">Headers</h4>
                <div className="bg-gray-50 p-3 rounded font-mono text-sm overflow-x-auto">
                  {Object.entries(recording.responseHeaders).map(([key, value]) => (
                    <div key={key} className="mb-1">
                      <span className="text-gray-600">{key}: </span>
                      <span className="text-gray-900">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
              
              {recording.responseBody && (
                <div>
                  <h4 className="text-sm font-medium text-gray-700 mb-2">Body</h4>
                  <div className="bg-gray-50 p-3 rounded font-mono text-sm overflow-x-auto whitespace-pre">
                    {formatJson(recording.responseBody)}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default RecordingDetail; 